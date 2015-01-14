package com.developerb.dm;

import com.developerb.dm.domain.BootedContainer;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.dsl.ContainerApi;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class DockerManagerTest extends TestSupport {


    @Test
    public void bootSimpleHelloWorldContainer() throws Exception {
        File simpleHelloWorldFolder = copySimpleHelloWorld();

        DockerManager manager = loadManager(simpleHelloWorldFolder);
        ContainerSources containerSource = manager.buildImages();

        try {
            containerSource.bootContainers();
            ContainerApi container = containerSource.byName("phpHelloWorld");
            BootedContainer bootedContainer = container.bootedContainer().get();
            JerseyWebTarget root = bootedContainer.httpClient();

            String response = root.request()
                    .buildGet()
                    .invoke(String.class);

            assertEquals("Hello world", response);
        }
        finally {
            containerSource.shutdownAndRemove();
        }
    }

    @Test
    public void linkedContainers() throws Exception {
        File linkedFolder = copyLinked();

        DockerManager manager = loadManager(linkedFolder);
        ContainerSources containerSource = manager.buildImages();

        try {
            containerSource.bootContainers();
            ContainerApi container = containerSource.byName("client");
            BootedContainer bootedContainer = container.bootedContainer().get();
            JerseyWebTarget root = bootedContainer.httpClient();

            String response = root.request()
                    .buildGet()
                    .invoke(String.class);

            assertEquals("2 + 3 = 5", response);
        }
        finally {
            containerSource.shutdownAndRemove();
        }
    }

    @Test
    public void linkedContainersRecursiveReboot() throws Exception {
        File linkedFolder = copyLinked();

        DockerManager manager = loadManager(linkedFolder);
        ContainerSources containerSource = manager.buildImages();

        try {
            // Boot containers
            containerSource.bootContainers();
            ContainerApi container = containerSource.byName("client");


            // Assert working
            {
                BootedContainer bootedContainer = container.bootedContainer().get();
                JerseyWebTarget root = bootedContainer.httpClient();
                String response = root.request()
                        .buildGet()
                        .invoke(String.class);

                assertEquals("2 + 3 = 5", response);
            }

            // Apply a change
            {
                File calculator = new File(linkedFolder, "calculator/src/index.php");
                String script = Files.toString(calculator, Charsets.UTF_8);
                String updatedScript = script + "\necho '!';\n";
                Files.write(updatedScript, calculator, Charsets.UTF_8);
            }

            // Detect changes
            {
                manager.buildImages();
                manager.bootContainers();
            }

            // Still working and updated
            {
                BootedContainer bootedContainer = container.bootedContainer().get();
                JerseyWebTarget root = bootedContainer.httpClient();
                String response = root.request()
                        .buildGet()
                        .invoke(String.class);

                assertEquals("2 + 3 = 5!", response);
            }

        }
        finally {
            containerSource.shutdownAndRemove();
        }
    }

}