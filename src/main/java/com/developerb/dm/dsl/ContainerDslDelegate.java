package com.developerb.dm.dsl;

import com.developerb.dm.dsl.hook.BeforeHook;
import com.developerb.dm.healtcheck.Healthcheck;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.lang.Closure;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static groovy.lang.Closure.DELEGATE_FIRST;

/**
 * Delegate for 'container' closures in the dsl.
 */
@SuppressWarnings("UnusedDeclaration")
class ContainerDslDelegate extends AbstractDelegate {

    private final List<ContainerApi> containers = Lists.newArrayList();

    private final DockerClient dockerClient;
    private final DockerfilesDelegate.ContainerRepo containerRepo;

    ContainerDslDelegate(DockerClient dockerClient, DockerfilesDelegate.ContainerRepo containerRepo) {
        this.dockerClient = dockerClient;
        this.containerRepo = containerRepo;
    }

    public void methodMissing(String containerName, Object args) {
        Object[] arguments = (Object[]) args;

        if (arguments.length == 1 && arguments[0] instanceof Closure) {
            defineContainer(containerName, (Closure) arguments[0]);
        }
    }

    private void defineContainer(String containerName, Closure containerDefinition) {
        Logger log = LoggerFactory.getLogger("container." + containerName);

        try {
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

        public void healthcheck(Closure implementation) {
            Map<String, Object> parameters = Maps.newHashMap();
            healthcheck(parameters, implementation);
        }

        public void healthcheck(Map<String, Object> parameters, Closure implementation) {
            Duration timeout = timeout(parameters);
            Duration interval = interval(parameters);

            if (!parameters.isEmpty()) {
                throw new IllegalStateException("Don't know what to do with health check parameters: " + parameters.keySet());
            }

            Healthcheck healthcheck = new Healthcheck(log, implementation, timeout, interval);
            containerApi.healthcheck(healthcheck);
        }

        private Duration timeout(Map<String, Object> parameters) {
            Integer value = (Integer) parameters.remove("timeout");

            if (value != null) {
                return Duration.standardSeconds(value);
            }
            else {
                return Duration.standardSeconds(60);
            }
        }

        private Duration interval(Map<String, Object> parameters) {
            Integer value = (Integer) parameters.remove("interval");

            if (value != null) {
                return Duration.standardSeconds(value);
            }
            else {
                return Duration.standardSeconds(5);
            }
        }

        public void before(Closure closure) {
            BeforeHook before = new BeforeHook(closure);
            containerApi.before(before);
        }

        ContainerApi build() {
            return containerApi;
        }

    }

}
