package com.developerb.dm.healtcheck;

import com.developerb.dm.domain.BootedContainer;
import groovy.lang.Closure;
import org.joda.time.Duration;
import org.slf4j.Logger;

/**
 * DSL authors can define health checks for containers.
 * The health check will be repeatedly executed after a container
 * has been started. Only when the health check is green or the
 * process has timed out will the boot process continue.
 */
public class Healthcheck {

    private final Logger log;
    private final Closure implementation;

    public Healthcheck(Logger log, Closure implementation) {
        if (implementation == null) {
            throw new IllegalArgumentException("No health check implementation provided");
        }

        this.log = log;
        this.implementation = implementation;
        this.implementation.setDelegate(new Delegate(log));
        this.implementation.setResolveStrategy(Closure.DELEGATE_FIRST);
    }

    public Result probeUntil(BootedContainer container, Duration remaining, Duration grace) throws InterruptedException {
        String lastErrorMessage = "No error message";

        while (true) {
            log.info("Running healthcheck, {} second(s) until giving up ({})", remaining.getStandardSeconds(), lastErrorMessage);

            if (container.hasStopped()) {
                return Result.unhealthy("Container has stopped - No point in running health check");
            }

            Result result = probe(container);
            if (result.isHealthy()) {
                return result;
            }

            lastErrorMessage = result.message();
            remaining = remaining.minus(grace);

            if (remaining.isLongerThan(grace)) {
                Thread.sleep(grace.getMillis());
            }
            else {
                return result;
            }
        }
    }

    public Result probe(BootedContainer container) {
        try {
            Object message = implementation.call(container);

            if (message == null) {
                return Result.unhealthy("Health check returned null");
            }
            else if (message instanceof String) {
                return Result.healthy((String) message);
            }
            else if (message instanceof Boolean) {
                if ((Boolean) message) {
                    return Result.healthy();
                }
                else {
                    return Result.unhealthy();
                }
            }
            else {
                return Result.unhealthy("Didn't expect health check to return: " + message);
            }
        }
        catch (Throwable trouble) {
            return Result.unhealthy(trouble);
        }
    }


}
