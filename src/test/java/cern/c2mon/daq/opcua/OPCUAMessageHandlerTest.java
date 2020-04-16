package cern.c2mon.daq.opcua;

import cern.c2mon.daq.common.messaging.IProcessMessageSender;
import cern.c2mon.daq.opcua.connection.ControllerProxy;
import cern.c2mon.daq.opcua.connection.Endpoint;
import cern.c2mon.daq.opcua.exceptions.ConfigurationException;
import cern.c2mon.daq.opcua.exceptions.OPCCommunicationException;
import cern.c2mon.daq.opcua.testutils.MiloMocker;
import cern.c2mon.daq.opcua.testutils.MiloTestClientWrapper;
import cern.c2mon.daq.test.GenericMessageHandlerTest;
import cern.c2mon.daq.test.UseConf;
import cern.c2mon.daq.test.UseHandler;
import cern.c2mon.daq.tools.equipmentexceptions.EqIOException;
import lombok.extern.slf4j.Slf4j;
import org.easymock.Capture;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.junit.Test;

import static cern.c2mon.daq.opcua.exceptions.ConfigurationException.Cause.ADDRESS_MISSING_PROPERTIES;
import static cern.c2mon.daq.opcua.upstream.EndpointListener.EquipmentState.CONNECTION_FAILED;
import static cern.c2mon.daq.opcua.upstream.EndpointListener.EquipmentState.OK;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@UseHandler(OPCUAMessageHandler.class)
public class OPCUAMessageHandlerTest extends GenericMessageHandlerTest {

    OPCUAMessageHandler handler;
    CommfaultSenderCapture capture;

    @Override
    protected void beforeTest () throws Exception {
        handler = (OPCUAMessageHandler) msgHandler;
        capture = new CommfaultSenderCapture(messageSender);
        ControllerProxy proxy = new ControllerProxy();
        handler.setProxy(proxy);
    }

    @Override
    protected void afterTest () throws Exception {
    }

    @Test
    @UseConf("commfault_ok.xml")
    public void properConfigShouldSendOKCommfault () throws EqIOException {
        mockSuccessfulConnectionAndReplay();
        handler.connectToDataSource();

        capture.verifyCapture(107211L,
                handler.getEquipmentConfiguration().getName()+":COMM_FAULT",
                OK.message);
    }

    @Test
    @UseConf("bad_address_string.xml")
    public void badAddressStringShouldThrowError () {
        replay(messageSender);
        assertThrows(ConfigurationException.class,
                () -> handler.connectToDataSource(),
                ADDRESS_MISSING_PROPERTIES.message);
    }

    @Test
    @UseConf("commfault_incorrect.xml")
    public void improperConfigShouldSendIncorrectCommfault () throws EqIOException {
        replay(messageSender);
        try {
            handler.connectToDataSource();
        } catch (Exception e) {
            //Ignore errors, we only care about the message
        }

        capture.verifyCapture(107211L,
                handler.getEquipmentConfiguration().getName()+":COMM_FAULT",
                CONNECTION_FAILED.message);
    }

    @Test
    @UseConf("commfault_incorrect.xml")
    public void improperConfigShouldThrowError () {
        replay(messageSender);
        assertThrows(OPCCommunicationException.class, () -> handler.connectToDataSource());
    }

    public static class CommfaultSenderCapture {
        Capture<Long> id = newCapture();
        Capture<String> tagName = newCapture();
        Capture<Boolean> val = newCapture();
        Capture<String> msg= newCapture();

        public CommfaultSenderCapture(IProcessMessageSender sender) {
            sender.sendCommfaultTag(captureLong(id), capture(tagName), captureBoolean(val), capture(msg));
            expectLastCall().once();
        }

        public void verifyCapture(Long id, String tagName, String msg) {
            assertEquals(id, this.id.getValue().longValue());
            assertEquals(tagName, this.tagName.getValue());
            assertEquals(msg, this.msg.getValue());
        }
    }

    private void mockSuccessfulConnectionAndReplay() {
        MiloTestClientWrapper wrapper = new MiloTestClientWrapper();
        Endpoint endpoint = handler.getProxy().getController().getEndpoint();
        endpoint.setWrapper(wrapper);

        MiloMocker mocker = new MiloMocker(wrapper, endpoint.getMapper());
        mocker.mockStatusCodeAndClientHandle(StatusCode.GOOD, equipmentConfiguration.getSourceDataTags().values());
        replay(messageSender, wrapper.getMonitoredItem());
    }
}
