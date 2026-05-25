package com.example.apigateway.registry;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ServerRegistry {

    //    private final List<BackendServer> servers = new CopyOnWriteArrayList<>();
    // 위와같이 list에 다 넣게 되면 한번 꺼진 서버는 다시 살아나도 참여할수 없음
    // 아래와 같이 분리해서 실행중인 서버와 후보군인 서버로 분리해서 운영하도록 함
    private final List<BackendServer> knownServers =
            new CopyOnWriteArrayList<>();

    private final List<BackendServer> activeServers =
            new CopyOnWriteArrayList<>();

//    public ServerRegistry() {
//
//        servers.add(new BackendServer("localhost", 8081));
//        servers.add(new BackendServer("localhost", 8082));
//    }
//
//    public List<BackendServer> getServers() {
//        return servers;
//    }

    public ServerRegistry() {

        addServer(new BackendServer("localhost", 8081));
        addServer(new BackendServer("localhost", 8082));
    }

    public List<BackendServer> getServers() {
        return activeServers;
    }

//    public void addServer(BackendServer server) {
//        servers.add(server);
//    }

    public void addServer(BackendServer server) {

        knownServers.add(server);

        activeServers.add(server);
    }

    //    public void removeServer(
//            String host,
//            int port
//    ) {
//
//        servers.removeIf(server ->
//                server.getHost().equals(host)
//                        && server.getPort() == port
//        );
//    }
    public void deactivateServer(
            String host,
            int port
    ) {

        activeServers.removeIf(server ->
                server.getHost().equals(host)
                        && server.getPort() == port
        );
    }

    public void activateServer(
            BackendServer target
    ) {

        boolean exists = activeServers.stream()
                .anyMatch(server ->
                        server.getHost().equals(target.getHost())
                                && server.getPort() == target.getPort()
                );

        if (!exists) {
            activeServers.add(target);
        }
    }

    public List<BackendServer> getKnownServers() {
        return knownServers;
    }
}