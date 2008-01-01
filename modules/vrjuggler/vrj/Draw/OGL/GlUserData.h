/*************** <auto-copyright.pl BEGIN do not edit this line> **************
 *
 * VR Juggler is (C) Copyright 1998-2008 by Iowa State University
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

#ifndef _VRJ_GL_USER_DATA_H_
#define _VRJ_GL_USER_DATA_H_

/**
 * \file
 *
 * This file is for backwards compatibility with VR Juggler 2.x code. New
 * code should not include this header and should use
 * vrj/Draw/OpenGL/UserData.h.
 *
 * @deprecated This file has been deprecated in favor of
 *             vrj/Draw/OpenGL/UserData.h.
 */

#if defined(__GNUC__)
#warning "This header is deprecated. Use vrj/Draw/OpenGL/UserData.h instead."
#endif

#include <vrj/Draw/OpenGL/UserData.h>


namespace vrj
{

/**
 * Backwards compatibility type declaration for vrj::opengl::UserData.
 *
 * @since 2.3.11
 *
 * @deprecated vrj::GlUserData has been deprecated in favor of
 *             vrj::opengl::UserData.
 */
typedef vrj::opengl::UserData GlUserData;

}


#endif
