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

import cern.c2mon.shared.common.datatag.ISourceDataTag;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mapper maintains an internal state of the tags that are subscribed on the server(s) in {@link
 * SubscriptionGroup}s, and maps in between the {@link ItemDefinition} containing the Milo-compatible {@link
 * org.eclipse.milo.opcua.stack.core.types.builtin.NodeId}s, and {@link ISourceDataTag}s.
 */
@NoArgsConstructor
@Component("mapper")
@Getter
public class TagSubscriptionMapper implements TagSubscriptionManager {

    private final Map<Integer, SubscriptionGroup> subscriptionGroups = new ConcurrentHashMap<>();
    private final BiMap<Long, ItemDefinition> tagIdDefinitionMap = HashBiMap.create();

    @Override
    public SubscriptionGroup getGroup(int timeDeadband) {
        if (groupExists(timeDeadband)) {
            return subscriptionGroups.get(timeDeadband);
        } else {
            SubscriptionGroup group = new SubscriptionGroup(timeDeadband);
            subscriptionGroups.put(timeDeadband, group);
            return group;
        }
    }

    @Override
    public Collection<SubscriptionGroup> getGroups() {
        return subscriptionGroups.values();
    }

    @Override
    public ItemDefinition getDefinition(Long tagId) {
        return tagIdDefinitionMap.get(tagId);
    }

    @Override
    public Long getTagId(UInteger clientHandle) {
        return tagIdDefinitionMap.entrySet().stream()
                .filter(e -> e.getValue().getClientHandle().equals(clientHandle))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Override
    public ItemDefinition getOrCreateDefinition(ISourceDataTag tag) {
        if (tagIdDefinitionMap.containsKey(tag.getId())) {
            return tagIdDefinitionMap.get(tag.getId());
        } else {
            final ItemDefinition definition = ItemDefinition.of(tag);
            tagIdDefinitionMap.put(tag.getId(), definition);
            return definition;
        }
    }

    @Override
    public void addTagToGroup(long tagId) {
        if (tagIdDefinitionMap.containsKey(tagId)) {
            final SubscriptionGroup group = getGroup(tagIdDefinitionMap.get(tagId).getTimeDeadband());
            group.add(tagId, tagIdDefinitionMap.get(tagId));
        }
    }

    @Override
    public boolean removeTag(long tagId) {
        ItemDefinition definition = tagIdDefinitionMap.remove(tagId);
        if (definition == null) {
            return false;
        }
        final SubscriptionGroup group = subscriptionGroups.get(definition.getTimeDeadband());
        return group != null && group.remove(tagId);
    }

    @Override
    public void clear() {
        tagIdDefinitionMap.clear();
        subscriptionGroups.clear();
    }

    private boolean groupExists(int deadband) {
        return subscriptionGroups.get(deadband) != null;
    }
}
