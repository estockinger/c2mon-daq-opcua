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

/**
 * Helper object to identify groups in the default groupig strategy.
 * Basically it says that if they have the same valueDeadband and timeDeadband
 * they are the same.
 * 
 * @author Andreas Lang
 *
 */
public class DeadBandGroupIdentifier {

    /**
     * The value deadband of the identifier.
     */
    private final float valueDeadband;

    /**
     * The timedeadband of the identifier.
     */
    private final int timeDeadband;

    /**
     * Creates a new DeadbandGroupIdentifier.
     * 
     * @param valueDeadband The value deadband to use.
     * @param timeDeadband The time deadband to use.
     */
    public DeadBandGroupIdentifier(final float valueDeadband, 
            final int timeDeadband) {
        this.valueDeadband = valueDeadband;
        this.timeDeadband = timeDeadband;
    }

    /**
     * @return the timeDeadband
     */
    public int getTimeDeadband() {
        return timeDeadband;
    }

    /**
     * @return the valueDeadband
     */
    public float getValueDeadband() {
        return valueDeadband;
    }

    /**
     * Overriden hash code method.
     * 
     * @return The hash code of this object.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + timeDeadband;
        result = prime * result + Float.floatToIntBits(valueDeadband);
        return result;
    }

    /**
     * Overriden equals method. Equals if the provided object is of type
     * DeadBandGroupIdentifier and the time deadband equals the time deadband
     * of this object as well as the value deadband of the provided object
     * equals the value deadband of this object.
     * 
     * @param obj The object to compare to.
     * @return True if the provided object equals this object else false.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeadBandGroupIdentifier other = (DeadBandGroupIdentifier) obj;
        if (timeDeadband != other.timeDeadband)
            return false;
        if (Float.floatToIntBits(valueDeadband) 
                != Float.floatToIntBits(other.valueDeadband))
            return false;
        return true;
    }
}

