package com.developerb.dm.domain;

import com.developerb.dm.Console;
import com.developerb.dm.dsl.ContainerApi;
import com.developerb.dm.task.PullRemoteImage;
import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class RemoteImage extends ContainerSource {

    private final Console console;

    private final String repository;
    private final String image;
    private final String tag;

    private volatile boolean hasFetchedId = false;

    public RemoteImage(Console console, String repository, String image, String tag, List<ContainerApi> containers) {
        super(containers);

        this.console = console;
        this.repository = repository;
        this.image = image;
        this.tag = tag;
    }

    @Override
    public void buildRecursive(DockerClient client) throws Exception {
        if (!hasFetchedId) {
            String imageId = new PullRemoteImage(console, image, tag, repository).doIt(client);
            console.out("Resolved '%s' to id '%s'", image, imageId);

            for (ContainerApi container : containers) {
                container.updateImageId(imageId);
            }

            hasFetchedId = true;
        }
    }

    @Override
    public boolean isNamed(String imageName) {
        return image.equals(imageName);
    }

}
