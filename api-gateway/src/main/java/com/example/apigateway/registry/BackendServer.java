package com.example.apigateway.registry;

public class BackendServer {

    private String host;
    private int port;
    // health check
    private boolean alive = true;

    public BackendServer() {
    }

    public BackendServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.alive = true;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}