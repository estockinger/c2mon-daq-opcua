package it;

import cern.c2mon.daq.opcua.downstream.*;
import cern.c2mon.daq.opcua.mapping.TagSubscriptionMapperImpl;
import cern.c2mon.daq.opcua.upstream.EventPublisher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ServerStartupCheckerThread implements Runnable {
    private String address;
    private Certifier certifier;
    @Getter
    private Thread thread;

    public ServerStartupCheckerThread(String address, Certifier certifier) {
        this.address = address;
        this.certifier = certifier;
        thread = new Thread(this, address);
        thread.start();
    }

    @SneakyThrows
    @Override
    public void run() {
        synchronized (this) {
            Endpoint endpoint = new EndpointImpl(
                    new MiloClientWrapperImpl(address, certifier),
                    new TagSubscriptionMapperImpl(),
                    new EventPublisher());

            boolean serverRunning = false;
            while (!serverRunning) {
                try {
                    endpoint.initialize(false);
                    serverRunning = endpoint.isConnected();
                } catch (Exception e) {
                    Thread.sleep(100);
                    log.debug("Server not yet ready");
                    //Server not yet ready
                }
            }
            endpoint.reset();
            notify();
        }
    }
}
