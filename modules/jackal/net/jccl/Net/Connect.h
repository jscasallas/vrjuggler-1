
#ifndef _VJ_CONNECT_H_
#define _VJ_CONNECT_H_

#include <vjConfig.h>
#include <Threads/vjThread.h>
#include <Performance/vjTimeStamp.h>
#include <Environment/vjCommand.h>
#include <queue>

class vjTimedUpdate;
class vjConfigChunk;

//--------------------------------------------------
//: vjConnect reads/writes to a file, pipe, or socket.
//
// @author  Christopher Just
//
//---------------------------------------
class vjConnect {
 public:

    vjConnect (int fd, const std::string& _name="unnamed");


    //: default constructor
    //! POST: self is initialized (filename = s, controlPID is NULL)
    //! ARGS: s - name of file to open; a non-NULL C string
    vjConnect(const std::string& s, const std::string& _name);



    vjConnect (vjConfigChunk* c);


    //: destructor
    //! PRE:  True
    //! POST: all dynamically-allocated memory associated
    //+       with self is freed.
    //+       If ControlPID is non-NULL, the process it refers
    //+       to is stopped.
    ~vjConnect();



    std::string getName () {
	return name;
    }



    //: Starts the file connection process.
    //! PRE:  None
    //! POST: A thread has been created to read from the file/pipe
    //+       connection (or an error has occurred while
    //+       attempting to do so).
    //+       controlPID is set to the PID of the generated process.
    //! RETURNS: True - successfully created the thread.
    //! RETURNS: False - unable to create thread.
    bool startProcess();



    //: Stops file connection process.
    //! PRE:  None
    //! POST: If self had a thread associated with it, that
    //+       thread is terminated.
    //! RETURNS: always True
    bool stopProcess();


    void sendDescDB (vjChunkDescDB* db);
    void sendChunkDB (vjConfigChunkDB* db, bool all);
    void sendRefresh ();


    //: Attaches a timed update object to this connection
    //! ARGS: _tu - a vjTimedUpdate* 
    //! ARGS: _refresh_time - time between refreshes, in milliseconds
    void addTimedUpdate (vjTimedUpdate* _tu, float _refresh_time);


    //: Detaches a timed update object from this connection
    void removeTimedUpdate (vjTimedUpdate* _tu);


private:

    ofstream                output;
    std::string             name;
    std::string             filename;
    vjThread*               connect_thread;
    int                     fd;
    bool                    readable;
    bool                    writeable;
    bool                    shutdown;        // set to stop procs

    struct vjCommandPtrCmp {
	bool operator() (const vjCommand* a, const vjCommand* b) {
	    return (a->next_fire_time > b->next_fire_time);
	}
    };

    std::priority_queue<vjCommand*, std::vector<vjCommand*>, vjCommandPtrCmp>
                               timed_commands; // used as heap
    std::queue<vjCommand*>     commands;
    vjMutex                    commands_mutex;

    //: used to see if it's time to spring a timed_command
    vjTimeStamp             current_time;

    //: body of network process.
    void controlLoop (void* nullParam);

    //: utility for controlLoop()
    void readCommand (ifstream& fin);

}; // end vjConnect


#endif
