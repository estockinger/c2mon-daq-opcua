/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.daq.opcua.connection.common.impl;

import org.junit.Test;

import cern.c2mon.daq.opcua.connection.common.impl.ItemDefinition;
import cern.c2mon.daq.opcua.connection.common.impl.SubscriptionGroup;
import static org.junit.Assert.*;

public class SubscriptionGroupTest {
    
    private static final float VALUE_DEADBAND = 0.1f;
    private static final int TIME_DEADBAND = 0;
    private static final float DIF = 0.000000001F;
    
    private SubscriptionGroup<ItemDefinition<String>> group =
        new SubscriptionGroup<ItemDefinition<String>>(TIME_DEADBAND, VALUE_DEADBAND);
    
    @Test
    public void testDeadbands() {
        assertEquals(TIME_DEADBAND, group.getTimeDeadband());
        assertTrue(VALUE_DEADBAND - group.getValueDeadband() < DIF);
    }

    @Test
    public void testAddAddress() {
        ItemDefinition<String> address1 = new ItemDefinition<String>(1L, "asd");
        ItemDefinition<String> address2 = new ItemDefinition<String>(2L, "asd");
        ItemDefinition<String> addressNull = null;
        group.addDefintion(address1);
        group.addDefintion(address2);
        group.addDefintion(addressNull);
        assertEquals(2, group.getDefintions().size());
        assertTrue(group.getDefintions().contains(address1));
        assertTrue(group.getDefintions().contains(address2));
    }
    
    @Test
    public void testRemoveAddress() {
        ItemDefinition<String> address1 = new ItemDefinition<String>(1L, "asd");
        ItemDefinition<String> address2 = new ItemDefinition<String>(2L, "asd");
        ItemDefinition<String> addressNull = null;
        group.addDefintion(address1);
        group.addDefintion(address2);
        assertEquals(2, group.getDefintions().size());
        assertTrue(group.getDefintions().contains(address1));
        assertTrue(group.getDefintions().contains(address2));
        
        // null address remove nothing should happen
        group.removeDefintion(addressNull);
        assertEquals(2, group.getDefintions().size());
        assertTrue(group.getDefintions().contains(address1));
        assertTrue(group.getDefintions().contains(address2));
        
        group.removeDefintion(address2);
        assertEquals(1, group.getDefintions().size());
        assertTrue(group.getDefintions().contains(address1));
        assertFalse(group.getDefintions().contains(address2));
        
        group.removeDefintion(address1);
        assertEquals(0, group.getDefintions().size());
        assertFalse(group.getDefintions().contains(address1));
        assertFalse(group.getDefintions().contains(address2));
    }
}
