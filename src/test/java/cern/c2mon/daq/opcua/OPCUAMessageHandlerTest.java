package cern.c2mon.daq.opcua;

import cern.c2mon.daq.opcua.config.AppConfigProperties;
import cern.c2mon.daq.opcua.control.Controller;
import cern.c2mon.daq.opcua.exceptions.ExceptionContext;
import cern.c2mon.daq.opcua.mapping.TagSubscriptionManager;
import cern.c2mon.daq.opcua.mapping.TagSubscriptionMapper;
import cern.c2mon.daq.opcua.taghandling.*;
import cern.c2mon.daq.opcua.testutils.ExceptionTestEndpoint;
import cern.c2mon.daq.opcua.testutils.MiloMocker;
import cern.c2mon.daq.opcua.testutils.TestEndpoint;
import cern.c2mon.daq.opcua.testutils.TestUtils;
import cern.c2mon.daq.test.GenericMessageHandlerTest;
import cern.c2mon.daq.test.UseConf;
import cern.c2mon.daq.test.UseHandler;
import cern.c2mon.daq.tools.equipmentexceptions.EqCommandTagException;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import cern.c2mon.shared.common.process.EquipmentConfiguration;
import cern.c2mon.shared.common.process.IEquipmentConfiguration;
import cern.c2mon.shared.daq.command.SourceCommandTagValue;
import cern.c2mon.shared.daq.config.ChangeReport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

@UseHandler(OPCUAMessageHandler.class)
public class OPCUAMessageHandlerTest extends GenericMessageHandlerTest {

    ApplicationContext context = createMock(ApplicationContext.class);
    OPCUAMessageHandler handler;
    MiloMocker mocker;
    MessageSender sender = niceMock(MessageSender.class);
    TagSubscriptionManager mapper = new TagSubscriptionMapper();
    TestEndpoint endpoint = new TestEndpoint(sender, mapper);
    AppConfigProperties appConfigProperties = TestUtils.createDefaultConfig();
    Controller controller = TestUtils.getFailoverProxy(endpoint, sender);
    IDataTagHandler dataTagHandler = new DataTagHandler(mapper, sender, controller);
    CommandTagHandler commandTagHandler = new CommandTagHandler(controller);
    DataTagChanger dataTagChanger = new DataTagChanger(dataTagHandler);
    AliveWriter writer = new AliveWriter(controller, sender);

    @Override
    protected void beforeTest() throws Exception {
        appConfigProperties.setRetryDelay(0);
        handler = (OPCUAMessageHandler) msgHandler;
        handler.setContext(context);

        mocker = new MiloMocker(endpoint, mapper);
        configureContextWithController(controller);

        final Collection<ISourceDataTag> tags = handler.getEquipmentConfiguration().getSourceDataTags().values();
        mocker.mockStatusCodeAndClientHandle(StatusCode.GOOD, tags);
        replay(context, endpoint.getMonitoredItem());
        handler.connectToDataSource();
    }

    @Override
    protected void afterTest() throws Exception {

    }

    private void configureContextWithController(Controller controller) {
        expect(context.getBean("controller", Controller.class)).andReturn(controller).anyTimes();
        expect(context.getBean(IDataTagHandler.class)).andReturn(dataTagHandler).anyTimes();
        expect(context.getBean(CommandTagHandler.class)).andReturn(commandTagHandler).anyTimes();
        expect(context.getBean(AppConfigProperties.class)).andReturn(appConfigProperties).anyTimes();
        expect(context.getBean(DataTagChanger.class)).andReturn(dataTagChanger).anyTimes();
        expect(context.getBean(AliveWriter.class)).andReturn(writer).anyTimes();
        expect(context.getBean(MessageSender.class)).andReturn(sender).anyTimes();
    }

    @Test
    @UseConf("mockTest.xml")
    public void refreshShouldTriggerValueUpdateForEachSubscribedTag() {
        resetToNice(sender);
        sender.onValueUpdate(anyLong(), anyObject(), anyObject());
        expectLastCall().times(handler.getEquipmentConfiguration().getSourceDataTags().values().size());
        replay(sender);

        handler.refreshAllDataTags();
        verify(sender);
    }

    @Test
    @UseConf("mockTest.xml")
    public void refreshTagShouldTriggerValueUpdate() {
        resetToNice(sender);
        Capture<Long> idCapture = newCapture(CaptureType.ALL);
        sender.onValueUpdate(captureLong(idCapture), anyObject(), anyObject());
        expectLastCall().once();
        replay(sender);

        handler.refreshDataTag(1L);
        assertEquals(1L, idCapture.getValue());

    }
    @Test
    @UseConf("mockTest.xml")
    public void refreshUnknownTagShouldDoNothing() {
        resetToStrict(sender);
        replay(sender);

        handler.refreshDataTag(-1L);
        verify(sender);
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationUpdateToNewAddressShouldRestartDAQ() {
        EquipmentConfiguration oldConfig = (EquipmentConfiguration) handler.getEquipmentConfiguration();
        EquipmentConfiguration config = oldConfig.clone();
        config.setEquipmentAddress("http://test:2");
        final ChangeReport changeReport = new ChangeReport();

        handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport);
        assertTrue(changeReport.isSuccess());
        assertTrue(changeReport.getInfoMessage().contains("DAQ restarted."));
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationShouldFailIfInterrupted() throws InterruptedException {
        appConfigProperties.setRetryDelay(5000L);
        EquipmentConfiguration oldConfig = (EquipmentConfiguration) handler.getEquipmentConfiguration();
        EquipmentConfiguration config = oldConfig.clone();
        config.setEquipmentAddress("http://test:2");
        final ChangeReport changeReport = new ChangeReport();
        ExecutorService s = Executors.newFixedThreadPool(2);

        s.submit(() -> handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport));
        TimeUnit.MILLISECONDS.sleep(100L);
        s.shutdownNow();
        s.awaitTermination(5000L, TimeUnit.MILLISECONDS);
        assertTrue(changeReport.isFail());
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationUpdateShouldFailOnException() {
        EquipmentConfiguration oldConfig = (EquipmentConfiguration) handler.getEquipmentConfiguration();
        EquipmentConfiguration config = oldConfig.clone();
        config.setEquipmentAddress("http://test:2");
        final ChangeReport changeReport = new ChangeReport();
        reset(context);
        configureContextWithController(TestUtils.getFailoverProxy(new ExceptionTestEndpoint(sender, mapper), sender));
        replay(context);

        handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport);
        assertTrue(changeReport.isFail());
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationUpdateShouldApplyDataTagChanges() {
        final IEquipmentConfiguration oldConfig = handler.getEquipmentConfiguration();
        EquipmentConfiguration config = new EquipmentConfiguration();
        config.setEquipmentAddress(oldConfig.getAddress());
        handler.onUpdateEquipmentConfiguration(config, oldConfig, new ChangeReport());

        assertTrue(mapper.getTagIdDefinitionMap().isEmpty());
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationShouldRestartAliveWriter() {
        appConfigProperties.setAliveWriterEnabled(true);
        EquipmentConfiguration oldConfig = (EquipmentConfiguration) handler.getEquipmentConfiguration();
        EquipmentConfiguration config = oldConfig.clone();
        config.setAliveTagId(2);
        config.setAliveTagInterval(2000L);
        final ChangeReport changeReport = new ChangeReport();
        handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport);

        assertTrue(changeReport.getInfoMessage().contains("Alive Writer updated."));
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationShouldReportBadAliveTagInterval() {
        appConfigProperties.setAliveWriterEnabled(true);
        EquipmentConfiguration oldConfig = (EquipmentConfiguration) handler.getEquipmentConfiguration();
        EquipmentConfiguration config = oldConfig.clone();
        config.setAliveTagId(2);
        final ChangeReport changeReport = new ChangeReport();
        handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport);

        assertTrue(changeReport.getErrorMessage().contains(ExceptionContext.BAD_ALIVE_TAG_INTERVAL.getMessage()));
    }

    @Test
    @UseConf("mockTest.xml")
    public void equipmentConfigurationShouldReportUnknownAliveTag() {
        appConfigProperties.setAliveWriterEnabled(true);
        IEquipmentConfiguration oldConfig = handler.getEquipmentConfiguration();
        EquipmentConfiguration config = new EquipmentConfiguration();
        config.setEquipmentAddress(oldConfig.getAddress());
        config.setAliveTagId(2);
        config.setAliveTagInterval(2000L);
        final ChangeReport changeReport = new ChangeReport();
        handler.onUpdateEquipmentConfiguration(config, oldConfig, changeReport);

        assertTrue(changeReport.getErrorMessage().contains(ExceptionContext.BAD_ALIVE_TAG.getMessage()));
    }

    @Test
    @UseConf("mockTest.xml")
    public void runUnknownCommandShouldThrowException() {
        final SourceCommandTagValue value = new SourceCommandTagValue();
        value.setId(-1L);
        assertThrows(EqCommandTagException.class, ()-> handler.runCommand(value));
    }

    @Test
    @UseConf("mockTest.xml")
    public void runKnownCommandShouldCallCommandTagHandlerWithTag() throws EqCommandTagException {
        final SourceCommandTagValue value = new SourceCommandTagValue();
        value.setId(20L);
        assertNull(handler.runCommand(value));
    }
}
