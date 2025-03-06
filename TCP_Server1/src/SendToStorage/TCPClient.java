package SendToStorage;

import java.io.*;
import java.net.Socket;
public class TCPClient {
    private String serverAddress;
    private int port;

    public TCPClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public void sendMessageToStorage(String jsonMessage) {
        try (
                Socket socket = new Socket(serverAddress, port);
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(outputStream, true);
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            // 타임아웃 설정 (5초)
            socket.setSoTimeout(5000);

            // JSON 메시지 전송
            writer.println(jsonMessage);
            writer.flush(); // 데이터 강제 전송
            System.out.println("TCP 스토리지 서버에 JSON 메시지를 전송했습니다.");

            // 서버 응답 읽기
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.trim().equals("END")) { // 서버가 END로 종료 신호를 보낼 경우
                    System.out.println("서버 응답 완료.");
                    break;
                }
                System.out.println("서버로부터 받은 응답: " + response);
            }

        } catch (IOException e) {
            System.err.println("서버와 통신 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
