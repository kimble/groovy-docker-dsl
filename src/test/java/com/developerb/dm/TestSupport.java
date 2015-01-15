package com.developerb.dm;


import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class TestSupport {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


    protected File copySimpleHelloWorld() throws IOException {
        return copyTest("simple-hello-world", new String[] {
                "php-hello-world/src/index.php",
                "php-hello-world/Dockerfile",
                "test-sample.groovy"
        });
    }


    protected File copyLinked() throws IOException {
        return copyTest("linked-hello-world", new String[] {
                "calculator/src/index.php",
                "calculator/Dockerfile",

                "client/src/index.php",
                "client/Dockerfile",

                "test-sample.groovy"
        });
    }

    protected File copyFigtest() throws IOException {
        return copyTest("figtest", new String[] {
                "web/app.py",
                "web/Dockerfile",
                "web/requirements.txt",

                "figtest.groovy"
        });
    }

    protected File copyTest(String scenarioName, String[] resources) throws IOException {
        File folder = tmp.newFolder(scenarioName);

        for (String resource : resources) {
            Path basePath = folder.toPath();
            Path resourcePath = Paths.get(resource);

            Path targetPath = basePath.resolve(resourcePath);
            File targetFile = targetPath.toFile();
            targetFile.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                URL url = Resources.getResource(scenarioName + "/" + resource);
                Resources.copy(url, fos);
            }
        }

        return folder;
    }

    protected DockerManager loadManager(File simpleHelloWorldFolder, String dslFilename) {
        File dslFile = new File(simpleHelloWorldFolder, dslFilename);
        CharSource source = Files.asCharSource(dslFile, Charsets.UTF_8);
        return new DockerManager(source, simpleHelloWorldFolder);
    }
}
