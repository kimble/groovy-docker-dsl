package com.developerb.dm.domain.docker;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * For defining port mappings
 */
public class MappedPorts {

    private final Set<MappedPort> ports = Sets.newHashSet();


    public MappedPorts add(String formatted) {
        String[] mainParts = formatted.split("/");
        String[] portParts = mainParts[0].split(":");

        boolean udp = mainParts.length == 2 && mainParts[1].equalsIgnoreCase("udp");

        MappedPort mappedPort = new MappedPort(
                Integer.parseInt(portParts[0]),
                Integer.parseInt(portParts[1]),
                udp
        );

        ports.add(mappedPort);
        return this;
    }

    public MappedPorts addUdp(int exposed, int to) {
        MappedPort mappedPort = new MappedPort(exposed, to, true);
        ports.add(mappedPort);

        return this;
    }

    public MappedPorts addTcp(int exposed, int to) {
        MappedPort mappedPort = new MappedPort(exposed, to, false);
        ports.add(mappedPort);

        return this;
    }

    public Ports toPorts() {
        Ports ports = new Ports();

        for (MappedPort mappedPort : this.ports) {
            ExposedPort exposed = mappedPort.exposed();
            Ports.Binding binding = mappedPort.boundTo();

            ports.bind(exposed, binding);
        }

        return ports;
    }

    public static class MappedPort {

        private final int from;
        private final int to;
        private final boolean udp;

        public MappedPort(int from, int to, boolean udp) {
            this.from = from;
            this.to = to;
            this.udp = udp;
        }

        @Override
        public String toString() {
            return from + ":" + to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MappedPort that = (MappedPort) o;
            return from == that.from && to == that.to && udp == that.udp;
        }

        @Override
        public int hashCode() {
            int result = from;
            result = 31 * result + to;
            result = 31 * result + (udp ? 1 : 0);
            return result;
        }

        public ExposedPort exposed() {
            return udp ? ExposedPort.udp(from)
                    : ExposedPort.tcp(from);
        }

        public Ports.Binding boundTo() {
            return Ports.Binding(to);
        }
    }

    @Override
    public String toString() {
        return Joiner.on(", ").join(ports);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MappedPorts that = (MappedPorts) o;
        return !(ports != null ? !ports.equals(that.ports) : that.ports != null);
    }

    @Override
    public int hashCode() {
        return ports != null ? ports.hashCode() : 0;
    }

}
