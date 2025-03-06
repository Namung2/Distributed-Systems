package distributed.loadbalancer.service;

import distributed.loadbalancer.repository.Servers.Server;
import distributed.loadbalancer.repository.Servers.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LoadbalancingService {
    private final ServerRepository serverRepository = ServerRepository.getInstance();
    private final AtomicInteger counter = new AtomicInteger(0); // 라운드로빈 인덱스

    public String handleSendingMessage(String message) {
        Set<Server> servers = serverRepository.checkAllServers();

        if (servers.isEmpty()) {
            return "No servers available";
        }

        List<Server> serverList = new ArrayList<>(servers); // Set을 List로 변환
        Server server = selectServer(serverList); // 라운드 로빈으로 서버 선택

        boolean success = sendMessageToServer(server, message); // 선택한 서버에 메시지 전송
        return success ? "Successfully sent a message to the server" : "Failed to send message to server";
    }

    // 라운드 로빈 방식으로 서버 선택
    private Server selectServer(List<Server> serverList) {
        int index = counter.getAndUpdate(i -> (i + 1) % serverList.size());
        return serverList.get(index);
    }

    // 서버에 메시지 전송
    private boolean sendMessageToServer(Server server, String message) {
        try {
            switch (server.getProtocol()) {
                case "api":
                    return sendApiMessage(server.getIp(), server.getPort(), message);
                case "tcp":
                    return sendTcpMessage(server.getIp(), server.getPort(), message);
                case "udp":
                    return sendUdpMessage(server.getIp(), server.getPort(), message);
                default:
                    log.warn("Unsupported protocol: {}", server.getProtocol());
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to send message to {}:{} via {}: {}",
                    server.getIp(), server.getPort(), server.getProtocol(), e.getMessage());
            return false;
        }
    }

    private boolean sendApiMessage(String hostIp, int port, String message) {
        String url = "http://" + hostIp + ":" + port + "/api/message";
        log.info("Sending API message to {}", url);

        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(url, message, String.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to send API message to {}:{}", hostIp, port, e);
            return false;
        }
    }

    private boolean sendTcpMessage(String hostIp, int port, String message) {
        try (Socket socket = new Socket(hostIp, port);
             OutputStream outputStream = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(outputStream, true)) {

            out.println(message);
            out.flush();
            log.info("TCP message sent to {}:{}", hostIp, port);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean sendUdpMessage(String hostIp, int port, String message) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(hostIp);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

            socket.send(packet);
            log.info("UDP message sent to {}:{}", hostIp, port);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
