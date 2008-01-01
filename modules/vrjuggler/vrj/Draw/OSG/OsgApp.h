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

#ifndef _VRJ_OSG_APP_COMPAT_H_
#define _VRJ_OSG_APP_COMPAT_H_

/**
 * \file
 *
 * This file is for backwards compatibility with VR Juggler 2.x code. New
 * code should not include this header and should use vrj/Draw/OSG/App.h.
 *
 * @deprecated This file has been deprecated in favor of vrj/Draw/OSG/App.h.
 */

#if defined(__GNUC__)
#warning "This header is deprecated. Use vrj/Draw/OSG/App.h instead."
#endif

#include <vrj/Draw/OSG/App.h>


namespace vrj
{

/**
 * Backwards compatibility type declaration for vrj::opensg::App.
 *
 * @since 2.3.11
 *
 * @deprecated vrj::OsgApp has been deprecated in favor of vrj::osg::App.
 */
typedef vrj::osg::App OsgApp;

} // End of vrj namespace


#endif
