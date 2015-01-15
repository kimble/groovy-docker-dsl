package com.developerb.dm.task;

import com.developerb.dm.Console;
import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.dsl.ContainerApi;
import com.github.dockerjava.api.DockerClient;

/**
 * Stop and remove all existing containers defined in dsl.
 */
public class StopAndRemoveAll implements Runnable {

    private final Console console;
    private final ContainerSources containerSources;
    private final DockerClient client;

    public StopAndRemoveAll(Console console, ContainerSources containerSources, DockerClient client) {
        this.containerSources = containerSources;
        this.console = console;
        this.client = client;
    }

    @Override
    public void run() {
        for (ContainerSource containerSource : containerSources) {
            for (ContainerApi container : containerSource.containers()) {
                Console jobConsole = console.subConsole(container.name());
                StopAndRemoveExistingContainer stopAndRemove = new StopAndRemoveExistingContainer(jobConsole, container.name());
                stopAndRemove.doIt(client);
            }
        }
    }

}
