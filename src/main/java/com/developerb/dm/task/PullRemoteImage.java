package com.developerb.dm.task;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;


/**
 * Build Docker image from folder
 *  - or -
 * Just return supplied image
 */
public class PullRemoteImage extends AbstractDockerTask<String> {

    private final Logger log;

    private final String tag;
    private final String repository;
    private final String imageName;

    public PullRemoteImage(String imageName, String tag, String repository) {
        this.log = LoggerFactory.getLogger("image." + imageName);

        this.imageName = imageName;
        this.repository = repository;
        this.tag = tag;
    }

    @Override
    public String doIt(DockerClient client) {
        log.info("Pulling image with repository {} and tag {}",
                repository != null ? repository : "'none'",
                tag != null ? tag : "'none'");

        String repoAndImage = Joiner.on("/").skipNulls().join(repository, imageName);
        String response = pullImage(client, repoAndImage);
        String imageId = stupidWayOfExtractingImageId(client, repoAndImage);

        if (imageId == null) {
            throw new IllegalStateException("Failed to get image id, raw response from pulling the image:\n" +response);
        }
        else {
            log.info("Got image id: {}", imageId);
            return imageId;
        }
    }

    /**
     * The response from 'pull image' doesnt include the image id so we create a container based
     * on the name of the image, grab the image id from the created container before deleting it.
     *
     * This should be a lot easier..
     */
    private String stupidWayOfExtractingImageId(DockerClient client, String repoAndImage) {
        log.info("Extracting image id..");

        CreateContainerResponse created = client.createContainerCmd(Joiner.on(":").skipNulls().join(repoAndImage, tag)).exec();
        String imageId = client.inspectContainerCmd(created.getId()).exec().getImageId();
        client.removeContainerCmd(created.getId()).exec();
        return imageId;
    }

    private String pullImage(DockerClient client, String repoAndImage) {
        PullImageCmd command = client.pullImageCmd(repoAndImage);

        if (tag != null) {
            command.withTag(tag);
        }

        InputStream responseStream = command.exec();
        return asString(responseStream);
    }

}
