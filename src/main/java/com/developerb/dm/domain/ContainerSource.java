package com.developerb.dm.domain;

import com.developerb.dm.dsl.ContainerApi;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Optional;

import java.util.List;

/**
 * Todo: I'm not very happy with this name..
 */
public abstract class ContainerSource {

    protected final List<ContainerApi> containers;

    protected ContainerSource(List<ContainerApi> containers) {
        this.containers = containers;
    }

    public List<ContainerApi> containers() {
        return containers;
    }

    public abstract void buildRecursive(DockerClient client) throws Exception;

    public abstract boolean isNamed(String imageName);


    public final Optional<ContainerApi> containerByName(String name) {
        for (ContainerApi container : containers) {
            if (container.isNamed(name)) {
                return Optional.of(container);
            }
        }

        return Optional.absent();
    }

}
