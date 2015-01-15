package com.developerb.dm.domain;

import com.developerb.dm.Console;
import com.developerb.dm.dsl.ContainerApi;
import com.developerb.dm.dsl.hook.AfterBootHook;
import com.developerb.dm.dsl.hook.AfterRebootHook;
import com.developerb.dm.dsl.hook.BeforeRebootHook;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.joda.time.Duration;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class ContainerSources implements Iterable<ContainerSource> {

    private final Console console;
    
    private final Set<ContainerSource> containerSources;

    private final AfterBootHook afterBootHook;
    private final AfterRebootHook afterRebootHook;
    private final BeforeRebootHook beforeRebootHook;

    public ContainerSources(Console console, Set<ContainerSource> containerSources, AfterBootHook afterBootHook, AfterRebootHook afterRebootHook, BeforeRebootHook beforeRebootHook) {
        this.console = console;
        this.containerSources = containerSources;
        this.afterBootHook = afterBootHook;
        this.afterRebootHook = afterRebootHook;
        this.beforeRebootHook = beforeRebootHook;
    }

    public void buildRecursive(DockerClient client) throws Exception {
        for (ContainerSource containerSource : containerSources) {
            containerSource.buildRecursive(client);
        }
    }

    public ContainerApi byName(String name) {
        for (ContainerSource containerSource : containerSources) {
            for (ContainerApi container : containerSource.containers()) {
                if (container.isNamed(name)) {
                    return container;
                }
            }
        }

        throw new IllegalStateException("Can't find container named " + name);
    }

    @Override
    public Iterator<ContainerSource> iterator() {
        return containerSources.iterator();
    }

    public synchronized void bootContainers() throws InterruptedException, ExecutionException {
        Set<ContainerApi> unBooted = Sets.newHashSet();
        Set<ContainerApi> needsRebooting = Sets.newHashSet();

        for (ContainerSource containerSource : containerSources) {
            for (ContainerApi container : containerSource.containers()) {
                Optional<BootedContainer> potentiallyBooted = container.bootedContainer();

                if (potentiallyBooted.isPresent()) {
                    if (container.shouldBeRebooted()) {
                        needsRebooting.add(container);
                        needsRebooting.addAll(container.mustBeRebooted());
                    }
                }
                else {
                    unBooted.add(container);
                }
            }
        }

        if (!unBooted.isEmpty()) {
            console.out("Un-booted containers: %s", Joiner.on(", ").join(unBooted));

            Stopwatch stopwatch = Stopwatch.createStarted();
            //bootInParallel(unBooted);

            for (ContainerApi toBoot : unBooted) {
                toBoot.ensureBooted();
            }

            afterBootHook.execute(new AfterBootHook.Args (
                    unBooted, Duration.millis(stopwatch.elapsed(TimeUnit.MILLISECONDS))
            ));
        }

        if (!needsRebooting.isEmpty()) {
            console.out("Need of re-boot: %s", Joiner.on(", ").join(needsRebooting));

            beforeRebootHook.execute (
                    new BeforeRebootHook.Args(needsRebooting)
            );

            Stopwatch stopwatch = Stopwatch.createStarted();
            rebootInOrder(needsRebooting);

            afterRebootHook.execute(new AfterRebootHook.Args (
                    needsRebooting, Duration.millis(stopwatch.elapsed(TimeUnit.MILLISECONDS))
            ));
        }
    }

    // Todo: Get this working.. Seems like Docker has some trouble with this
    private void bootInParallel(Set<ContainerApi> unBooted) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(6);

        try {
            CompletionService<ContainerApi> completionService = new ExecutorCompletionService<>(executor);
            Set<ContainerApi> queue = Sets.newHashSet(unBooted);

            while (!queue.isEmpty()) {
                final Iterator<ContainerApi> iterator = queue.iterator();
                final AtomicInteger batchSize = new AtomicInteger(0);

                while (iterator.hasNext()) {
                    final ContainerApi container = iterator.next();

                    if (container.allDependenciesBooted()) {
                        batchSize.incrementAndGet();
                        completionService.submit(new Callable<ContainerApi>() {

                            @Override
                            public ContainerApi call() throws Exception {
                                container.ensureBooted();
                                return container;
                            }

                        });
                    }
                }

                int bc = batchSize.get();
                if (bc == 0 && !queue.isEmpty()) {
                    throw new IllegalStateException("Can't figure out how to start " + queue + ", batch size: " + bc);
                }

                console.out("Waiting for batch of " + bc);
                for (int i = 0; i < bc; i++) {
                    ContainerApi booted = completionService.take().get();
                    queue.remove(booted);
                }
            }
        }
        finally {
            executor.shutdownNow();
        }
    }

    private void rebootInOrder(Set<ContainerApi> needsRebooting) {
        Set<ContainerApi> queue = Sets.newHashSet(needsRebooting);
        Set<ContainerApi> rebooted = Sets.newHashSet();

        while (!queue.isEmpty()) {
            Set<ContainerApi> batch = Sets.newHashSet();

            for (ContainerApi queued : queue) {
                if (addToBatch(queue, queued)) {
                    batch.add(queued);
                }
            }

            if (batch.isEmpty() && !queue.isEmpty()) {
                throw new IllegalStateException("Unable to re-boot: " + queue);
            }

            for (ContainerApi queued : batch) {
                queued.reboot();
                queue.remove(queued);
                rebooted.add(queued);
            }
        }
    }

    private boolean addToBatch(Set<ContainerApi> queue, ContainerApi container) {
        Set<ContainerApi> dependenciesOfQueued = container.findAllDependencies();

        for (ContainerApi dependency : dependenciesOfQueued) {
            if (queue.contains(dependency)) {
                return false;
            }
        }

        return true;
    }

    public void shutdownAndRemove() {
        for (ContainerSource containerSource : containerSources) {
            for (ContainerApi container : containerSource.containers()) {
                Optional<BootedContainer> booted = container.bootedContainer();

                if (booted.isPresent()) {
                    BootedContainer bootedContainer = booted.get();
                    bootedContainer.shutdown();
                    bootedContainer.remove();
                }
            }

        }
    }

}
