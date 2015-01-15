package com.developerb.dm.domain;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.base.Preconditions;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Represents a booted container and exposes operations that can be done
 * on a container in this state.
 */
public class BootedContainer extends IdentifiedContainer {

    private final String bootedFromImage;

    public BootedContainer(String containerId, String containerName, String bootedFromImage, DockerClient client) {
        super(containerName, containerId, client);

        this.bootedFromImage = Preconditions.checkNotNull(bootedFromImage, "Booted from image");
    }

    public String ipAddress() {
        return inspect().getNetworkSettings().getIpAddress();
    }

    public boolean isRunning() {
        return inspect().getState().isRunning();
    }

    public String httpGet(String path) {
        return httpClient()
                .path(path)
                .request()
                .buildGet()
                .invoke(String.class);
    }

    public JerseyWebTarget httpClient() {
        JerseyClient client = JerseyClientBuilder.createClient();
        return client.target("http://" + ipAddress());
    }

    public boolean canOpenTcpSocket(int port) throws IOException {
        Socket socket = null;

        try {
            String ip = ipAddress();
            socket = new Socket(ip, port);
            return true;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public boolean canOpenUdpSocket(int port) throws IOException {
        DatagramSocket socket = null;

        try {
            String ip = ipAddress();
            InetAddress address = InetAddress.getByName(ip);
            socket = new DatagramSocket(port, address);

            return true;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public boolean hasStopped() {
        InspectContainerResponse.ContainerState state = inspect().getState();
        return !state.isRunning() && state.getPid() < 1;
    }

    public boolean isBootedFrom(String image) {
        return bootedFromImage.equals(image);
    }

    public void shutdown() {
        log.info("Shutting down");

        client.stopContainerCmd(id)
                .withTimeout(2)
                .exec();
    }

}
