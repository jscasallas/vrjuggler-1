/***************** <Tweek heading BEGIN do not edit this line> ****************
 * Tweek
 *
 * -----------------------------------------------------------------
 * File:          $RCSfile$
 * Date modified: $Date$
 * Version:       $Revision$
 * -----------------------------------------------------------------
 ***************** <Tweek heading END do not edit this line> *****************/

/*************** <auto-copyright.pl BEGIN do not edit this line> **************
 *
 * VR Juggler is (C) Copyright 1998-2003 by Iowa State University
 *
 * Original Authors:
 *   Allen Bierbaum, Christopher Just,
 *   Patrick Hartling, Kevin Meinert,
 *   Carolina Cruz-Neira, Albert Baker
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 *************** <auto-copyright.pl END do not edit this line> ***************/

package org.vrjuggler.tweek.gui;

import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.vrjuggler.tweek.TweekCore;
import org.vrjuggler.tweek.beans.*;
import org.vrjuggler.tweek.beans.loader.BeanJarClassLoader;
import org.vrjuggler.tweek.net.CommunicationEvent;
import org.vrjuggler.tweek.net.CommunicationListener;
import org.vrjuggler.tweek.net.corba.*;
import org.vrjuggler.tweek.services.*;
import org.vrjuggler.tweek.text.*;


/**
 * The mediator for the whole GUI.  This holds an instance of
 * org.vrjuggler.tweek.BeanContainer which in turn holds the JavaBeans loaded
 * at runtime.  Events generated by this frame are dispatched to the Beans
 * through the org.vrjuggler.tweek.BeanContainer instance.
 *
 * @version $Revision$
 *
 * @see org.vrjuggler.tweek.BeanContainer
 */
public class TweekFrame
   extends JFrame
   implements BeanFocusChangeListener
            , MessageAdditionListener
            , BeanInstantiationListener
            , GlobalPrefsUpdateListener
{
   public TweekFrame(MessageDocument msgDocument)
   {
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);

      // This needs to be done as early as possible so that we receive events
      // that happen during initialization.
      msgDocument.addMessageAdditionListener(this);
      mMsgDocument = msgDocument;

      String icon_name = "tweek-icon.gif";

      // Try to load an icon for this frame.
      try
      {
         ImageIcon icon = new ImageIcon(TweekFrame.class.getResource(icon_name));
         this.setIconImage(icon.getImage());
      }
      catch (NullPointerException e)
      {
         mMsgDocument.printWarning("WARNING: Failed to load icon " +
                                   icon_name + "\n");
      }

      mBeanPrefsDialog =
         new BeanPrefsDialog(this, "Bean-Specific Preferences Editor");

      // Before we register ourselves as a listener for Bean instatiation
      // events, query the list of existing Beans.  We need to make sure that
      // we know about any existing Beans that implement the BeanPreferences
      // interface.
      List known_beans =
         BeanRegistry.instance().getBeansOfType(TweekBean.class.getName());

      Iterator i = known_beans.iterator();
      TweekBean bean;

      while ( i.hasNext() )
      {
         bean = (TweekBean) i.next();
         if ( bean.getBean() instanceof BeanPreferences )
         {
            addPrefsBean((BeanPreferences) bean.getBean());
         }
      }

      BeanInstantiationCommunicator.instance().addBeanInstantiationListener(this);
   }

   /**
    * Sets the BeanViewer used to display the Bean heirarchy.
    */
   public void setBeanViewer (String viewer)
   {
      BeanRegistry registry = BeanRegistry.instance();

      // If a viewer string is valid, we'll try to find the Viewer Bean by
      // name.
      if ( viewer != null && ! viewer.equals("") )
      {
         mBeanViewer = (ViewerBean)registry.getBean( viewer );

         // If the by-name lookup was successful, we use the requested viewer.
         if ( mBeanViewer != null )
         {
            BeanModelViewer bv = mBeanViewer.getViewer();
            mBeanContainer.replaceViewer(bv);
         }
         // Otherwise, we fall back on the first Viewer Bean we know about.
         else
         {
            mMsgDocument.printWarning("WARNING: Unknown viewer type: '" + viewer + "'\n");
            loadFirstViewerBean();
         }
      }
      // If we do not have a viewer name, we'll just use the first one in the
      // collection of known Viewer Beans.
      else
      {
         loadFirstViewerBean();
      }
   }

   /**
    * Loads the first Viewer Bean in the collection of known Viewer Beans.  If
    * there are now Viewer Beans known, mBeanViewer will be set to null.
    */
   private void loadFirstViewerBean()
   {
      List viewers = BeanRegistry.instance().getBeansOfType(ViewerBean.class.getName());

      if ( viewers.size() > 0 )
      {
         mBeanViewer = (ViewerBean) viewers.get(0);
         mBeanContainer.replaceViewer((BeanModelViewer)mBeanViewer.getViewer() );

         try
         {
            GlobalPreferencesService prefs =
               new GlobalPreferencesServiceProxy();
            prefs.setBeanViewer(mBeanViewer.getName());
         }
         catch(java.io.IOException ex)
         {
            System.err.println("WARNING: No preferred viewer Bean can be loaded");
            System.err.println(ex.getMessage());
         }
         catch(RuntimeException ex)
         {
            System.err.println("WARNING: No preferred viewer Bean can be loaded");
            System.err.println(ex.getMessage());
         }
      }
      else
      {
         mMsgDocument.printWarning("WARNING: No Viewer Beans loaded");
         mBeanViewer = null;
      }
   }

   public ViewerBean getBeanViewer()
   {
      return mBeanViewer;
   }

   /**
    * Initializes the GUI.
    *
    * @pre The JavaBean search has been performed.
    */
   public void initGUI()
   {
      mMessagePanel = new MessagePanel(mMsgDocument);

      try
      {
         jbInit();
         mMenuPrefsBeanEdit.setEnabled(mBeanPrefsDialog.getPrefsBeanCount() > 0);

         if ( mMacOS )
         {
            try
            {
               ClassLoader loader = getClass().getClassLoader();
               String class_name = "org.vrjuggler.tweek.mac.TweekAppListener";
               final Class mac_app_class = loader.loadClass(class_name);
               java.lang.reflect.Constructor[] ctors =
                  mac_app_class.getConstructors();
               mMacAppListener = ctors[0].newInstance(new Object[] {this});
            }
            catch(InstantiationException e)
            {
               e.printStackTrace();
            }
            catch(ClassNotFoundException e)
            {
               e.printStackTrace();
            }
         }
         else
         {
            setMnemonics();
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      // Validate frames that have preset sizes.
      // Pack frames that have useful preferred size info, e.g. from their
      // layout.
      if ( mPackFrame )
      {
         this.pack();
      }
      else
      {
         this.validate();
      }

      this.setVisible(true);
   }

   /**
    * Handles a BeanFocusChangeEvent.  The change in Bean focus gives us the
    * opportunity to manipulate context-specific (Bean-specific) information.
    * This includes context-specific file loading.  When a new Bean is focuesd,
    * it is queried to determine if it implements
    * org.vrjuggler.tweek.beans.FileLoader.  If so, the Open item in the File
    * menu is enabled, and the newly focused Bean is set up to receive open
    * requests from the GUI.
    */
   public void beanFocusChanged (BeanFocusChangeEvent e)
   {
      if ( e.getFocusType() == BeanFocusChangeEvent.BEAN_FOCUSED )
      {
         mMsgDocument.printStatus("Bean " + e.getBean() + " focused!\n");

         Object panel_bean = e.getBean().getBean();

         // If the Panel Bean supports file opening, enable mMenuFileOpen and
         // set the Bean as the current file loader.  If and when the user
         // selects the Open item in the File menu, panel_bean will be informed
         // of the open request via the FileLoader interface.
         if ( panel_bean instanceof FileLoader )
         {
            FileLoader loader = (FileLoader) panel_bean;
            mMenuFileOpen.setText("Open " + loader.getFileType() + " ...");
            mMenuFileClose.setText("Close " + loader.getFileType() + " ...");
            mMenuFileOpen.setEnabled(true);
            mMenuFileSave.setEnabled(loader.canSave());
            mMenuFileSaveAs.setEnabled(loader.canSave());

            if ( loader.getOpenFileCount() > 0 )
            {
               mMenuFileClose.setEnabled(true);
            }

            mFileLoader = loader;
         }
         // This new Bean can't open files, we we make sure to disable the
         // Open menu item.
         else
         {
            disableFileHandlingItems();
         }
      }
      else if ( e.getFocusType() == BeanFocusChangeEvent.BEAN_UNFOCUSED )
      {
         mMsgDocument.printStatus("Bean " + e.getBean() + " unfocused!\n");

         // Disable the Open menu item in the File menu just to be safe.
         disableFileHandlingItems();
      }
   }

   /**
    * Handles events from the message panel's document indicating that a new
    * message has been added.  Depending on the state of the message panel, the
    * button for opening and closing the panel is updated to reflect that a new
    * message is present.
    */
   public void messageAdded(MessageAdditionEvent e)
   {
      // If the message panel is closed, change mStatusMsgButton to indicate
      // that a new message has been added.
      if ( ! mMsgPanelExpanded )
      {
         if ( null != mBulbOnIcon )
         {
            mStatusMsgButton.setIcon(mBulbOnIcon);
         }
         else
         {
            mStatusMsgButton.setForeground(Color.red);
         }

         mStatusMsgLabel.setText("New message in message panel");
      }
   }

   /**
    * @post If the instantiated Bean implements the BeanPreferences
    *       interface, its preferences are loaded, and its editor is added to
    *       the Bean-specified editor dialog.
    */
   public void beanInstantiated(BeanInstantiationEvent e)
   {
      Object new_bean = e.getTweekBean().getBean();

      if ( new_bean instanceof BeanPreferences )
      {
         addPrefsBean((BeanPreferences) new_bean);
         mMenuPrefsBeanEdit.setEnabled(true);
      }
   }

   /**
    * Handles individual updates to global preferences via the editor dialog.
    * At this time, this method does nothing.  At some point, it could be used
    * to provide immediate feedback to a user as he or she edits preferences
    * values.
    */
   public void globalPrefsModified(GlobalPrefsUpdateEvent e)
   {
   }

   public void globalPrefsSaved(GlobalPrefsUpdateEvent e)
   {
      try
      {
         GlobalPreferencesService prefs = new GlobalPreferencesServiceProxy();

         // Save this for later.
         int old_level = prefs.getUserLevel();

         String viewer = prefs.getBeanViewer();

         ViewerBean bean = (ViewerBean)BeanRegistry.instance().getBean(viewer);

         // Verify that the viewer lookup did not fail.
         // XXX: There should be a check here to compare the existing viewer
         // with the selected viewer.  If they are the same, do not do the
         // replacement.
         if ( null != bean )
         {
            mBeanContainer.replaceViewer(bean.getViewer());
         }

         String new_laf = prefs.getLookAndFeel();
         String old_laf = UIManager.getLookAndFeel().getName();

         if ( ! old_laf.equals(new_laf) )
         {
            try
            {
               UIManager.setLookAndFeel(new_laf);
               this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
               SwingUtilities.updateComponentTreeUI(this);

               // Update all the loaded Beans.
               List beans =
                  BeanRegistry.instance().getBeansOfType(PanelBean.class.getName());
               Iterator i = beans.iterator();
               PanelBean cur_bean;

               while ( i.hasNext() )
               {
                  cur_bean = (PanelBean) i.next();

                  if ( null != cur_bean.getComponent() )
                  {
                     SwingUtilities.updateComponentTreeUI(cur_bean.getComponent());
                  }
               }

               validate();
            }
            catch (Exception laf_e)
            {
               // Set the look and feel back to the old value because the
               // newly chosen setting isn't valid.
               prefs.setLookAndFeel(old_laf);
               prefs.save();
               JOptionPane.showMessageDialog(this,
                                             "Invalid look and feel '" +
                                                new_laf + "'",
                                             "Bad Look and Feel Setting",
                                             JOptionPane.ERROR_MESSAGE);
            }
            finally
            {
               this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
         }

         // If the user level changed, fire an event saying as much.
         if ( old_level != prefs.getUserLevel() )
         {
            mBeanContainer.fireUserLevelChange(old_level,
                                               prefs.getUserLevel());
         }

         // Handle resizing this frame if necessary.
         int window_width  = prefs.getWindowWidth();
         int window_height = prefs.getWindowHeight();

         Dimension cur_size = this.getSize();

         if ( cur_size.width != window_width ||
              cur_size.height != window_height )
         {
            this.setSize(window_width, window_height);
         }
      }
      catch(Exception ex)
      {
         // There is no need to do anything here.  If the GlobalPreferences
         // Bean is not available, the menu option will be disabled, thus
         // preventing this method from ever being executed.
      }
   }

   public void handleAbout()
   {
      helpAboutAction(null);
   }

   public void handleOpenFile()
   {
      fileOpenAction(null);
   }

   public void handlePrefs()
   {
      prefsEditGlobal(null);
   }

   public void handleQuit()
   {
      fileQuitAction(null);
   }

   // =========================================================================
   // Private methods.
   // =========================================================================

   /**
    * Adds the given Bean to the collection of Beans that have editable
    * preferences.
    *
    * @post The Bean's preferences are loaded, and it is added to the
    *       collection in the Bean preferences editor dialog.
    */
   private void addPrefsBean(BeanPreferences bean)
   {
      try
      {
         bean.load();
      }
      catch (java.io.IOException io_ex)
      {
         mMsgDocument.printWarning("Failed to load preferences for " +
                                   ((TweekBean) bean).getName() +
                                   ": " + io_ex.getMessage());
      }

      mBeanPrefsDialog.addPrefsBean(bean);
   }

   /**
    * Performes the real GUI intialization.  This method is needed for JBuilder
    * to be happy.
    */
   private void jbInit() throws Exception
   {
      int shortcut_mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

      GlobalPreferencesService prefs = new GlobalPreferencesServiceProxy();
      setBeanViewer( prefs.getBeanViewer() );

      mContentPane = (JPanel) this.getContentPane();
      mContentPane.setLayout(mContentPaneLayout);
      this.setSize(new Dimension(prefs.getWindowWidth(),
                                 prefs.getWindowHeight()));
      this.setTitle("Tweek JavaBean Loader");

      // Define the Connect option in the Network menu.
      mMenuNetConnect.setText("Connect to ORB ...");
      mMenuNetConnect.setAccelerator(javax.swing.KeyStroke.getKeyStroke(67, shortcut_mask | KeyEvent.SHIFT_MASK, false));
      mMenuNetConnect.addActionListener(new ActionListener()
         {
            public void actionPerformed (ActionEvent e)
            {
               networkConnectAction(e);
            }
         });

      // Define the Disconnect option in the Network menu.
      mMenuNetDisconnect.setEnabled(false);
      mMenuNetDisconnect.setText("Disconnect from ORB ...");
      mMenuNetDisconnect.addActionListener(new ActionListener()
         {
            public void actionPerformed (ActionEvent e)
            {
               networkDisconnectAction(e);
            }
         });

      // Define the Edit Global option in the Preferences menu.
      if ( ! mMacOS )
      {
         boolean global_prefs_enabled = true;

         try
         {
            new GlobalPreferencesServiceProxy();
         }
         catch(Exception ex)
         {
            global_prefs_enabled = false;
         }

         mMenuPrefsGlobalEdit.setText("Edit Global ...");
         mMenuPrefsGlobalEdit.setEnabled(global_prefs_enabled);
         mMenuPrefsGlobalEdit.addActionListener(new ActionListener()
            {
               public void actionPerformed (ActionEvent e)
               {
                  prefsEditGlobal(e);
               }
            });
      }

      // Define the Edit Local option in the Preferences menu.
      mMenuPrefsBeanEdit.setText("Edit Bean-Specific ...");
      mMenuPrefsBeanEdit.setEnabled(false);
      mMenuPrefsBeanEdit.addActionListener(new ActionListener()
         {
            public void actionPerformed (ActionEvent e)
            {
               prefsEditBean(e);
            }
         });

      mMenuBeansLoad.setText("Load Beans ...");
      mMenuBeansLoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(66, shortcut_mask, false));
      mMenuBeansLoad.addActionListener(new ActionListener ()
         {
            public void actionPerformed (ActionEvent e)
            {
               beansLoadAction(e);
            }
         });

      mMenuBeansStatus.setText("Beans Status ...");
      mMenuBeansStatus.setAccelerator(javax.swing.KeyStroke.getKeyStroke(83, shortcut_mask, false));
      mMenuBeansStatus.addActionListener(new ActionListener ()
         {
            public void actionPerformed (ActionEvent e)
            {
               beansStatusAction(e);
            }
         });


      // Define the About option in the Help menu.
      mMenuHelpAbout.setText("About ...");
      mMenuHelpAbout.addActionListener(new ActionListener ()
         {
            public void actionPerformed(ActionEvent e)
            {
               helpAboutAction(e);
            }
         });

      mMenuFileOpen.setText("Open ...");
      mMenuFileOpen.setEnabled(false);
      mMenuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(79, shortcut_mask, false));
      mMenuFileOpen.addActionListener(new java.awt.event.ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            fileOpenAction(e);
         }
      });

      mMenuFileClose.setEnabled(false);
      mMenuFileClose.setText("Close ...");
      mMenuFileClose.setAccelerator(javax.swing.KeyStroke.getKeyStroke(87, shortcut_mask, false));
      mMenuFileClose.addActionListener(new java.awt.event.ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            fileCloseAction(e);
         }
      });

      // Define the Quit option in the File menu.
      if ( ! mMacOS )
      {
         mMenuFileQuit.setText("Quit");
         mMenuFileQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(81, shortcut_mask, false));
         mMenuFileQuit.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent e)
               {
                  fileQuitAction(e);
               }
            });
      }

      // Set up the File menu.  This adds the Open option, a separator, and
      // the Quit option.
      mMenuFile.setText("File");
      mMenuFileSave.setEnabled(false);
      mMenuFileSave.setText("Save");
      mMenuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(83, shortcut_mask, false));
      mMenuFileSave.addActionListener(new java.awt.event.ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            fileSaveAction(e);
         }
      });
      mMenuFileSaveAs.setEnabled(false);
      mMenuFileSaveAs.setText("Save As ...");
      mMenuFileSaveAs.addActionListener(new java.awt.event.ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            fileSaveAsAction(e);
         }
      });
      mMenuFile.add(mMenuFileOpen);
      mMenuFile.add(mMenuFileSave);
      mMenuFile.add(mMenuFileSaveAs);
      mMenuFile.add(mMenuFileClose);

      if ( ! mMacOS )
      {
         mMenuFile.addSeparator();
         mMenuFile.add(mMenuFileQuit);
      }

      // Set up the Network menu.
      mMenuNetwork.setText("Network");
      mMenuNetwork.add(mMenuNetConnect);
      mMenuNetwork.add(mMenuNetDisconnect);

      // Set up the Preferences menu.
      mMenuPrefs.setText("Preferences");
      if ( ! mMacOS )
      {
         mMenuPrefs.add(mMenuPrefsGlobalEdit);
      }
      mMenuPrefs.add(mMenuPrefsBeanEdit);

      // Set up the Beans menu.
      mMenuBeans.setText("Beans");
      mMenuBeans.add(mMenuBeansLoad);
      mMenuBeans.add(mMenuBeansStatus);

      // Add the About option to the Help menu.
      mMenuHelp.setText("Help");
      mMenuHelp.add(mMenuHelpAbout);

      // Add the menus to the menu bar.
      mMenuBar.add(mMenuFile);
      mMenuBar.add(mMenuNetwork);
      mMenuBar.add(mMenuPrefs);
      mMenuBar.add(mMenuBeans);
      mMenuBar.add(mMenuHelp);

      // Finally, set the menu bar to what we have just defined.
      this.setJMenuBar(mMenuBar);

      mMainPanel.setTopComponent(mBeanContainer);
      mMainPanel.setBottomComponent(null);
      mMainPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
      mMainPanel.setDividerSize(1);

      mStatusBar.setLayout(mStatusBarLayout);
      mStatusBar.setBorder(BorderFactory.createLoweredBevelBorder());
      mProgressBar.setMinimumSize(new Dimension(100, 14));
      mStatusMsgButton.setMinimumSize(new Dimension(24, 24));
      mStatusMsgButton.setToolTipText("Expand or collapse the message panel");

      String bulb_on_icon_name  = "org/vrjuggler/tweek/gui/bulb-on-small.gif";
      String bulb_off_icon_name = "org/vrjuggler/tweek/gui/bulb-off-small.gif";

      try
      {
         mBulbOnIcon = new ImageIcon(BeanJarClassLoader.instance().getResource(bulb_on_icon_name));
      }
      catch (NullPointerException e)
      {
         mMsgDocument.printWarning("WARNING: Failed to load icon " +
                                   bulb_on_icon_name + "\n");
      }

      try
      {
         mBulbOffIcon = new ImageIcon(BeanJarClassLoader.instance().getResource(bulb_off_icon_name));
      }
      catch (NullPointerException e)
      {
         mMsgDocument.printWarning("WARNING: Failed to load icon " +
                                   bulb_off_icon_name + "\n");
      }

      if ( mBulbOffIcon != null )
      {
         mStatusMsgButton.setPreferredSize(new Dimension(24, 24));
         mStatusMsgButton.setIcon(mBulbOffIcon);
      }
      else
      {
         mStatusMsgButton.setText("Expand");
      }

      mStatusMsgButton.addActionListener(new ActionListener()
         {
            public void actionPerformed (ActionEvent e)
            {
               statusMessageExpandAction(e);
            }
         });

      // Add the various components to their respective containers.
      mContentPane.add(mMainPanel, BorderLayout.CENTER);
      mContentPane.add(mStatusBar,  BorderLayout.SOUTH);
      mStatusBar.add(mStatusMsgButton,  BorderLayout.EAST);
      mStatusBar.add(mStatusMsgLabel,  BorderLayout.CENTER);
      mStatusBar.add(mProgressBar, BorderLayout.WEST);
      mMainPanel.setDividerLocation(500);
   }

   private void setMnemonics()
   {
      mMenuNetConnect.setMnemonic('C');
      mMenuNetDisconnect.setMnemonic('D');
      mMenuPrefsGlobalEdit.setMnemonic('G');
      mMenuPrefsBeanEdit.setMnemonic('B');
      mMenuBeansLoad.setMnemonic('L');
      mMenuBeansStatus.setMnemonic('S');
      mMenuHelpAbout.setMnemonic('A');
      mMenuFileOpen.setMnemonic('O');
      mMenuFileClose.setMnemonic('C');
      mMenuFileQuit.setMnemonic('Q');
      mMenuFile.setMnemonic('F');
      mMenuFileSave.setMnemonic('S');
      mMenuFileSaveAs.setMnemonic('A');
      mMenuNetwork.setMnemonic('N');
      mMenuPrefs.setMnemonic('P');
      mMenuBeans.setMnemonic('B');
      mMenuHelp.setMnemonic('H');
   }

   // =========================================================================
   // Protected methods.
   // =========================================================================

   /**
    * Overridden so we can exit when window is closed.
    */
   protected void processWindowEvent (WindowEvent e)
   {
      super.processWindowEvent(e);

      if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
         fileQuitAction(null);
      }
   }

   // ========================================================================
   // Private methods.
   // ========================================================================

   /**
    * File | Open action performed.
    */
   private void fileOpenAction (ActionEvent e)
   {
      if ( mFileLoader.openRequested() )
      {
         mMenuFileClose.setEnabled(true);

         if ( ! mFileLoader.canOpenMultiple() )
         {
            mMenuFileOpen.setEnabled(false);
         }
      }
   }

   /**
    * File | Save action performed.
    */
   private void fileSaveAction (ActionEvent e)
   {
      mFileLoader.saveRequested();
   }

   /**
    * File | Save As action performed.
    */
   private void fileSaveAsAction(ActionEvent e)
   {
      mFileLoader.saveAsRequested();
   }

   /**
    * File | Close action performed.
    */
   private void fileCloseAction (ActionEvent e)
   {
      if ( mFileLoader.closeRequested() )
      {
         mMenuFileOpen.setEnabled(true);

         if ( mFileLoader.getOpenFileCount() == 0 )
         {
            mMenuFileClose.setEnabled(false);
         }
      }
   }

   /**
    * File | Quit action performed.
    */
   private void fileQuitAction (ActionEvent e)
   {
      if ( mBeanContainer != null )
      {
         mBeanContainer.fireFrameClosed();
      }

      for ( int i = 0; i < mORBs.size(); i++ )
      {
         ((CorbaService) mORBs.elementAt(i)).shutdown(true);
      }

      System.exit(0);
   }

   /**
    * Network | Connect action performed.
    */
   private void networkConnectAction (ActionEvent e)
   {
      ConnectionDialog dialog = new ConnectionDialog(this, "ORB Connections");
      dialog.show();

      if ( dialog.getStatus() == ConnectionDialog.OK_OPTION )
      {
         CorbaService corba_service = dialog.getCorbaService();

         if ( null != corba_service &&
              null != corba_service.getSubjectManager() )
         {
            mORBs.add(corba_service);
            mBeanContainer.fireConnectionEvent(corba_service);
            mMenuNetDisconnect.setEnabled(true);
         }
      }
   }

   /**
    * Network | Disconnect action performed.
    */
   private void networkDisconnectAction(ActionEvent e)
   {
      DisconnectionDialog dialog =
         new DisconnectionDialog(this, "Disconnect from CORBA Service", mORBs);
      dialog.show();

      if ( dialog.getStatus() == DisconnectionDialog.DISCONNECT_OPTION )
      {
         try
         {
            CorbaService selected_orb = dialog.getSelectedCorbaService();

            // Tell the Bean container that we are disconnecting from the active
            // ORB, and then take steps to shut down that connection.
            mBeanContainer.fireDisconnectionEvent(selected_orb);
            mORBs.remove(selected_orb);

            // Do not wait for the ORB to shut down.
            selected_orb.shutdown(false);

            // If there are no available ORBs, disable the "disconnect" menu
            // item.
            if ( mORBs.size() == 0 )
            {
               mMenuNetDisconnect.setEnabled(false);
            }
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }
   }

   /**
    * Opens a dialog box used for editing the user's global preferences
    * visually.
    */
   private void prefsEditGlobal(ActionEvent e)
   {
      try
      {
         GlobalPreferencesService prefs = new GlobalPreferencesServiceProxy();

         // Save this for later.
         int old_level = prefs.getUserLevel();

         PrefsDialog dialog = new PrefsDialog(this, "Global Preferences",
                                              prefs);
         dialog.addGlobalPrefsUpdateListener(this);
         dialog.show();
      }
      catch(Exception ex)
      {
         // There is no need to do anything here.  If the GlobalPreferences
         // Bean is not available, the menu option will be disabled, thus
         // preventing this method from ever being executed.
      }
   }

   private void prefsEditBean (ActionEvent e)
   {
      mBeanPrefsDialog.setLocationRelativeTo(this);
      mBeanPrefsDialog.show();
   }

   private void beansLoadAction(ActionEvent e)
   {
      JFileChooser chooser = new JFileChooser();
      chooser.setMultiSelectionEnabled(true);
      chooser.setDialogTitle("Tweek Bean Finder");
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

      ExtensionFileFilter filter = new ExtensionFileFilter("Bean XML Files");
      filter.addExtension("xml");
      chooser.addChoosableFileFilter(filter);

      int status = chooser.showOpenDialog(this);

      if ( status == JFileChooser.APPROVE_OPTION )
      {
         java.io.File[] files = chooser.getSelectedFiles();

         if ( files.length > 0 )
         {
            for ( int i = 0; i < files.length; i++ )
            {
               String path = files[i].getAbsolutePath();
               loadBeansFromPath(path);
            }
         }
      }
   }

   private void beansStatusAction(ActionEvent e)
   {
      BeanStatusDialog dialog = new BeanStatusDialog(this, "Bean Status");
      dialog.show();
   }  

   private void loadBeansFromPath (String path)
   {
      try
      {
         TweekCore.instance().findAndLoadBeans( path );
      }
      catch (BeanPathException path_ex)
      {
         JOptionPane.showMessageDialog(null, "Invalid Bean XML path '" + path + "'",
                                       "Bad Bean Path",
                                       JOptionPane.ERROR_MESSAGE);
      }
   }

   /**
    * Help | About action performed.
    */
   private void helpAboutAction(ActionEvent e)
   {
      AboutBox dlg = new AboutBox(this);
      dlg.setModal(true);
      dlg.show();
   }

   private void statusMessageExpandAction (ActionEvent e)
   {
      if ( mMsgPanelExpanded )
      {
         mMainPanel.setBottomComponent(null);
         mMainPanel.resetToPreferredSizes();
         mMsgPanelExpanded = false;

         mStatusMsgLabel.setText("");

         if ( mStatusMsgButton.getIcon() == null )
         {
            mStatusMsgButton.setText("Expand");
         }
      }
      else
      {
         mMainPanel.setBottomComponent(mMessagePanel);
         mMainPanel.setDividerLocation(0.85);
         mMsgPanelExpanded = true;

         if ( mBulbOffIcon != null )
         {
            mStatusMsgButton.setIcon(mBulbOffIcon);
         }
         else
         {
            mStatusMsgButton.setText("Collapse");
            mStatusMsgButton.setForeground(Color.black);
         }
      }
   }

   /**
    * Disables the menu items in the File menu related to handling of data
    * files.  This effectively resets the menu items to their state before any
    * Beans were loaded in the GUI.
    */
   private void disableFileHandlingItems ()
   {
      mFileLoader = null;
      mMenuFileOpen.setText("Open ...");
      mMenuFileOpen.setEnabled(false);
      mMenuFileSave.setEnabled(false);
      mMenuFileClose.setText("Close ...");
      mMenuFileClose.setEnabled(false);
   }

   // ========================================================================
   // Private data members.
   // ========================================================================

   private boolean mPackFrame = false;

   // GUI objects.
   private JPanel        mContentPane       = null;    /**< Top-level container */
   private BorderLayout  mContentPaneLayout = new BorderLayout();
   private JSplitPane    mMainPanel         = new JSplitPane();
   private BeanContainer mBeanContainer     = new BeanContainer();
   private ViewerBean    mBeanViewer        = null;

   // Status bar stuff.
   private JPanel        mStatusBar        = new JPanel();
   private JProgressBar  mProgressBar      = new JProgressBar();
   private JLabel        mStatusMsgLabel   = new JLabel();
   private JButton       mStatusMsgButton  = new JButton();
   private BorderLayout  mStatusBarLayout  = new BorderLayout();
   private ImageIcon     mBulbOnIcon       = null;
   private ImageIcon     mBulbOffIcon      = null;
   private boolean       mMsgPanelExpanded = false;

   // Menu bar objects.
   private JMenuBar mMenuBar              = new JMenuBar();
   private JMenu mMenuFile                = new JMenu();
   private JMenuItem mMenuFileOpen        = new JMenuItem();
   private JMenuItem mMenuFileSave        = new JMenuItem();
   private JMenuItem mMenuFileSaveAs      = new JMenuItem();
   private JMenuItem mMenuFileClose       = new JMenuItem();
   private JMenuItem mMenuFileQuit        = new JMenuItem();
   private JMenu mMenuNetwork             = new JMenu();
   private JMenuItem mMenuNetConnect      = new JMenuItem();
   private JMenuItem mMenuNetDisconnect   = new JMenuItem();
   private JMenu mMenuPrefs               = new JMenu();
   private JMenuItem mMenuPrefsGlobalEdit = new JMenuItem();
   private JMenuItem mMenuPrefsBeanEdit   = new JMenuItem();
   private JMenu mMenuBeans               = new JMenu();
   private JMenuItem mMenuBeansLoad       = new JMenuItem();
   private JMenuItem mMenuBeansStatus       = new JMenuItem();
   private JMenu mMenuHelp                = new JMenu();
   private JMenuItem mMenuHelpAbout       = new JMenuItem();

   // Networking stuff.
   private Vector mORBs = new Vector();

   private MessagePanel    mMessagePanel    = null;
   private MessageDocument mMsgDocument     = null;
   private FileLoader      mFileLoader      = null;
   private BeanPrefsDialog mBeanPrefsDialog = null;

   private static boolean mMacOS =
      System.getProperty("os.name").toLowerCase().startsWith("mac os x");
   private Object mMacAppListener = null;
}
