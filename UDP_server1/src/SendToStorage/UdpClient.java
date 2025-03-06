package SendToStorage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {

    private final String serverAddress;
    private final int port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UdpClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public void sendMessageToStorage(String method, String path, String title, String bodyContent) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(serverAddress);

            // 요청 메시지 구성
            String methodAndPath = String.format("{\"method\": \"%s\", \"path\": \"%s\"}", method, path);
            String body = (title != null && bodyContent != null)
                    ? String.format("{\"title\": \"%s\", \"body\": \"%s\"}", title, bodyContent)
                    : "";

            // 두 부분을 |로 구분
            String request;
            if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                request = methodAndPath;
            } else {
                request = methodAndPath + "|" + body;
            }

            byte[] requestData = request.getBytes();

            // 요청 패킷 전송
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, address, port);
            socket.send(requestPacket);
            System.out.println("서버로 요청 보냄: " + request);

            // 응답 수신
            socket.setSoTimeout(5000); // 5초 타임아웃 설정
            byte[] buffer = new byte[4096];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(responsePacket);
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                System.out.println("서버 응답 (원본): " + response);

                // JSON 응답 포매팅
                if (response.trim().startsWith("{") && response.trim().endsWith("}")) {
                    Object jsonResponse = objectMapper.readValue(response, Object.class);
                    String formattedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse);
                    System.out.println("서버 응답 (포매팅): \n" + formattedResponse);
                } else {
                    System.out.println("응답이 JSON 형식이 아닙니다: " + response);
                }
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("응답 시간 초과: 서버가 응답하지 않습니다.");
            }
        } catch (Exception e) {
            System.err.println("UDP 요청 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}