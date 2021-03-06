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
package cern.c2mon.daq.opcua;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import cern.c2mon.daq.opcua.connection.common.IOPCEndpointListener;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.datatag.address.OPCHardwareAddress;
import cern.c2mon.shared.common.type.TypeConverter;

/**
 * Listener for endpoint events. Makes sure all important events are logged.
 *
 * @author Andreas Lang
 */
@Slf4j
public class EndpointEquipmentLogListener implements IOPCEndpointListener {

  /**
   * Creates a new endpoint log listener.
   */
  public EndpointEquipmentLogListener() {
  }

  /**
   * Logs an onNewTagValue event. The log level is debug.
   *
   * @param dataTag   The tag which has a value update.
   * @param timestamp The timestamp when the tag changed.
   * @param tagValue  The updated value of the tag.
   */
  @Override
  public void onNewTagValue(final ISourceDataTag dataTag, final long timestamp, final Object tagValue) {
    Object convertedValue = TypeConverter.cast(tagValue, dataTag.getDataType());

    if (log.isDebugEnabled()) {
      log.debug("Original value: '" + (tagValue != null ? tagValue.toString() : "null") + "', Tag type: '" + dataTag.getDataType() + "', Original type: '"
              + (tagValue != null ? tagValue.getClass().getSimpleName() : "null") + "'");
      log.debug("New tag value (ID: '" + dataTag.getId() + "'," + " converted value: '" + convertedValue + "', converted type: '"
              + (convertedValue != null ? convertedValue.getClass().getSimpleName() : "null") + "', Timestamp: '" + timestamp + " " + new Date(timestamp) + "').");
    }
  }

  /**
   * Logs an error subscription exception.
   *
   * @param cause The cause of the subscription failing.
   */
  @Override
  public void onSubscriptionException(final Throwable cause) {
    log.error("Exception in OPC subscription.", cause);
  }

  /**
   * Logs a warning for an invalid tag.
   *
   * @param dataTag The invalid tag.
   * @param cause   The cause of the tag to be invalid.
   */
  @Override
  public void onTagInvalidException(final ISourceDataTag dataTag, final Throwable cause) {
    log.warn("Tag with id '" + dataTag.getId() + "' caused exception. " + "Check configuration. Address: "
            + ((OPCHardwareAddress) dataTag.getHardwareAddress()).getOPCItemName(), cause);
  }

}
