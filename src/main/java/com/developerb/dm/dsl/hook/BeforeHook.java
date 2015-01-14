package com.developerb.dm.dsl.hook;

import com.developerb.dm.domain.BootedContainer;
import com.developerb.dm.domain.CreatedContainer;
import com.google.common.collect.Maps;
import groovy.lang.Closure;

import java.util.Map;
import java.util.Set;

/**
 * Something that should be done _before_ the container is started.
 */
public class BeforeHook extends AbstractHook<BeforeHook.Args> {

    public BeforeHook() {
        this(null);
    }

    public BeforeHook(Closure<?> implementation) {
        super(implementation);
    }

    @Override
    protected void execute(Closure<?> implementation, Args args) {
        Map<String, BootedContainer> containerMap = Maps.newHashMap();
        for (BootedContainer dependency : args.dependencies) {
            containerMap.put(dependency.name(), dependency);
        }

        implementation.call(args.createdContainer, containerMap);
    }


    public static class Args {

        final CreatedContainer createdContainer;
        final Set<BootedContainer> dependencies;

        public Args(CreatedContainer createdContainer, Set<BootedContainer> dependencies) {
            if (createdContainer == null) {
                throw new IllegalArgumentException("Created container can't be null");
            }

            this.createdContainer = createdContainer;
            this.dependencies = dependencies;
        }

    }
}
