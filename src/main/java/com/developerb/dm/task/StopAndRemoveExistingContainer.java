package com.developerb.dm.task;

import com.developerb.dm.Console;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Stop and remove a named container.
 */
public class StopAndRemoveExistingContainer extends AbstractDockerTask<Void> {

    private final String containerName;

    public StopAndRemoveExistingContainer(Console console, String containerName) {
        super(console);
        this.containerName = containerName;
    }

    @Override
    public Void doIt(DockerClient client) {
        for (Container container : listAllContainers(client)) {
            for (String name : container.getNames()) {
                if (name.equals("/" + containerName)) {
                    console.out("Found existing container name %s (%s), stopping and removing", name, StringUtils.abbreviate(container.getId(), 10));

                    if (isRunning(client, container.getId())) {
                        stopWithTimeout(client, container.getId(), 2);
                    }

                    removeWithForce(client, container.getId());
                }
            }
        }

        return null;
    }


    private List<Container> listAllContainers(DockerClient client) {
        return client.listContainersCmd().withShowAll(true).exec();
    }

    private boolean isRunning(DockerClient client, String containerId) {
        return client.inspectContainerCmd(containerId).exec().getState().isRunning();
    }

    private void stopWithTimeout(DockerClient client, String containerId, int timeout) {
        client.stopContainerCmd(containerId)
                .withTimeout(timeout)
                .exec();
    }

    private void removeWithForce(DockerClient client, String containerId) {
        client.removeContainerCmd(containerId)
                .withForce(true)
                .exec();
    }

}
