package com.developerb.dm;

import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.dot.Dotter;
import com.developerb.dm.dsl.DSLReader;
import com.developerb.dm.task.StopAndRemoveAll;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Main class
 */
public class DockerManager {

    private final static Logger log = LoggerFactory.getLogger(DockerManager.class);

    private final ContainerSources containerSources;
    private final DockerClient dockerClient;

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar docker-manager.jar path/to/dsl.groovy");
            System.exit(1);
        }
        else {
            File file = new File(args[0]);
            if (!file.exists()) {
                System.err.println(args[0] + " does not exist");
                System.exit(2);
            }
            else {
                CharSource scriptSource = Files.asCharSource(file, Charsets.UTF_8);
                DockerManager manager = new DockerManager(scriptSource, file.getParentFile());
                manager.visualize();
                Thread thread = manager.bootAndMonitor();

                try {
                    thread.join();
                }
                catch (InterruptedException e) {
                    log.info("Bye bye!");
                }
            }
        }
    }


    public DockerManager(CharSource scriptSource, File workingDirectory) {
        dockerClient = configureDocker();
        containerSources = new DSLReader(dockerClient).load(scriptSource, workingDirectory);
    }

    public void visualize() {
        Dotter.visualize(containerSources);
    }


    public Thread bootAndMonitor() {
        final Thread stopAlreadyRunningContainers = startStopJob(dockerClient, containerSources);

        BuilderMonitoring job = new BuilderMonitoring(stopAlreadyRunningContainers);
        Thread thread = new Thread(job);
        thread.setName("worker");
        thread.start();

        return thread;
    }

    private Thread startStopJob(DockerClient dockerClient, ContainerSources containerSources) {
        final Thread stopping = new Thread(new StopAndRemoveAll(containerSources, dockerClient));
        stopping.setName("stopper");
        stopping.start();

        return stopping;
    }

    private DockerClient configureDocker() {
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri("http://localhost:2375")
                .withLoggingFilter(false)
                .withVersion("1.15")
                .build();

        return DockerClientBuilder.getInstance(config).build();
    }

    public ContainerSources buildImages() {
        try {
            containerSources.buildRecursive(dockerClient);
            return containerSources;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to build image", ex);
        }
    }

    public void bootContainers() {
        try {
            containerSources.bootContainers();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to boot container", ex);
        }
    }

    private class BuilderMonitoring implements Runnable {

        private final Thread stopAlreadyRunningContainers;

        public BuilderMonitoring(Thread stopAlreadyRunningContainers) {
            this.stopAlreadyRunningContainers = stopAlreadyRunningContainers;
        }

        @Override
        public void run() {
            log.debug("Looking for changes..");

            while (!Thread.interrupted()) {
                try {
                    Stopwatch stopwatch = Stopwatch.createStarted();

                    buildImages();
                    waitForStopJob();
                    bootContainers();

                    long elapsed = stopwatch.elapsed(SECONDS);
                    if (elapsed > 2) {
                        log.info("Spent {} seconds building images and booting containers", elapsed);
                        log.info("Hanging around waiting for something to do...");
                    }

                    Thread.sleep(10 * 1000);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.info("Stopping monitoring..");
                }
                catch (Exception ex) {
                    Thread.currentThread().interrupt();
                    log.info("Oh my...", ex);
                }
            }

        }

        private void waitForStopJob() {
            try {
                if (stopAlreadyRunningContainers.isAlive()) {
                    log.info("Waiting for stop job to finish");
                    stopAlreadyRunningContainers.join();

                    log.info("Already running containers has been stopped");
                }
            }
            catch (Exception e) {
                log.error("Trouble waiting for containers to stop / removed");
            }
        }

    }

}
