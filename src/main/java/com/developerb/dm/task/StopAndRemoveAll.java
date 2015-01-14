package com.developerb.dm.task;

import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.dsl.ContainerApi;
import com.github.dockerjava.api.DockerClient;

/**
 * Stop and remove all existing containers defined in dsl.
 */
public class StopAndRemoveAll implements Runnable {

    private final ContainerSources containerSources;
    private final DockerClient client;

    public StopAndRemoveAll(ContainerSources containerSources, DockerClient client) {
        this.containerSources = containerSources;
        this.client = client;
    }

    @Override
    public void run() {
        for (ContainerSource containerSource : containerSources) {
            for (ContainerApi container : containerSource.containers()) {
                StopAndRemoveExistingContainer stopAndRemove = new StopAndRemoveExistingContainer(container.name());
                stopAndRemove.doIt(client);
            }
        }
    }

}
