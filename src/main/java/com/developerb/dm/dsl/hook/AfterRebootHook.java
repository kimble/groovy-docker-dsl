package com.developerb.dm.dsl.hook;

import com.developerb.dm.dsl.ContainerApi;
import groovy.lang.Closure;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Hook that will be called after a reboot has taken place.
 */
public class AfterRebootHook extends AbstractHook<AfterRebootHook.Args> {

    public AfterRebootHook() {
        super(null);
    }

    public AfterRebootHook(Closure implementation) {
        super(implementation);
    }

    @Override
    protected void execute(Closure<?> implementation, Args args) {
        Delegate delegate = new Delegate();
        implementation.setDelegate(delegate);
        implementation.setResolveStrategy(Closure.DELEGATE_FIRST);
        implementation.call(args);
    }

    static class Delegate {

        final static Logger log = LoggerFactory.getLogger("after-reboot");

    }

    public static class Args {

        final Set<ContainerApi> containers;
        final Duration duration;

        public Args(Set<ContainerApi> containers, Duration duration) {
            this.containers = containers;
            this.duration = duration;
        }

    }

}
