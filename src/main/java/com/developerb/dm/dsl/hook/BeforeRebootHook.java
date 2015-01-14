package com.developerb.dm.dsl.hook;

import com.developerb.dm.dsl.ContainerApi;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Will be invoked before a reboot is issued.
 */
public class BeforeRebootHook extends AbstractHook<BeforeRebootHook.Args> {

    public BeforeRebootHook() {
        super(null);
    }

    public BeforeRebootHook(Closure implementation) {
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

        final static Logger log = LoggerFactory.getLogger("before-reboot");

    }

    public static class Args {

        final Set<ContainerApi> containers;

        public Args(Set<ContainerApi> containers) {
            this.containers = containers;
        }

    }

}
