package com.developerb.dm.dsl;

import com.developerb.dm.dsl.hook.BeforeHook;
import com.developerb.dm.healtcheck.Healthcheck;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static groovy.lang.Closure.DELEGATE_FIRST;

/**
 * Delegate for 'container' closures in the dsl.
 */
class ContainerDslDelegate extends AbstractDelegate {

    private final List<ContainerApi> containers = Lists.newArrayList();

    private final DockerClient dockerClient;
    private final DockerfilesDelegate.ContainerRepo containerRepo;

    ContainerDslDelegate(DockerClient dockerClient, DockerfilesDelegate.ContainerRepo containerRepo) {
        this.dockerClient = dockerClient;
        this.containerRepo = containerRepo;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void methodMissing(String containerName, Object args) {
        Logger log = LoggerFactory.getLogger("container." + containerName);

        try {
            Closure containerDefinition = (Closure) ((Object[])args)[0];
            ContainerDefinitionDelegate delegate = new ContainerDefinitionDelegate(log, containerName);
            containerDefinition.setResolveStrategy(DELEGATE_FIRST);
            containerDefinition.setDelegate(delegate);
            containerDefinition.run();

            ContainerApi builtContainer = delegate.build();
            containers.add(builtContainer);
        }
        catch (Exception ex) {
            log.error("Invalid... {}", containerName, ex); // Todo (throwing an exception here is a bad idea)
        }
    }

    public List<ContainerApi> containers() {
        return containers;
    }


    class ContainerDefinitionDelegate {

        private final ContainerApi containerApi;
        private final Logger log;


        public ContainerDefinitionDelegate(Logger logger, String containerName) {
            Preconditions.checkNotNull(containerName, "container name");
            Preconditions.checkNotNull(logger, "log");

            log = logger;
            containerApi = new ContainerApi(logger, dockerClient, containerName);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void command(String... command) {
            containerApi.defaultCommand(command);
        }

        public void env(String key, String value) {
            containerApi.env(key + "=" + value);
        }

        public void mapPort(String formatted) {
            containerApi.mappedPort(formatted);
        }

        public void linkedTo(String formatted) {
            containerApi.linkTo(formatted, containerRepo);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void healthcheck(Closure closure) {
            Healthcheck healthcheck = new Healthcheck(log, closure);
            containerApi.healthcheck(healthcheck);
        }

        @SuppressWarnings("UnusedDeclaration")
        public void before(Closure closure) {
            BeforeHook before = new BeforeHook(closure);
            containerApi.before(before);
        }

        ContainerApi build() {
            return containerApi;
        }

    }

}
