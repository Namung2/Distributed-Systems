package loadbalance;

import SendToStorage.TCPClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TcpServer {
    private final String loadBalancerHost;
    private final int loadBalancerPort;

    public TcpServer(String loadBalancerHost, int loadBalancerPort) {
        this.loadBalancerHost = loadBalancerHost;
        this.loadBalancerPort = loadBalancerPort;
    }

    public void startConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter 1 to register or 2 to unregister or 3 to call TCP storage or Type 'exit' to quit.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Exiting...");
                break;
            } else if ("1".equals(input)) {
                // Register request
                String jsonRequest = String.format("{\"cmd\":\"register\",\"protocol\":\"tcp\",\"port\":9001}");
                String response = sendHttpRegisterRequest(jsonRequest);
                System.out.println("Response from LoadBalancer: " + response);
            } else if ("2".equals(input)) {
                // Unregister request
                String jsonRequest = String.format("{\"cmd\":\"unregister\",\"protocol\":\"tcp\",\"port\":9001}");
                String response = sendHttpUnregisterRequest(jsonRequest);
                System.out.println("Response from LoadBalancer: " + response);
            } else if ("3".equals(input)) {
                TCPClient client = new TCPClient("localhost", 7002);
                client.sendMessageToStorage("{\"method\": \"GET\", \"path\": \"/notes\"}");
            } else {
                System.out.println("Invalid input. Please enter '1' to register, '2' to unregister");
            }
        }

        scanner.close();
    }
    private String sendHttpRegisterRequest(String jsonRequest) {
        return sendHttpRequest("/loadbalancer/register", jsonRequest);
    }

    private String sendHttpUnregisterRequest(String jsonRequest) {
        return sendHttpRequest("/loadbalancer/unregister", jsonRequest);
    }

    private String sendHttpRequest(String endpoint, String jsonRequest) {
        try (Socket socket = new Socket(loadBalancerHost, loadBalancerPort);
             OutputStream outputStream = socket.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(outputStream, true)) {

            // HTTP 요청 작성
            String httpRequest = String.format(
                    "POST %s HTTP/1.1\r\n" +
                            "Host: %s:%d\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: %d\r\n" +
                            "\r\n%s",
                    endpoint, loadBalancerHost, loadBalancerPort, jsonRequest.length(), jsonRequest
            );

            // 요청 전송
            out.print(httpRequest);
            out.flush();

            // 응답 헤더 파싱
            StringBuilder headers = new StringBuilder();
            String responseLine;
            int contentLength = 0;

            while ((responseLine = in.readLine()) != null) {
                if (responseLine.isEmpty()) {
                    break;
                }
                headers.append(responseLine).append("\n");

                // Content-Length 추출
                if (responseLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(responseLine.split(":")[1].trim());
                }
            }

            // 응답 바디 파싱
            StringBuilder responseBody = new StringBuilder();
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                int bytesRead = in.read(buffer, 0, contentLength);
                if (bytesRead > 0) {
                    responseBody.append(buffer, 0, bytesRead);
                }
            }

            return responseBody.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"ack\":\"failed\",\"msg\":\"Failed to communicate with LoadBalancer\"}";
        }
    }

}
