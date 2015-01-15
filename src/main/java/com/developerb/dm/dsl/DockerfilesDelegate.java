package com.developerb.dm.dsl;

import com.developerb.dm.Console;
import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.Dockerfile;
import com.developerb.dm.domain.MonitoredDockerfile;
import com.developerb.dm.domain.RemoteImage;
import com.developerb.dm.task.BuildImageFromFolder;
import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static groovy.lang.Closure.DELEGATE_FIRST;

@SuppressWarnings("UnusedDeclaration")
class DockerfilesDelegate extends AbstractDelegate {

    private final Set<ContainerSource> containerSourceObjects = Sets.newHashSet();

    private final DockerClient dockerClient;
    private final File workingDirectory;
    private final Console console;

    DockerfilesDelegate(Console console, DockerClient client, File workingDirectory) {
        if (workingDirectory == null) {
            throw new IllegalArgumentException("Working directory is mandatory");
        }
        if (console == null) {
            throw new IllegalArgumentException("Console is mandatory");
        }
        if (client == null) {
            throw new IllegalArgumentException("Docker client is mandatory");
        }

        this.console = console;
        this.dockerClient = client;
        this.workingDirectory = workingDirectory;
    }


    public void methodMissing(String rawImageName, Object args) {
        String imageName = LOWER_CAMEL.to(LOWER_HYPHEN, rawImageName);

        if (((Object[])args)[0] instanceof Closure) {
            Closure containerDefinition = (Closure) ((Object[])args)[0];
            DockerfileDelegate delegate = new DockerfileDelegate(imageName, workingDirectory);
            containerDefinition.setResolveStrategy(DELEGATE_FIRST);
            containerDefinition.setDelegate(delegate);
            containerDefinition.run();

            ContainerSource containerSource = delegate.build();

            if (containerSource != null) {
                containerSourceObjects.add(containerSource);
            }
        }
        else {
            throw new IllegalStateException("Non-closure passed to " + rawImageName);
        }
    }

    public Set<ContainerSource> tasks() {
        return containerSourceObjects;
    }



    public class DockerfileDelegate {

        private final Logger log;
        private final String imageName;
        private final File workingDirectory;

        private String tag;
        private String repository;
        private File folder;

        private String image;

        private Set<MonitoredDockerfile> inheritsFrom = Sets.newHashSet();
        private List<ContainerApi> containers = Lists.newArrayList();

        public DockerfileDelegate(String imageName, File workingDirectory) {
            if (workingDirectory == null) {
                throw new IllegalStateException("Working directory can't be null");
            }
            if (!workingDirectory.isDirectory()) {
                throw new IllegalStateException(workingDirectory + " is not a directory");
            }

            this.workingDirectory = workingDirectory;
            this.log = LoggerFactory.getLogger("image." + imageName);
            this.imageName = imageName;
        }

        public void folder(String path) {
            Path basePath = workingDirectory.toPath();
            Path filePath = Paths.get(path);
            folder = basePath.resolve(filePath).toFile();

            if (!folder.isDirectory()) {
                String error = String.format("%s is not a directory (%s)", folder, imageName);
                throw new IllegalStateException(error);
            }
        }

        public void repository(String repository) {
            this.repository = repository;
        }

        public void tag(String tag) {
            this.tag = tag;
        }

        public void image(String image) {
            this.image = image;
        }

        public void inherits(String rawImageName) {
            MonitoredDockerfile folder = findByName(rawImageName);
            inheritsFrom.add(folder);
        }



        private MonitoredDockerfile findByName(String rawImageName) {
            String imageName = LOWER_CAMEL.to(LOWER_HYPHEN, rawImageName);
            for (ContainerSource folder : containerSourceObjects) {
                if (folder instanceof MonitoredDockerfile) {
                    if (folder.isNamed(imageName) || folder.isNamed(rawImageName)) {
                        return (MonitoredDockerfile) folder;
                    }
                }
            }

            throw new IllegalStateException("Unable to find folder named " + rawImageName);
        }

        public void containers(Closure definition) {
            ContainerDslDelegate delegate = new ContainerDslDelegate(console, dockerClient, new ContainerRepo());
            definition.setResolveStrategy(DELEGATE_FIRST);
            definition.setDelegate(delegate);
            definition.run();

            containers = delegate.containers();

            for (ContainerApi container : containers) {
                Console containerConsole = console.subConsole(container.name());
                containerConsole.line("Defined container");
            }
        }

        public ContainerSource build() {
            if (image != null) {
                return new RemoteImage(console.subConsole(imageName), repository, image, tag, containers);
            }
            else {
                BuildImageFromFolder buildImageFromFolder = new BuildImageFromFolder(console.subConsole(imageName), folder, repository, imageName, tag);
                Dockerfile dockerfile = new Dockerfile(folder, imageName, buildImageFromFolder, inheritsFrom);

                return new MonitoredDockerfile(console, dockerfile, inheritsFrom, containers);
            }
        }

    }


    // Todo: This is not nice..
    class ContainerRepo {

        public ContainerApi findByName(String name) {
            for (ContainerSource folder : containerSourceObjects) {
                Optional<ContainerApi> container = folder.containerByName(name);

                if (container.isPresent()) {
                    return container.get();
                }
            }

            throw new IllegalStateException("No container named " + name);
        }

    }

}
