package cern.c2mon.daq.opcua.connection;

import cern.c2mon.daq.opcua.address.EquipmentAddress;
import cern.c2mon.daq.opcua.exceptions.EndpointTypesUnknownException;
import cern.c2mon.daq.opcua.exceptions.OPCCommunicationException;
import cern.c2mon.shared.common.datatag.ISourceDataTag;

import java.util.List;
import java.util.Optional;

public interface EndpointController {

    String UA_TCP_TYPE = "opc.tcp";

    void initialize() throws EndpointTypesUnknownException, OPCCommunicationException;
    void subscribe (EndpointListener listener);
    void stop();
    void checkConnection();
    void refreshAllDataTags();
    void refreshDataTag(ISourceDataTag sourceDataTag);
    String updateAliveWriterAndReport ();

    static EquipmentAddress getEquipmentAddress (List<EquipmentAddress> equipmentAddresses) {
        Optional<EquipmentAddress> matchingAddress = equipmentAddresses.stream().filter(o -> o.getProtocol().equals(EndpointController.UA_TCP_TYPE)).findFirst();
        if (!matchingAddress.isPresent()) {
            throw new EndpointTypesUnknownException("No supported protocol found. createEndpoint - Endpoint creation for '" + equipmentAddresses + "' failed. Stop Startup.");
        }
        return matchingAddress.get();
    }
}
