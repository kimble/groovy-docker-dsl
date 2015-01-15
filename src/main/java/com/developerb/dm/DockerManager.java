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

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Main class
 */
public class DockerManager {

    private final ContainerSources containerSources;
    private final DockerClient dockerClient;
    private final Console console;

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar docker-manager.jar path/to/dsl.groovy");
            System.exit(1);
        }
        else {
            File file = new File(args[0]);
            Console console = new Console(file.getName().replace(".groovy", ""));

            if (!file.exists()) {
                console.err(args[0] + " does not exist");
                System.exit(2);
            }
            else if (!file.isFile()) {
                console.err(args[0] + " is not a file");
                System.exit(3);
            }
            else {
                CharSource scriptSource = Files.asCharSource(file.getAbsoluteFile(), Charsets.UTF_8);
                File workingDirectory = file.getAbsoluteFile().getParentFile();
                DockerManager manager = new DockerManager(console, scriptSource, workingDirectory);
                manager.visualize(console.subConsole("Graphviz"));
                Thread thread = manager.bootAndMonitor();

                try {
                    thread.join();
                }
                catch (InterruptedException e) {
                    console.out("Bye bye!");
                }
            }
        }
    }


    public DockerManager(Console console, CharSource scriptSource, File workingDirectory) {
        this.console = console;
        this.dockerClient = configureDocker();
        this.containerSources = new DSLReader(console, dockerClient)
                .load(scriptSource, workingDirectory);
    }

    public void visualize(Console console) {
        Dotter.visualize(console, containerSources);
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
        final Thread stopping = new Thread(new StopAndRemoveAll(console, containerSources, dockerClient));
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
            while (!Thread.interrupted()) {
                try {
                    Stopwatch stopwatch = Stopwatch.createStarted();

                    buildImages();
                    waitForStopJob();
                    bootContainers();

                    long elapsed = stopwatch.elapsed(SECONDS);
                    if (elapsed > 2) {
                        console.out("Spent %s seconds building images and booting containers", elapsed);
                        console.out("Hanging around waiting for something to do...");
                    }

                    Thread.sleep(10 * 1000);
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    console.out("Stopping monitoring..");
                }
                catch (Exception ex) {
                    Thread.currentThread().interrupt();
                    console.out("Oh my...", ex);
                }
            }

        }

        private void waitForStopJob() {
            try {
                if (stopAlreadyRunningContainers.isAlive()) {
                    console.out("Waiting for stop job to finish");
                    stopAlreadyRunningContainers.join();

                    console.out("Already running containers has been stopped");
                }
            }
            catch (Exception e) {
                console.err("Trouble waiting for containers to stop / removed, " + e.getMessage());
            }
        }

    }

}
