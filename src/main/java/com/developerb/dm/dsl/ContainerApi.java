package com.developerb.dm.dsl;

import com.developerb.dm.domain.BootedContainer;
import com.developerb.dm.domain.CreatedContainer;
import com.developerb.dm.domain.docker.ContainerCommand;
import com.developerb.dm.domain.docker.ContainerLinks;
import com.developerb.dm.domain.docker.EnvVariables;
import com.developerb.dm.domain.docker.MappedPorts;
import com.developerb.dm.dsl.hook.BeforeHook;
import com.developerb.dm.healtcheck.Healthcheck;
import com.developerb.dm.task.CreateContainer;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Container API.
 */
public class ContainerApi {

    private final Logger log;

    private final DockerClient dockerClient;
    private final String containerName;

    private Healthcheck healthcheck;
    private BeforeHook before = new BeforeHook();

    private final Set<ContainerApi> linkedContainers = Sets.newHashSet();
    private final Set<ContainerApi> incomingLinks = Sets.newHashSet();

    private ContainerCommand defaultCommand = new ContainerCommand();

    private final ContainerLinks links = new ContainerLinks();
    private final EnvVariables environmentVariables = new EnvVariables();
    private final MappedPorts mappedPorts = new MappedPorts();

    private final AtomicReference<BootedContainer> bootedContainer;
    private final AtomicReference<String> imageId;

    public ContainerApi(Logger log, DockerClient dockerClient, String containerName) {
        this.bootedContainer = new AtomicReference<>();
        this.imageId = new AtomicReference<>();

        this.dockerClient = dockerClient;
        this.containerName = containerName;
        this.log = log;
    }

    public ContainerLinks links() {
        return links;
    }

    public String name() {
        return containerName;
    }

    public void addIncomingLink(ContainerApi container) {
        log.debug("Registered {} -> {}", container.name(), name());
        incomingLinks.add(container);
    }

    public Optional<BootedContainer> bootedContainer() {
        return Optional.fromNullable(bootedContainer.get());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerApi that = (ContainerApi) o;
        return containerName.equals(that.containerName);
    }

    @Override
    public int hashCode() {
        return containerName.hashCode();
    }

    @Override
    public String toString() {
        return containerName;
    }

    public boolean isNamed(String name) {
        return containerName.equals(name);
    }

    public void defaultCommand(String[] command) {
        if (command == null) {
            throw new IllegalArgumentException("Command can't be null");
        }

        defaultCommand = new ContainerCommand(command);
    }

    public void healthcheck(Healthcheck healthcheck) {
        this.healthcheck = healthcheck;
    }

    public void before(BeforeHook before) {
        this.before = before;
    }

    public void env(String env) {
        environmentVariables.add(env);
    }

    public void updateImageId(String imageId) {
        this.imageId.set(imageId);
    }

    public void mappedPort(String formatted) {
        mappedPorts.add(formatted);
    }

    public void linkTo(String formatted, DockerfilesDelegate.ContainerRepo containerRepo) {
        ContainerLinks.ContainerLink added = links.add(formatted);
        ContainerApi linkedContainer = containerRepo.findByName(added.containerName());

        linkedContainer.addIncomingLink(this);
        linkedContainers.add(linkedContainer);
    }

    public synchronized BootedContainer ensureBooted() {
        BootedContainer alreadyBooted = bootedContainer.get();

        if (alreadyBooted != null) {
            log.debug("Already running");
            return alreadyBooted;
        }
        else {
            Set<BootedContainer> dependencies = Sets.newHashSet();

            // Make sure that any containers we depend on has been booted
            for (ContainerApi linkedContainer : linkedContainers) {
                BootedContainer dependency = linkedContainer.ensureBooted();
                dependencies.add(dependency);
            }

            String img = imageId.get();
            if (img != null) {
                try {
                    CreateContainer createContainer = new CreateContainer(containerName, img, defaultCommand, environmentVariables, mappedPorts, links);
                    CreatedContainer createdContainer = createContainer.doIt(dockerClient);
                    BootedContainer booted = createdContainer.boot(before, healthcheck, dependencies);

                    bootedContainer.set(booted);
                    return booted;
                }
                catch (Exception ex) {
                    throw new IllegalStateException("Failed to create / boot container " + containerName, ex);
                }
            }
            else {
                throw new IllegalStateException("Image has not been set for " + containerName);
            }
        }
    }

    private synchronized void shutdown() {
        BootedContainer booted = bootedContainer.get();

        if (booted != null) {
            booted.shutdown();
            bootedContainer.set(null);
        }
    }


    public Set<ContainerApi> mustBeRebooted() {
        Set<ContainerApi> needsRebooting = Sets.newHashSet();

        if (shouldBeRebooted()) {
            needsRebooting.add(this);
            needsRebooting.addAll(findAllDependentContainers(this));

            log.info("{} must be re-booted, which means that these will have to do the same: {}", this, Joiner.on(", ").join(findAllDependentContainers(this)));
        }

        return needsRebooting;
    }

    public boolean shouldBeRebooted() {
        Optional<BootedContainer> potentiallyBooted = bootedContainer();

        if (potentiallyBooted.isPresent()) {
            BootedContainer booted = potentiallyBooted.get();
            String updatedImage = imageId.get();

            return !booted.isBootedFrom(updatedImage);
        }
        else {
            return false; // No need to re-boot
        }
    }

    public Set<ContainerApi> findAllDependencies() {
        return findAllDependencies(this);
    }

    private static Set<ContainerApi> findAllDependentContainers(ContainerApi container) {
        Set<ContainerApi> dependent = Sets.newHashSet();

        for (ContainerApi incomingLink : container.incomingLinks) {
            dependent.add(incomingLink);
            dependent.addAll(findAllDependentContainers(incomingLink));
        }

        return dependent;
    }

    private static Set<ContainerApi> findAllDependencies(ContainerApi container) {
        Set<ContainerApi> dependent = Sets.newHashSet();

        for (ContainerApi linked : container.linkedContainers) {
            dependent.add(linked);
            dependent.addAll(findAllDependencies(linked));
        }

        return dependent;
    }

    /**
     * Un-changed containers doesn't really have to be shut down and re-created,
     * but it turns out to be faster then restarting it(!?)
     */
    public void reboot() {
        log.info("Rebooting");

        shutdown();
        ensureBooted();
    }

    public boolean allDependenciesBooted() {
        for (ContainerApi dependency : findAllDependencies()) {
            if (!dependency.bootedContainer().isPresent()) {
                return false;
            }
        }

        return true;
    }

}
