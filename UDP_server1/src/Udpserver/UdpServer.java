package Udpserver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class UdpServer {
    private final String loadBalancerHost;
    private final int loadBalancerPort;

    public UdpServer(String loadBalancerHost, int loadBalancerPort) {
        this.loadBalancerHost = loadBalancerHost;
        this.loadBalancerPort = loadBalancerPort;
    }

    public void startConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter 1 to register or 2 to unregister the server. Type 'exit' to quit.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Exiting...");
                break;
            } else if ("1".equals(input)) {
                // Register request
                String jsonRequest = String.format("{\"cmd\":\"register\",\"protocol\":\"udp\",\"port\":5001}");
                String response = sendHttpRequest("/loadbalancer/register", jsonRequest);
                System.out.println("Response from LoadBalancer: " + response);
            } else if ("2".equals(input)) {
                // Unregister request
                String jsonRequest = String.format("{\"cmd\":\"unregister\",\"protocol\":\"udp\",\"port\":5001}");
                String response = sendHttpRequest("/loadbalancer/unregister", jsonRequest);
                System.out.println("Response from LoadBalancer: " + response);
            } else {
                System.out.println("Invalid input. Please enter '1' to register, '2' to unregister.");
            }
        }

        scanner.close();
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
            String responseLine;
            StringBuilder headers = new StringBuilder();
            int contentLength = 0;

            while ((responseLine = in.readLine()) != null) {
                if (responseLine.isEmpty()) {
                    // 빈 줄을 만나면 헤더 끝
                    break;
                }
                headers.append(responseLine).append("\n");

                // Content-Length 추출
                if (responseLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(responseLine.split(":")[1].trim());
                }
            }

            // 응답 바디 읽기
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
