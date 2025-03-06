package Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Socketconnection {

    private final int port; // UDP 통신 포트
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper 사용

    public Socketconnection(int port) {
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            try (DatagramSocket serverSocket = new DatagramSocket(port)) {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    // 데이터 수신
                    serverSocket.receive(packet);
                    String request = new String(packet.getData(), 0, packet.getLength());

                    try {
                        JsonNode jsonRequest = objectMapper.readTree(request);
                        if (jsonRequest.has("cmd")) {
                            // 헬스 체크 요청 처리
                            System.out.println("Received HealthCheck request: " + request);

                            String response = handleRequest(jsonRequest);

                            // 응답 전송
                            byte[] responseBytes = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseBytes, responseBytes.length, packet.getAddress(), packet.getPort()
                            );
                            serverSocket.send(responsePacket);
                            System.out.println("Sent HealthCheck response: " + response);
                        } else {
                            // 로드 밸런싱된 메시지 처리
                            System.out.println("Received message: " + request);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse JSON request: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String handleRequest(JsonNode jsonRequest) {
        String cmd = jsonRequest.get("cmd").asText();

        // "cmd" 값이 "hello"이면, "ack" 값도 "hello"로 응답
        JsonNode jsonResponse;
        if ("hello".equals(cmd)) {
            jsonResponse = objectMapper.createObjectNode().put("ack", "hello");
        } else {
            // 알 수 없는 명령어에 대한 응답
            jsonResponse = objectMapper.createObjectNode()
                    .put("ack", "failed")
                    .put("msg", "Unknown command");
        }
        try {
            return objectMapper.writeValueAsString(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"ack\":\"failed\",\"msg\":\"Internal server error\"}";
        }
    }
}
