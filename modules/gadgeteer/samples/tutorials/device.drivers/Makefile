# ************** <auto-copyright.pl BEGIN do not edit this line> **************
#
# VR Juggler is (C) Copyright 1998-2010 by Iowa State University
#
# Original Authors:
#   Allen Bierbaum, Christopher Just,
#   Patrick Hartling, Kevin Meinert,
#   Carolina Cruz-Neira, Albert Baker
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Library General Public
# License as published by the Free Software Foundation; either
# version 2 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Library General Public License for more details.
#
# You should have received a copy of the GNU Library General Public
# License along with this library; if not, write to the
# Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
# Boston, MA 02110-1301, USA.
#
# *************** <auto-copyright.pl END do not edit this line> ***************

# -----------------------------------------------------------------------------
# Example makefile for the example device driver described in the Gadgeteer
# Device Driver Authoring Guide.
# -----------------------------------------------------------------------------

srcdir=		.
BUILD_TYPE=	dbg
#BUILD_TYPE=	opt

DRIVER_NAME=	button
SRCS=		buttondevice.cpp

GADGET_BASE_DIR=$(shell flagpoll gadgeteer --get-prefix)
GADGET_VERSION=	$(shell flagpoll gadgeteer --modversion)

include $(GADGET_BASE_DIR)/share/gadgeteer-$(GADGET_VERSION)/gadget.driver.mk
