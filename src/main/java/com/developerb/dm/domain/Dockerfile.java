package com.developerb.dm.domain;

import com.developerb.dm.task.BuildImageFromFolder;
import com.github.dockerjava.api.DockerClient;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a folder with a Dockerfile inside.
 */
public class Dockerfile {

    private final File folder;
    private final String imageName;
    private final BuildImageFromFolder buildImageFromFolder;
    private final Set<MonitoredDockerfile> inheritsFrom;


    public Dockerfile(File folder, String imageName, BuildImageFromFolder buildImageFromFolder, Set<MonitoredDockerfile> inheritsFrom) {
        this.folder = folder;
        this.imageName = imageName;
        this.inheritsFrom = inheritsFrom;
        this.buildImageFromFolder = buildImageFromFolder;
    }

    public String build(DockerClient client) throws IOException {
        return buildImageFromFolder.doIt(client);
    }

    public boolean isNamed(String name) {
        return imageName.equals(name);
    }

    public HashCode generateChecksum() throws IOException {
        Iterator<File> iterator = Files.fileTreeTraverser()
                .breadthFirstTraversal(folder)
                .filter(Files.isFile())
                .iterator();

        HashFunction sha1 = Hashing.sha1();
        Hasher hasher = sha1.newHasher();

        // Add files
        while (iterator.hasNext()) {
            File file = iterator.next();
            HashCode fileHash = Files.hash(file, sha1);
            hasher.putBytes(fileHash.asBytes());
        }

        // Add dependencies
        for (MonitoredDockerfile dependency : inheritsFrom) {
            HashCode dependencyChecksum = dependency.generateChecksum();
            hasher.putBytes(dependencyChecksum.asBytes());
        }

        return hasher.hash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dockerfile that = (Dockerfile) o;
        return !(imageName != null ? !imageName.equals(that.imageName) : that.imageName != null);
    }

    @Override
    public int hashCode() {
        return imageName != null ? imageName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return imageName;
    }

    public String name() {
        return imageName;
    }

}
