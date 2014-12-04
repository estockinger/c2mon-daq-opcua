/******************************************************************************
 * This file is part of the Technical Infrastructure Monitoring (TIM) project.
 * See http://ts-project-tim.web.cern.ch
 * 
 * Copyright (C) 2005 - 2014 CERN This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received
 * a copy of the GNU General Public License along with this program; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 * 
 * Author: TIM team, tim.support@cern.ch
 *****************************************************************************/

package cern.c2mon.daq.opcua.connection.common.impl;

import java.net.URI;
import java.net.URISyntaxException;

import cern.c2mon.daq.opcua.connection.common.AbstractOPCUAAddress;

/**
 * The OPCUA specific address for siemens
 * 
 * @author Nacho Vilches
 * 
 */
public final class OPCUASiemensAddress extends AbstractOPCUAAddress {

    /**
     * Internal variable name to access the value provided by Siemens for showing the status of the PLC
     * 
     * Possible values are:
     *  0 Initialization
     *  1 Master Server
     *  2 Standby Server
     *  3 Redundancy error
     *  4 No redundancy
     */
    private String redundantServerStateName;
    
    /**
     * Private constructor. Use the Builder class to create an instance.
     * 
     * @param builder The builder to create an instance of this class.
     */
    private OPCUASiemensAddress(final BuilderSiemens builderSiemens) {
        this.uri = builderSiemens.getUri();
        this.serverRetryTimeout = builderSiemens.getServerRetryTimeout();
        this.serverTimeout = builderSiemens.getServerTimeout();
        this.user = builderSiemens.getUser();
        this.password = builderSiemens.getPassword();
        this.domain = builderSiemens.getDomain();
        this.aliveWriter = builderSiemens.isAliveWriter();
        this.redundantServerStateName = builderSiemens.redundantServerStateName;
    }
    
    /**
     * @return the redundantServerStateName
     */
    public String redundantServerStateName() {
        return redundantServerStateName;
    }
    
    /**
     * BuilderSiemens class.
     * 
     * @author Nacho Vilches
     *
     */
    public static class BuilderSiemens extends AbstractBuilder {
        
        /**
         * Internal variable name to access the value provided by Siemens for showing the status of the PLC
         */
        private String redundantServerStateName;
        
        /**
         * 
         * @param uri
         * @param serverTimeout
         * @param serverRetryTimeout
         * @param redundantServerStateName
         * @throws URISyntaxException
         */
        public BuilderSiemens(String uri, int serverTimeout, int serverRetryTimeout, String redundantServerStateName) 
                throws URISyntaxException {
            this(new URI(uri), serverTimeout, serverRetryTimeout, redundantServerStateName);
        }
        
       /**
        * 
        * @param uri
        * @param serverTimeout
        * @param serverRetryTimeout
        * @param redundantServerStateName
        * @throws URISyntaxException
        */
        public BuilderSiemens(URI uri, int serverTimeout, int serverRetryTimeout, String redundantServerStateName) 
                throws URISyntaxException {
            this.uri = uri;
            this.serverTimeout = serverTimeout;
            this.serverRetryTimeout = serverRetryTimeout;
            this.redundantServerStateName = redundantServerStateName;
        }
        
        /**
         * Sets the domain to authenticate to.
         * 
         * @param domain The domain name.
         * @return The Builder object itself to chain the calls.
         */
        public BuilderSiemens RedundantServerStateName(final String redundantServerStateName) {
            this.redundantServerStateName = redundantServerStateName;
            return this;
        }
        
        /**
         * 
         * @return
         */
        public final String getRedundantServerStateName() {
            return redundantServerStateName;
        }

        /**
         * Builds the OPCUAAddress Siemens object based on the provided parameters.
         * 
         * @return The new OPCAddress Siemens object.
         */
        @Override
        public OPCUASiemensAddress build() {
            return new OPCUASiemensAddress(this);
        }
    }
}