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
package cern.c2mon.daq.opcua.mapping;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionGroup{

    @Getter
    private final List<ItemDefinition> definitions = new ArrayList<>();

    @Getter
    @Setter
    private UaSubscription subscription;

    @Getter
    private final Deadband deadband;

    public SubscriptionGroup(Deadband deadband) {
        this.deadband = deadband;
    }

    public int size() {
        return this.definitions.size();
    }

    public boolean isSubscribed() {
        return this.subscription != null;
    }

    public void add(final ItemDefinition itemDefinition) {
        if (this.definitions.contains(itemDefinition)) {
            throw new IllegalArgumentException("The item definition has already been subscribed.");
        }

        if (!Deadband.of(itemDefinition.getTag()).equals(deadband)) {
            throw new IllegalArgumentException("This item does not belong to this group.");
        }

        this.definitions.add(itemDefinition);
    }

    public void reset() {
        definitions.clear();
        subscription = null;
    }

    public void remove(final ItemDefinition itemDefinition) {
        this.definitions.remove(itemDefinition);
    }

    public boolean contains(ItemDefinition definition) {
        return definitions.contains(definition);
    }

}
