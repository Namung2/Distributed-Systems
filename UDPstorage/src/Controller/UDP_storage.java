package Controller;

import Connection.Connection2Primary;
import Repository.LocalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class UDP_storage {
    private final int port = 7003; // UDP 스토리지 포트
    private final Connection2Primary connection2Primary;
    private final BackupController backupController;

    //테스트 해보니 계속 별도처리중곂침 현상 발생
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);//병렬 처리 스레드
    private final BlockingQueue<DatagramPacket> requestQueue = new LinkedBlockingQueue<>();// 들어오는 요청을 큐에 저장하여 최대한 구분


    public UDP_storage(Connection2Primary connection2Primary) {
        this.connection2Primary = connection2Primary;
        LocalRepository localRepository = LocalRepository.getInstance();
        this.backupController = new BackupController(localRepository); // 초기화
    }


    public void startStorage() {
        connection2Primary.initConnectionAndFetchNotes();

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP Storage listening on port " + port);

            // 요청 수신 쓰레드
            new Thread(() -> {
                while (true) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        requestQueue.add(packet); // 요청 큐에 추가
                    } catch (IOException e) {
                        System.err.println("[ERROR] UDP 요청 수신 중 오류 발생: " + e.getMessage());
                    }
                }
            }).start();

            // 요청 처리 쓰레드
            while (true) {
                try {
                    DatagramPacket packet = requestQueue.take(); // 큐에서 요청 가져오기
                    processClientRequest(socket, packet);
                } catch (InterruptedException e) {
                    System.err.println("[ERROR] 요청 처리 중 인터럽트 발생: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] UDP Storage 서버 오류: " + e.getMessage());
        }
    }

    private void processClientRequest(DatagramSocket socket, DatagramPacket packet) {
        executorService.submit(() -> {
            System.out.println("processClientRequest 메서드 시작");

            try {
                // 메시지 변환
                String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[DEBUG] 요청 수신: " + receivedMessage);

                // 메시지 유형 판별
                if (isPrimaryStorageMessage(receivedMessage)) {
                    System.out.println("[DEBUG] 프라이머리 스토리지로부터의 메시지 처리 시작");
                    handlePrimaryStorageMessage(receivedMessage, packet, socket);
                } else {
                    System.out.println("[DEBUG] UDP 클라이언트로부터의 메시지 처리 시작");
                    handleUdpMessage(receivedMessage, packet, socket);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] 클라이언트 요청 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private boolean isPrimaryStorageMessage(String message) {
        // 프라이머리 스토리지에서 오는 메시지인지 확인
        return message.contains("/connect/status") || message.contains("/backup");
    }

    private void handlePrimaryStorageMessage(String message, DatagramPacket packet, DatagramSocket socket) {
        System.out.println("[DEBUG] handlePrimaryStorageMessage 메서드 시작");

        try {
            // 메시지 파싱
            if (message.contains("/connect/status") || message.contains("check_status")) {
                sendUdpResponse(socket, packet, "READY");
                System.out.println("[DEBUG] /connect/status 또는 check_status 응답: READY");
            }else if (message.contains("/backup")) {
                System.out.println("[DEBUG] /backup 요청 처리 시작");
                // JSON 메시지 파싱
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> parsedMessage = objectMapper.readValue(message, Map.class);

                String method = (String) parsedMessage.get("method"); // HTTP 메서드 추출
                String path = (String) parsedMessage.get("path"); // 경로 추출
                String body = parsedMessage.containsKey("body") ? objectMapper.writeValueAsString(parsedMessage.get("body")) : "";

                // BackupController에 메서드, 경로, 바디 전달
                backupController.handleRequest(method, path, body);
                sendUdpResponse(socket, packet, "{\"message\": \"Backup completed\"}");
                System.out.println("[DEBUG] /backup 요청 처리 완료");
            } else {
                // 지원되지 않는 경로 처리
                sendUdpResponse(socket, packet, "{\"error\": \"Unsupported operation\"}");
                System.err.println("[ERROR] 지원되지 않는 프라이머리 메시지: " + message);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] 프라이머리 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }




    private void handleUdpMessage(String receivedMessage, DatagramPacket packet, DatagramSocket socket) {
        System.out.println("handleUdpMessage 메서드 시작");
        try {
            connection2Primary.processMessageFromClient(receivedMessage);
            sendUdpResponse(socket, packet, "READY");
            System.out.println("[DEBUG] UDP 메시지 처리 완료: " + receivedMessage);
        } catch (Exception e) {
            String errorResponse = "{\"error\": \"Processing error\", \"details\": \"" + e.getMessage() + "\"}";
            try {
                sendUdpResponse(socket, packet, errorResponse);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            System.err.println("[ERROR] UDP 메시지 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendUdpResponse(DatagramSocket socket, DatagramPacket requestPacket, String response) throws IOException {
        byte[] responseData = response.getBytes();

        // 요청 패킷의 주소와 포트를 사용하여 응답 패킷 생성
        DatagramPacket responsePacket = new DatagramPacket(
                responseData,
                responseData.length,
                requestPacket.getAddress(),
                requestPacket.getPort()
        );

        // 응답 전송
        socket.send(responsePacket);
    }


}
