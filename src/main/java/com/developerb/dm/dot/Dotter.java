package com.developerb.dm.dot;

import com.developerb.dm.Console;
import com.developerb.dm.domain.ContainerSource;
import com.developerb.dm.domain.ContainerSources;
import com.developerb.dm.domain.docker.ContainerLinks;
import com.developerb.dm.dsl.ContainerApi;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;

/**
 * Uses Graphviz to draw a graph of all containers and their dependencies.
 */
public class Dotter {


    public static void visualize(Console console, ContainerSources containerSource) {
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
            console.line("Generating graph");

            File destination = new File("containers.dot");
            Files.write(dotted, destination, Charsets.UTF_8);
            Process process = Runtime.getRuntime().exec("dot containers.dot -Tpng -o containers.png");
            int exit = process.waitFor();

            if (exit != 0) {
                console.err("Failed to create graph, exit: " + exit);
            }
        }
        catch (Exception ex) {
            console.err("Unable to write graph: " + ex.getMessage());
        }
    }

}
