package cern.c2mon.daq.opcua.connection;

import cern.c2mon.daq.opcua.config.AppConfigProperties;
import cern.c2mon.daq.opcua.exceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The RetryDelegate executes a given method a number of times in case of failure until the method completes
 * successfully with a delay as given in the application configuration. It simultaneously keeps track of how long the
 * client has been disconnected from the server. If that period is longer than the total period likely needed to
 * complete all retries, a method is only executed once before it fails. Retry logic is implemented using Spring Retry,
 * which is AOP-based, meaning that Retry annotations methods do not retry if called from within the same class.
 */
@Component(value = "retryDelegate")
@RequiredArgsConstructor
@Slf4j
public class RetryDelegate {

    private final AppConfigProperties config;

    /**
     * Attempt to execute the get() method of a given supplier for a certain number of times with a timeout given as
     * app.serverTimeout in the application configuration. If the action fails due to a reason indicating a
     * configuration issue, a {@link ConfigurationException} is thrown and no further attempts are made. Similarly, if
     * the client has been disconnected from the server for a period of time longer than the estimated time spent
     * executing all retry attempts, no further attempts are made and a {@link LongLostConnectionException} is thrown.
     * In all other cases, the supplier.get() method is attempted for a maximum amount of times given in the application
     * configuration as app.maxRetryAttempts with a delay in between executions of app.retryDelay.
     * @param context           the context of the supplier.
     * @param futureSupplier    a supplier returning the CompletableFuture with the action to be executed.
     * @param disconnectionTime a supplier fetching the time that the calling endpoint has been disconnected
     * @param <T>               The return type of the CompletableFuture returned by the supplier.
     * @return the result of the operation represented as a CompletableFuture returned by futureSupplier
     * @throws OPCUAException of type {@link ConfigurationException} if the supplier fails due to a reason indicating a
     *                        configuration issue, or type {@link LongLostConnectionException} is the time of
     *                        disconnection from the server is greater than the time needed to attempt all retries, and
     *                        of type {@link CommunicationException} is all retries attempts have been exhausted without
     *                        a success.
     */
    @Retryable(value = {CommunicationException.class},
            maxAttemptsExpression = "#{@appConfigProperties.getMaxRetryAttempts()}",
            backoff = @Backoff(delayExpression = "#{@appConfigProperties.getRetryDelay()}",
                    maxDelayExpression = "#{@appConfigProperties.getMaxRetryDelay()}",
                    multiplierExpression = "#{@appConfigProperties.getRetryMultiplier()}"))
    <T> T completeOrRetry(ExceptionContext context, LongSupplier disconnectionTime, Supplier<CompletableFuture<T>> futureSupplier) throws OPCUAException {
        try {
            // The call must be passed as a supplier rather than a future. Otherwise only the join() method is repeated,
            // but not the underlying action on the OpcUaClient
            if (disconnectionTime.getAsLong() < 0L) {
                log.info("Endpoint was stopped, cease retries.");
                return null;
            }
            return futureSupplier.get().get(config.getRequestTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.debug("Execution {} failed with interrupted exception; ", context.name(), e);
            Thread.currentThread().interrupt();
            throw OPCUAException.of(context, e.getCause(), isLongDisconnection(disconnectionTime));
        } catch (ExecutionException | TimeoutException e) {
            log.debug("Execution {} failed with exception; ", context.name(), e);
            throw OPCUAException.of(context, e.getCause(), isLongDisconnection(disconnectionTime));
        } catch (Exception e) {
            log.info("An unexpected exception occurred during {}.", context.name(), e);
            throw OPCUAException.of(context, e, isLongDisconnection(disconnectionTime));
        }
    }

    private boolean isLongDisconnection(LongSupplier disconnectionTime) {
        return config.getMaxRetryAttempts() * (config.getRetryDelay() + config.getRequestTimeout()) < disconnectionTime.getAsLong();
    }
}