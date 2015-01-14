package com.developerb.dm.dot;

import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.domain.docker.ContainerLinks;
import com.developerb.dm.dsl.ContainerApi;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Uses Graphviz to draw a graph of all containers and their dependencies.
 */
public class Dotter {

    private final static Logger log = LoggerFactory.getLogger(Dotter.class);


    public static void visualize(ContainerSources containerSource) {
        StringBuilder dot = new StringBuilder("digraph containers {\n");
        dot.append("rankdir=LR;\n"); // Left to right
        // dot.append("splines=line;\n"); // Only straight lines

        for (ContainerSource m : containerSource) {
            for (ContainerApi c : m.containers()) {
                for (ContainerLinks.ContainerLink link : c.links()) {
                    dot.append("  ").append(c.name()).append(" -> ").append(link.containerName());

                    if (link.hasDifferentAlias()) {
                        dot.append(" [label=").append(link.alias()).append("]");
                    }

                    dot.append(";\n");
                }
            }
        }

        dot.append("\n}");
        String dotted = dot.toString();

        try {
            log.info("Generating graph");

            File destination = new File("containers.dot");
            Files.write(dotted, destination, Charsets.UTF_8);
            Process process = Runtime.getRuntime().exec("dot containers.dot -Tpng -o containers.png");
            int exit = process.waitFor();

            if (exit != 0) {
                log.warn("Failed to create graph, exit: " + exit);
            }
        }
        catch (Exception ex) {
            log.warn("Unable to write graph", ex);
        }
    }

}
