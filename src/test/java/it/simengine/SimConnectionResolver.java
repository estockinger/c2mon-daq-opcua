package it.simengine;

import cern.c2mon.daq.opcua.downstream.NoSecurityCertifier;
import it.ConnectionResolverBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Runs before all tests across different classes
 */
@Slf4j
public class SimConnectionResolver extends ConnectionResolverBase {

    private static int PILOT_PORT = 8890;
    private static int SIMENGINE_PORT = 4841;


    @Override
    public void beforeAll(ExtensionContext context) throws InterruptedException {
        this.certifier = new NoSecurityCertifier();
        super.beforeAll(context, "simEngineDockerImage");
    }

    public void initialize() {
        // Without testcontainers started with:
        // docker run --net=host --expose=4840-4940 --expose=8800-8900 --env "SIMCONFIG=sim_BASIC.short.xml" gitlab-registry.cern.ch/mludwig/venuscaensimulationengine:venuscombo1.0.3
        image = new GenericContainer("gitlab-registry.cern.ch/mludwig/venuscaensimulationengine:venuscombo1.0.3")
                .waitingFor(Wait.forLogMessage(".*Server opened endpoints for following URLs:.*", 2))
                .withEnv("SIMCONFIG", "sim_BASIC.short.xml")
                .withNetworkMode("host");
        image.start();

        log.info("Servers starting... ");
        extractAddress(PILOT_PORT, "pilot");
        extractAddress(SIMENGINE_PORT, "simEngine");
        log.info("Servers ready");
    }

}
