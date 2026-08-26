import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.stream.Collectors;

/**
 * 테스트용 경량 HTTP 서버
 * 사용법: java TestBackend.java [포트]
 * 예)   java TestBackend.java 8081
 */
public class TestBackend {

    static int PORT;

    public static void main(String[] args) throws Exception {
        PORT = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();
            String path   = exchange.getRequestURI().toString();

            String reqBody = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            String headers = exchange.getRequestHeaders().entrySet().stream()
                    .map(e -> "      \"" + e.getKey() + "\": \"" + String.join(",", e.getValue()) + "\"")
                    .collect(Collectors.joining(",\n"));

            // 헬스체크 전용 응답
            String json;
            if (path.startsWith("/actuator/health")) {
                json = """
                        {"status":"UP","port":%d}
                        """.formatted(PORT).strip();
            } else {
                json = """
                        {
                          "server_port": %d,
                          "time": "%s",
                          "method": "%s",
                          "path": "%s",
                          "headers": {
                        %s
                          },
                          "body": %s
                        }
                        """.formatted(PORT, LocalTime.now(), method, path, headers,
                                reqBody.isBlank() ? "null" : "\"" + reqBody.replace("\"", "\\\"") + "\"");
            }

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();

            System.out.printf("[:%d] %s %s%n", PORT, method, path);
        });
        server.start();
        System.out.printf("✅ TestBackend started on port %d  (Ctrl+C to stop)%n", PORT);
    }
}
