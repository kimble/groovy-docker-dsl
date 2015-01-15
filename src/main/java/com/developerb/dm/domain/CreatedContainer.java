package com.developerb.dm.domain;

import com.developerb.dm.Console;
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

    public CreatedContainer(Console console, DockerClient client, String containerId, String containerName, ContainerLinks links, MappedPorts mappedPorts, CreateContainer createAnotherContainer) {
        super(console, containerName, containerId, client);

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
        console.line("Running and waiting for %s", shortId());
        start();

        console.line("Waiting for container to stop");
        client.waitContainerCmd(id).exec();
        client.removeContainerCmd(id).withForce(true).exec();

        console.line("Removed %s", shortId());
    }

    public BootedContainer boot(BeforeHook before, Healthcheck healthcheck, Set<BootedContainer> dependencies) throws InterruptedException {
        console.line("Executing before hook associated with %s", shortId());
        before.execute(new BeforeHook.Args (
                this, dependencies
        ));

        console.line("Starting container %s", shortId());
        start();

        String bootedFromImage = fetchImageId();

        console.line("Booted from: %s", bootedFromImage);

        BootedContainer bootedContainer = new BootedContainer(console, id, name, bootedFromImage, client);

        if (healthcheck != null) {
            Result result = healthcheck.probeUntilTimeout(bootedContainer);

            if (result.isHealthy()) {
                console.line(result.toString());
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
