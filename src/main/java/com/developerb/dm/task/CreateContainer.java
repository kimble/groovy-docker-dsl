package com.developerb.dm.task;

import com.developerb.dm.domain.CreatedContainer;
import com.developerb.dm.domain.docker.ContainerCommand;
import com.developerb.dm.domain.docker.ContainerLinks;
import com.developerb.dm.domain.docker.EnvVariables;
import com.developerb.dm.domain.docker.MappedPorts;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/**
 * Create a Docker container and return the id.
 */
public class CreateContainer extends AbstractDockerTask<CreatedContainer> {

    private final Logger log;

    private final String containerName;
    private final String image;

    private final StopAndRemoveExistingContainer stopAndRemove;

    private final ContainerCommand command;
    private final EnvVariables environmentVariables;
    private final ContainerLinks links;
    private final MappedPorts mappedPorts;

    public CreateContainer(String containerName, String image, ContainerCommand command, EnvVariables env, MappedPorts mappedPorts, ContainerLinks links) {
        this.log = containerLogger(containerName);

        this.containerName = Preconditions.checkNotNull(containerName, "container-name");
        this.mappedPorts = Preconditions.checkNotNull(mappedPorts, "mapped-ports");
        this.image = Preconditions.checkNotNull(image, "image");
        this.stopAndRemove = new StopAndRemoveExistingContainer(containerName);

        this.command = command;
        this.environmentVariables = env;
        this.links = links;
    }


    public CreateContainer replaceCommand(String... newCommands) {
        ContainerCommand newCommand = new ContainerCommand(newCommands);
        return new CreateContainer(containerName + "_tmp", image, newCommand, environmentVariables, mappedPorts, links);
    }

    @Override
    public CreatedContainer doIt(DockerClient client) {
        log.info("Creating container for '{}', based on image '{}'", containerName, image);

        stopAndRemove.doIt(client);

        CreateContainerCmd query = client.createContainerCmd(image)
                .withEnv(environmentVariables.toEnv())
                .withName(containerName)
                .withCmd(command.cmd());

        CreateContainerResponse response = query.exec();
        String containerId = response.getId();

        log.info("Created container with id {}", StringUtils.abbreviate(containerId, 12));

        return new CreatedContainer(client, containerId, containerName, links, mappedPorts, this);
    }

}
