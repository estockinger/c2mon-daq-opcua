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

import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagSubscriptionManagerTest extends MappingBase {

    @Test
    public void getGroupForTagDoesNotAddTagToGroup() {
        SubscriptionGroup group = mapper.getGroup(tag.getTimeDeadband());
        assertFalse(group.contains(tag.getId()));
    }

    @Test
    public void toGroupCreatesProperDeadband() {
        SubscriptionGroup group = mapper.getGroup(tag.getTimeDeadband());
        assertEquals(tag.getTimeDeadband(), group.getPublishInterval());
    }

    @Test
    public void secondExecutionOfTagToGroupShouldReturnPriorGroup() {
        SubscriptionGroup group1 = mapper.getGroup(tag.getTimeDeadband());
        SubscriptionGroup group2 = mapper.getGroup(tag.getTimeDeadband());
        assertEquals(group1, group2);
    }

    @Test
    public void secondExecutionOfTagToDefinitionShouldReturnPriorDefinition() {
        ItemDefinition definition1 = mapper.getOrCreateDefinition(tag);
        ItemDefinition definition2 = mapper.getOrCreateDefinition(tag);
        assertEquals(definition1, definition2);
    }

    @Test
    public void getOrCreateDefinitionShouldCreateProperDefinition() {
        ItemDefinition expected = ItemDefinition.of(tag);
        ItemDefinition actual = mapper.getOrCreateDefinition(tag);

        assertEquals(expected.getNodeId(), actual.getNodeId());
        assertEquals(expected.getMethodNodeId(), actual.getMethodNodeId());
        assertEquals(expected.getTimeDeadband(), actual.getTimeDeadband());
        assertEquals(expected.getValueDeadband(), actual.getValueDeadband());
        assertEquals(expected.getValueDeadbandType(), actual.getValueDeadbandType());
    }

    @Test
    public void tagsWithSameDeadbandToGroupShouldReturnSameGroup() {
        SubscriptionGroup group1 = mapper.getGroup(tag.getTimeDeadband());
        SubscriptionGroup group2 = mapper.getGroup(tagWithSameDeadband.getTimeDeadband());

        assertEquals(group1, group2);
    }

    @Test
    public void tagsWithDifferentDeadbandsToGroupReturnDifferentGroup() {
        SubscriptionGroup group1 = mapper.getGroup(tag.getTimeDeadband());
        SubscriptionGroup group2 = mapper.getGroup(tagWithDifferentDeadband.getTimeDeadband());

        assertNotEquals(group1, group2);
    }

    @Test
    public void removeNewTagShouldReturnFalse() {
        assertFalse(mapper.removeTag(tag.getId()));
    }

    @Test
    public void removeTagFromEmptyGroupShouldReturnFalse() {
        mapper.getGroup(tag.getTimeDeadband());
        assertFalse(mapper.removeTag(tag.getId()));
    }

    @Test
    public void removeTagFromGroupShouldDeleteCorrespondingDefinition() {
        SubscriptionGroup group = mapper.getGroup(tag.getTimeDeadband());
        mapper.addTagToGroup(tag.getId());
        mapper.removeTag(tag.getId());
        assertFalse(group.contains(tag.getId()));
    }

    @Test
    public void removeExistingTagShouldReturnTrue() {
        mapper.getOrCreateDefinition(tag);
        mapper.addTagToGroup(tag.getId());
        assertTrue(mapper.removeTag(tag.getId()));
    }

    @Test
    public void getDefinitionShouldReturnDefinitionWithCorrespondingTagReference() {
        ItemDefinition definition = mapper.getOrCreateDefinition(tag);

        assertEquals(tag.getTimeDeadband(), definition.getTimeDeadband());
        assertEquals(tag.getValueDeadband(), definition.getValueDeadband());
        assertEquals(tag.getValueDeadbandType(), definition.getValueDeadbandType());
    }

    @Test
    public void getDefinitionWithSameTagShouldReturnSameDefinitionTwice() {
        ItemDefinition definition = mapper.getOrCreateDefinition(tag);
        final ItemDefinition definition2 = mapper.getOrCreateDefinition(tag);

        assertEquals(definition.getTimeDeadband(), definition2.getTimeDeadband());
        assertEquals(definition.getValueDeadband(), definition2.getValueDeadband());
        assertEquals(definition.getValueDeadbandType(), definition2.getValueDeadbandType());
    }

    @Test
    public void getDefinitionWithDifferentTagsShouldReturnDifferentDefinitions() {
        assertNotEquals(mapper.getOrCreateDefinition(tag), mapper.getOrCreateDefinition(tagWithSameDeadband));
    }

    @Test
    public void registerDefinitionInGroupShouldAddDefinitionToGroup() {
        mapper.getOrCreateDefinition(tag);
        SubscriptionGroup group = mapper.getGroup(tag.getTimeDeadband());
        mapper.addTagToGroup(tag.getId());
        assertTrue(group.contains(tag.getId()));
    }

    @Test
    public void registerDefinitionTwiceShouldThrowError() {
        mapper.getOrCreateDefinition(tag);
        mapper.addTagToGroup(tag.getId());
        final int size = mapper.getGroup(tag.getTimeDeadband()).size();
        mapper.addTagToGroup(tag.getId());
        assertEquals(size, mapper.getGroup(tag.getTimeDeadband()).size());
    }

    @Test
    public void registerDefinitionsInGroupShouldAddAllDefinitionsToGroup() {
        mapper.getOrCreateDefinition(tag);
        mapper.getOrCreateDefinition(tagWithSameDeadband);
        SubscriptionGroup group = mapper.getGroup(tag.getTimeDeadband());

        mapper.addTagToGroup(tag.getId());
        mapper.addTagToGroup(tagWithSameDeadband.getId());

        assertTrue(group.contains(tag.getId()));
        assertTrue(group.contains(tagWithSameDeadband.getId()));
    }

    @Test
    public void getTagByRandomClientHandleShouldReturnNull() {
        assertNull(mapper.getTagId(UInteger.valueOf(2)));
    }

    @Test
    public void getTagByClientHandleShouldReturnProperTagId() {
        ItemDefinition definition = mapper.getOrCreateDefinition(tag);
        Long actual = mapper.getTagId(definition.getClientHandle());
        assertEquals(tag.getId(), actual);
    }
}
