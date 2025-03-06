package Controller;

import Connection.Connection2Primary;
import Repository.LocalRepository;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCP_storage {
    private final int port = 7002;
    private final Connection2Primary connection2Primary;
    private final BackupController backupController;
    private final ExecutorService executor;

    public TCP_storage(Connection2Primary connection2Primary) {
        this.connection2Primary = connection2Primary;
        LocalRepository localRepository = LocalRepository.getInstance();
        this.backupController = new BackupController(localRepository); // 초기화
        this.executor = Executors.newFixedThreadPool(10); // 동시 요청 처리 스레드풀
    }

    public void startStorage() {
        connection2Primary.initConnectionAndFetchNotes(); // 초기 데이터 가져오기

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Storage listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> processClientRequest(clientSocket)); // 비동기 요청 처리
            }
        } catch (IOException e) {
            System.err.println("[ERROR] TCP Storage 서버 소켓 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processClientRequest(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String firstLine = reader.readLine();

            if (firstLine == null || firstLine.isEmpty()) return;

            if (isHttpRequest(firstLine)) {
                handleHttpRequest(firstLine, reader, outputStream);
            } else {
                handleTcpMessage(firstLine, reader, outputStream);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 클라이언트 요청 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[ERROR] 소켓 종료 중 오류 발생: " + e.getMessage());
            }
        }
    }
    private boolean isHttpRequest(String firstLine) {
        return firstLine.startsWith("GET") || firstLine.startsWith("POST") || firstLine.startsWith("PUT") ||
                firstLine.startsWith("PATCH") || firstLine.startsWith("DELETE");
    }

    private void handleHttpRequest(String requestLine, BufferedReader reader, OutputStream outputStream) {
        try {
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendHttpResponse(outputStream, "400 Bad Request", "text/plain", "잘못된 요청입니다.");
                System.err.println("[ERROR] 잘못된 HTTP 요청: " + requestLine);
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            int contentLength = 0;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
            }

            // 요청 본문 읽기
            String body = readRequestBody(reader, contentLength);

            // 메서드와 경로에 따라 처리
            if ("GET".equalsIgnoreCase(method) && "/connect/status".equalsIgnoreCase(path)) {
                sendHttpResponse(outputStream, "200 OK", "text/plain", "READY");
            } else if (path.startsWith("/backup")) {
                // 모든 /backup 관련 메서드 처리
                backupController.handleRequest(method, path, body);
                sendHttpResponse(outputStream, "200 OK", "application/json", "{\"message\": \"요청 처리 완료\"}");
            } else {
                // 지원되지 않는 경로 처리
                sendHttpResponse(outputStream, "404 Not Found", "text/plain", "경로를 찾을 수 없습니다: " + path);
                System.err.println("[ERROR] 지원되지 않는 경로: " + path);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] HTTP 요청 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void sendHttpResponse(OutputStream outputStream, String status, String contentType, String body) throws IOException {
        // HTTP 응답 작성
        String response = String.format(
                "HTTP/1.1 %s\r\n" +
                        "Content-Type: %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Connection: close\r\n" + // 연결 종료 명시
                        "\r\n%s",
                status, contentType, body.getBytes("UTF-8").length, body
        );

        // 응답 전송
        outputStream.write(response.getBytes("UTF-8"));
        outputStream.flush();
    }

    private String readRequestBody(BufferedReader reader, int contentLength) throws IOException {
        if (contentLength <= 0) return "";
        char[] buffer = new char[contentLength];
        int read = reader.read(buffer, 0, contentLength);
        return (read > 0) ? new String(buffer, 0, read) : "";
    }

    private void handleTcpMessage(String firstLine, BufferedReader reader, OutputStream outputStream) {
        try {
            StringBuilder messageBuilder = new StringBuilder(firstLine);
            while (reader.ready()) {
                messageBuilder.append(reader.readLine());
            }
            String message = messageBuilder.toString();
            connection2Primary.processMessageFromClient(message);

            outputStream.write("END\r\n".getBytes());
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("[ERROR] TCP 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
