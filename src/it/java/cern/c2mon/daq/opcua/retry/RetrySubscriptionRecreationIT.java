package cern.c2mon.daq.opcua.retry;

import cern.c2mon.daq.opcua.exceptions.CommunicationException;
import cern.c2mon.daq.opcua.exceptions.ConfigurationException;
import cern.c2mon.daq.opcua.exceptions.ExceptionContext;
import cern.c2mon.daq.opcua.mapping.TagSubscriptionMapper;
import cern.c2mon.daq.opcua.testutils.EdgeTagFactory;
import cern.c2mon.shared.common.datatag.ISourceDataTag;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static cern.c2mon.daq.opcua.testutils.EdgeTagFactory.AlternatingBoolean;
import static cern.c2mon.daq.opcua.testutils.EdgeTagFactory.RandomUnsignedInt32;
import static org.easymock.EasyMock.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:retry.properties")
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RetrySubscriptionRecreationIT {

    @Autowired
    TagSubscriptionMapper mapper;
    @Autowired UaSubscriptionManager.SubscriptionListener endpoint;
    @Value("${app.retryDelay}") int delay;

    private final OpcUaClient clientMock = createNiceMock(OpcUaClient.class);
    private final UaSubscription subscriptionMock = createNiceMock(UaSubscription.class);
    private final OpcUaSubscriptionManager managerMock = createNiceMock(OpcUaSubscriptionManager.class);

    @BeforeEach
    public void setUp() throws ConfigurationException {
        resetToNice(clientMock, managerMock, subscriptionMock);
        ReflectionTestUtils.setField(endpoint, "client", clientMock);
        BiMap<Integer, UaSubscription> subscriptionMap = HashBiMap.create();
        subscriptionMap.put(0, subscriptionMock);
        ReflectionTestUtils.setField(endpoint, "subscriptionMap", subscriptionMap);
        mapper.clear();
        setUpMapper();
    }

    @Test
    public void stopCallingOnInterruptedThread() throws InterruptedException {
        int numAttempts = 3;
        mockNAttempts(numAttempts, new CommunicationException(ExceptionContext.CREATE_SUBSCRIPTION));
        final Thread thread = new Thread(() -> endpoint.onSubscriptionTransferFailed(subscriptionMock, StatusCode.GOOD));
        thread.start();

        // add buffer
        TimeUnit.MILLISECONDS.sleep(Math.round(delay * (numAttempts + 1.5)));
        thread.interrupt();
        verify(subscriptionMock, clientMock, managerMock);
    }

    @Test
    public void callOnceOnConfigurationException() {
        // UnknownHostException is thrown as ConfigurationException
        mockNAttempts(1, new UnknownHostException());
        endpoint.onSubscriptionTransferFailed(subscriptionMock, StatusCode.GOOD);
        verify(subscriptionMock, clientMock, managerMock);
    }

    @Test
    public void statusChangedShouldNotRetryIfStatusCodeIsBadButNotTimeout() {
        replay(subscriptionMock, clientMock, managerMock);
        endpoint.onStatusChanged(subscriptionMock, StatusCode.BAD);
        verify(subscriptionMock, clientMock, managerMock);
    }

    private void mockNAttempts(int numAttempts, Exception e) {
        final CompletableFuture<UaSubscription> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(e);
        expect(subscriptionMock.getSubscriptionId())
                .andReturn(UInteger.valueOf(1))
                .anyTimes();
        expect(clientMock.getSubscriptionManager())
                .andReturn(managerMock)
                .anyTimes();
        expect(managerMock.deleteSubscription(anyObject()))
                .andReturn(CompletableFuture.completedFuture(subscriptionMock))
                .anyTimes();
        expect(managerMock.createSubscription(anyDouble()))
                .andReturn(failedFuture)
                .times(numAttempts);
        replay(subscriptionMock, clientMock, managerMock);
    }

    private void setUpMapper() throws ConfigurationException {
        for (EdgeTagFactory tagConfig : new EdgeTagFactory[]{AlternatingBoolean, RandomUnsignedInt32}) {
            final ISourceDataTag tag = tagConfig.createDataTag();
            mapper.getOrCreateDefinition(tag);
            mapper.addTagToGroup(tag.getId());
        }
    }
}
