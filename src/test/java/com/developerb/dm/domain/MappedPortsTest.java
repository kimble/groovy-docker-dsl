package com.developerb.dm.domain;

import com.developerb.dm.domain.docker.MappedPorts;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MappedPortsTest {

    @Test
    public void udp() throws Exception {
        MappedPorts ports = new MappedPorts();
        ports.add("1:2/udp");

        MappedPorts expected = new MappedPorts();
        expected.addUdp(1, 2);

        assertEquals(ports, expected);
    }

    @Test
    public void tcp() throws Exception {
        MappedPorts ports = new MappedPorts();
        ports.add("1:2");

        MappedPorts expected = new MappedPorts();
        expected.addTcp(1, 2);

        assertEquals(ports, expected);
    }

}