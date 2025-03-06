package distributed.loadbalancer.healthcheck.service;

import distributed.loadbalancer.repository.Servers.Server;
import distributed.loadbalancer.repository.Servers.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class HealthCheckService {

    private final ServerRepository serverRepository = ServerRepository.getInstance();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedRate = 3000)
    public void performHealthCheck() {
        Set<Server> servers = serverRepository.checkAllServers();

        if (servers.isEmpty()) {
            log.info("No servers registered. Skipping health check.");
            return;  // 등록된 서버가 없으면 이번 주기는 건너뜁니다.
        }

        log.info("Performing health check on {} servers...", servers.size());

        for (Server server : servers) {
            executorService.submit(() -> {
                boolean isAlive = sendHealthCheck(server);
                if (!isAlive) {
                    log.warn("Server {}:{} (protocol: {}) did not respond with ACK. Removing it.",
                            server.getIp(), server.getPort(), server.getProtocol());
                    serverRepository.removeServer(server);
                }
            });
        }
    }

    private boolean sendHealthCheck(Server server) {
        switch (server.getProtocol()) {
            case "api":
                return sendApiHealthCheck(server);
            case "tcp":
                return sendTcpHealthCheck(server);
            case "udp":
                return sendUdpHealthCheck(server);
            default:
                log.warn("Unknown protocol: {}", server.getProtocol());
                return false;
        }
    }

    private boolean sendApiHealthCheck(Server server) {
        try {
            String urlString;
            if (server.getIp().contains(":")) { // IPv6 주소
                urlString = String.format("http://[%s]:%d/api/health", server.getIp(), server.getPort());
            } else { // IPv4 주소
                urlString = String.format("http://%s:%d/api/health", server.getIp(), server.getPort());
            }
            log.info("Sending health check to: {}", urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            String jsonRequest = objectMapper.writeValueAsString(Map.of("cmd", "hello"));
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonRequest.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            log.info("Received response code {} from server: {}:{}", responseCode, server.getIp(), server.getPort());

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String response = br.readLine();
                    log.info("Response from server {}:{} -> {}", server.getIp(), server.getPort(), response);
                    return response.contains("\"ack\":\"hello\"");
                }
            }
        } catch (Exception e) {
            log.error("Failed to send health check to {}:{}", server.getIp(), server.getPort(), e);
        }
        return false;
    }

    private boolean sendTcpHealthCheck(Server server) {
        try (Socket socket = new Socket(server.getIp(), server.getPort());
             OutputStream outputStream = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(outputStream, true)) {

            String healthCheckMessage = "{\"cmd\":\"hello\"}";
            log.info("Sending health check msg to {}:{} - {}", server.getIp(), server.getPort(), healthCheckMessage);
            out.println(healthCheckMessage);
            out.flush();

            String response = in.readLine();
            log.info("Received response from {}:{} - {}", server.getIp(), server.getPort(), response);

            return response != null && response.equals("{\"ack\":\"hello\"}");
        } catch (Exception e) {
            log.info("Health check failed for {}:{}", server.getIp(), server.getPort(), e);
        }
        return false;
    }

    private boolean sendUdpHealthCheck(Server server) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String healthCheckMessage = "{\"cmd\":\"hello\"}";
            log.info("Sending health check msg to {}:{} - {}", server.getIp(), server.getPort(), healthCheckMessage);

            byte[] buffer = healthCheckMessage.getBytes();
            InetAddress address = InetAddress.getByName(server.getIp());
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, server.getPort());

            socket.send(packet);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.setSoTimeout(3000);  // 3초 타임아웃 설정
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            log.info("Received response from {}:{} - {}", server.getIp(), server.getPort(), response);

            return response.equals("{\"ack\":\"hello\"}");
        } catch (Exception e) {
            log.info("Health check failed for {}:{}", server.getIp(), server.getPort(), e);
        }
        return false;
    }
}
