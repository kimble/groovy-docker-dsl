package com.developerb.dm.domain.docker;

import com.google.common.base.Joiner;

import java.util.Arrays;

/**
 * Represents a command to be passed to a container.
 */
public class ContainerCommand {

    private final String[] command;

    public ContainerCommand(String... command) {
        this.command = command;
    }

    public String[] cmd() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerCommand that = (ContainerCommand) o;
        return Arrays.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(command);
    }

    @Override
    public String toString() {
        return Joiner.on(" ").join(command);
    }

}
