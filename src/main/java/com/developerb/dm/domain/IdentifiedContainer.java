package com.developerb.dm.domain;

import com.developerb.dm.Console;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 */
abstract class IdentifiedContainer {

    protected final Console console;

    protected final String name;
    protected final String id;
    protected final DockerClient client;

    protected IdentifiedContainer(Console console, String name, String id, DockerClient client) {
        this.console = Preconditions.checkNotNull(console, "Console");
        this.client = Preconditions.checkNotNull(client, "Docker client");
        this.name = Preconditions.checkNotNull(name, "Container name");
        this.id = Preconditions.checkNotNull(id, "Container id");
    }

    public final InspectContainerResponse inspect() {
        return client.inspectContainerCmd(id).exec();
    }

    @Override
    public String toString() {
        return name + " [id=" + shortId() + "]";
    }

    protected String shortId() {
        return StringUtils.abbreviate(id, 12);
    }

    public String name() {
        return name;
    }

    protected String asString(InputStream response)  {
        StringWriter writer = new StringWriter();

        try {
            LineIterator itr = IOUtils.lineIterator(response, StandardCharsets.UTF_8);

            while (itr.hasNext()) {
                String line = itr.next();
                writer.write(line + (itr.hasNext() ? "\n" : ""));
            }

            return writer.toString();
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to read Docker response stream", e);
        }
        finally {
            IOUtils.closeQuietly(response);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentifiedContainer that = (IdentifiedContainer) o;
        return !(id != null ? !id.equals(that.id) : that.id != null);
    }

    @Override
    public final int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    protected String fetchLogs() {
        InputStream logStream = client.logContainerCmd(id)
                .withStdErr()
                .withStdOut()
                .withTail(100)
                .exec();

        return asString(logStream);
    }

    protected String fetchImageId() {
        return inspect().getImageId();
    }

    public void remove() {
        console.out("Removing container");
        client.removeContainerCmd(id).exec();
    }

}
