package com.developerb.dm.task;

import com.developerb.dm.Console;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


abstract class AbstractDockerTask<T> {

    protected final Console console;

    protected AbstractDockerTask(Console console) {
        this.console = console;
    }

    protected Logger containerLogger(String containerName) {
        return LoggerFactory.getLogger("container." + containerName);
    }

    protected abstract T doIt(DockerClient client) throws Exception;

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

}
