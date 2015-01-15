package com.developerb.dm.healtcheck;

import com.developerb.dm.Console;
import com.developerb.dm.domain.BootedContainer;
import groovy.lang.Closure;
import org.joda.time.Duration;

/**
 * DSL authors can define health checks for containers.
 * The health check will be repeatedly executed after a container
 * has been started. Only when the health check is green or the
 * process has timed out will the boot process continue.
 */
public class Healthcheck {

    private final Console log;
    private final Closure implementation;
    private final Duration timeout, interval;

    public Healthcheck(Console console, Closure implementation, Duration timeout, Duration interval) {
        if (implementation == null) {
            throw new IllegalArgumentException("No health check implementation provided");
        }
        if (timeout == null) {
            throw new IllegalArgumentException("No timeout supplied");
        }
        if (interval == null) {
            throw new IllegalArgumentException("No interval supplied");
        }

        this.log = console;
        this.timeout = timeout;
        this.interval = interval;
        this.implementation = implementation;
    }

    public Result probeUntilTimeout(BootedContainer container) throws InterruptedException {
        String lastErrorMessage = "No error message";
        Duration remaining = timeout;

        while (true) {
            log.out("Running healthcheck, %s second(s) until giving up (%s)", remaining.getStandardSeconds(), lastErrorMessage);

            if (container.hasStopped()) {
                return Result.unhealthy("Container has stopped - No point in running health check");
            }

            Result result = probe(container);
            if (result.isHealthy()) {
                return result;
            }

            lastErrorMessage = result.message();
            remaining = remaining.minus(interval);

            if (remaining.isLongerThan(interval)) {
                Thread.sleep(interval.getMillis());
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
