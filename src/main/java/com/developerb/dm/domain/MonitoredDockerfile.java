package com.developerb.dm.domain;

import com.developerb.dm.Console;
import com.developerb.dm.dsl.ContainerApi;
import com.github.dockerjava.api.DockerClient;
import com.google.common.hash.HashCode;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class MonitoredDockerfile extends ContainerSource {

    private final Console console;

    private final Dockerfile dockerfile;

    private final AtomicReference<HashCode> currentHashCode;
    private final AtomicReference<String> imageId;

    private final Set<MonitoredDockerfile> inheritsFrom;

    public MonitoredDockerfile(Console console, Dockerfile dockerfile, Set<MonitoredDockerfile> inheritsFrom, List<ContainerApi> containers) {
        super(containers);

        this.console = console;
        this.inheritsFrom = inheritsFrom;
        this.currentHashCode = new AtomicReference<>();
        this.imageId = new AtomicReference<>();
        this.dockerfile = dockerfile;
    }

    @Override
    public void buildRecursive(DockerClient client) throws Exception {
        if (hasChanged()) {
            console.line("Detected a change");

            // Ensures that any Dockerfile we're inheriting from has been re-built
            for (ContainerSource folder : inheritsFrom) {
                folder.buildRecursive(client);
            }

            build(client);
        }
    }

    @Override
    public boolean isNamed(String name) {
        return dockerfile.isNamed(name);
    }

    private void build(DockerClient client) throws IOException, InterruptedException {
        HashCode updatedChecksum = dockerfile.generateChecksum();
        currentHashCode.set(updatedChecksum);

        String img = dockerfile.build(client);
        imageId.set(img);

        for (ContainerApi container : containers) {
            container.updateImageId(img);
        }
    }

    public boolean hasChanged() throws IOException {
        HashCode currentChecksum = currentHashCode.get();
        HashCode updatedChecksum = dockerfile.generateChecksum();
        
        return currentChecksum == null || !updatedChecksum.equals(currentChecksum);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitoredDockerfile that = (MonitoredDockerfile) o;
        return dockerfile.equals(that.dockerfile);
    }

    @Override
    public int hashCode() {
        return dockerfile.hashCode();
    }

    @Override
    public String toString() {
        return dockerfile.toString();
    }

    public HashCode generateChecksum() throws IOException {
        return dockerfile.generateChecksum();
    }

}
