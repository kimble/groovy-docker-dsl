package com.developerb.dm.domain;

import com.developerb.dm.domain.docker.ContainerLinks;
import com.developerb.dm.domain.docker.MappedPorts;
import com.developerb.dm.dsl.hook.BeforeHook;
import com.developerb.dm.healtcheck.Healthcheck;
import com.developerb.dm.healtcheck.Result;
import com.developerb.dm.task.CreateContainer;
import com.github.dockerjava.api.DockerClient;

import java.util.Set;

/**
 * Represents a container that has been created, but not yet booted.
 */
public class CreatedContainer extends IdentifiedContainer {

    private final ContainerLinks containerLinks;

    private final CreateContainer createAnotherContainer;
    private final MappedPorts mappedPorts;

    public CreatedContainer(DockerClient client, String containerId, String containerName, ContainerLinks links, MappedPorts mappedPorts, CreateContainer createAnotherContainer) {
        super(containerName, containerId, client);

        this.createAnotherContainer = createAnotherContainer;
        this.mappedPorts = mappedPorts;
        this.containerLinks = links;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void runAndWait(String... newCommand) {
        CreateContainer createAnother = createAnotherContainer.replaceCommand(newCommand);
        CreatedContainer anotherContainer = createAnother.doIt(client);

        anotherContainer.runAndWait();
    }

    private void runAndWait() {
        log.info("Running and waiting for {}", shortId());
        start();

        log.info("Waiting for container to stop");
        client.waitContainerCmd(id).exec();
        client.removeContainerCmd(id).withForce(true).exec();

        log.info("Removed {}", shortId());
    }

    public BootedContainer boot(BeforeHook before, Healthcheck healthcheck, Set<BootedContainer> dependencies) throws InterruptedException {
        log.info("Executing before hook associated with {}", shortId());
        before.execute(new BeforeHook.Args (
                this, dependencies
        ));

        log.info("Starting container {}", shortId());
        start();

        String bootedFromImage = fetchImageId();

        log.info("Booted from: {}", bootedFromImage);

        BootedContainer bootedContainer = new BootedContainer(id, name, bootedFromImage, client);

        if (healthcheck != null) {
            Result result = healthcheck.probeUntilTimeout(bootedContainer);

            if (result.isHealthy()) {
                log.info(result.toString());
                return bootedContainer;
            }
            else {
                throw new IllegalStateException("Unable to verify health: " + result);
            }
        }
        else {
            return bootedContainer;
        }
    }

    private void start() {
        client.startContainerCmd(id)
                .withLinks(containerLinks.toLinks())
                .withPortBindings(mappedPorts.toPorts())
                .exec();
    }

}
