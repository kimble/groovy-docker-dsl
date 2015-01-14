package com.developerb.dm.task;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


/**
 * Build Docker image from folder
 *  - or -
 * Just return supplied image
 */
public class BuildImageFromFolder extends AbstractDockerTask<String> {

    private final Logger log;

    private final File folder;
    private final String tag;
    private final String repository;
    private final String imageName;

    public BuildImageFromFolder(File folder, String repository, String imageName, String tag) {
        this.log = LoggerFactory.getLogger("image." + imageName);

        this.imageName = imageName;
        this.repository = repository;
        this.folder = folder;
        this.tag = tag;
    }

    @Override
    public String doIt(DockerClient client) {
        log.info("Building image with repository {} and tag {}",
                repository != null ? repository : "'none'",
                tag != null ? tag : "'none'");

        BuildImageCmd command = client.buildImageCmd(folder);

        if (tag != null) {
            String formattedTag = String.format("%s/%s:%s", repository, imageName, tag);
            command.withTag(formattedTag);
        }

        String response = asString(command.exec());
        String imageId = StringUtils.substringBetween(response, "Successfully built ", "\\n\"}");

        if (imageId == null) {
            throw new IllegalStateException("Failed to get image id from response:\n" +response);
        }
        else {
            String fullImageId = client.inspectImageCmd(imageId.trim()).exec().getId();
            log.info("Got image id: {}", fullImageId);

            return fullImageId;
        }
    }

}
