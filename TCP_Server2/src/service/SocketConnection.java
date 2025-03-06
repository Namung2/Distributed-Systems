package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class SocketConnection {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int port; // HealthCheck 포트

    public SocketConnection(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        // 단일 줄 메시지 처리 (HealthCheck 메시지는 단일 줄)
                        String request = in.readLine();
                        if (request == null || request.trim().isEmpty()) {
                            System.out.println("Empty request received.");
                            continue;  // 빈 요청 무시
                        }

                        if (isValidJson(request)) {
                            Map<String, String> map = objectMapper.readValue(request, Map.class);

                            if (map.containsKey("cmd")) {
                                System.out.println("Received HealthCheck request: " + request);
                                String response = handleRequest(request);
                                out.println(response);
                                System.out.println("Sent HealthCheck response: " + response);
                            } else {
                                System.out.println("Received message: " + request);
                            }
                        } else {
                            System.out.println("Invalid JSON format: " + request);
                            out.println("{\"ack\":\"failed\",\"message\":\"Invalid JSON format\"}");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String handleRequest(String request) {
        try {
            JsonNode jsonRequest = objectMapper.readTree(request);
            String cmd = jsonRequest.get("cmd").asText();

            if ("hello".equals(cmd)) {
                JsonNode jsonResponse = objectMapper.createObjectNode().put("ack", "hello");
                return objectMapper.writeValueAsString(jsonResponse);
            } else {
                JsonNode jsonResponse = objectMapper.createObjectNode()
                        .put("ack", "failed")
                        .put("message", "Unknown command");
                return objectMapper.writeValueAsString(jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonNode jsonResponse = objectMapper.createObjectNode()
                    .put("ack", "failed")
                    .put("message", "Invalid JSON format");
            try {
                return objectMapper.writeValueAsString(jsonResponse);
            } catch (Exception ex) {
                ex.printStackTrace();
                return "{\"ack\":\"failed\",\"msg\":\"Internal server error\"}";
            }
        }
    }
}
