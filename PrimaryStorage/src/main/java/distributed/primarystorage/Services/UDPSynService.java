package distributed.primarystorage.Services;

import distributed.primarystorage.DTO.NoteDto;
import distributed.primarystorage.Repository.LocalStorageRepository;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@Service
public class UDPSynService {

    private final String udpAddress = "localhost"; // UDP 스토리지 주소
    private final int udpPort = 7003; // UDP 스토리지 포트
    private final LocalStorageRepository localStorageRepository;

    // 생성자 주입
    public UDPSynService(LocalStorageRepository localStorageRepository) {
        this.localStorageRepository = localStorageRepository;
    }
    // UDP 스토리지 상태 확인
    public void checkUdpStorageStatus() {
        String statusMessage = "{\"cmd\":\"check_status\"}";

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            // 상태 요청 전송
            byte[] messageBytes = statusMessage.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(
                    messageBytes, messageBytes.length, InetAddress.getByName(udpAddress), udpPort
            );
            udpSocket.send(requestPacket);
            System.out.println("[DEBUG] UDP 스토리지 상태 확인 메시지 전송");

            // 응답 대기
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            udpSocket.setSoTimeout(3000); // 최대 3초 대기
            udpSocket.receive(responsePacket);

            String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();
            System.out.println("[DEBUG] UDP 스토리지 상태 응답 수신: " + responseMessage);

            if ("READY".equalsIgnoreCase(responseMessage)) {
                System.out.println("[DEBUG] UDP 스토리지가 준비되었습니다.");
                // 여기서 로컬 스토리지 레포지토리에 UDP 스토리지를 추가
                localStorageRepository.addLocalStorage(udpAddress, udpPort);
            } else {
                System.err.println("[ERROR] UDP 스토리지 응답이 올바르지 않습니다: " + responseMessage);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] UDP 스토리지 상태 확인 실패: " + e.getMessage());
        }
    }

    // UDP 메시지 전송 메서드
    private void sendUdpMessage(String message) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] messageBytes = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    messageBytes, messageBytes.length, InetAddress.getByName(udpAddress), udpPort
            );
            udpSocket.send(packet);
            System.out.println("[DEBUG] UDP 메시지 전송 완료: " + message);
        } catch (Exception e) {
            System.err.println("[ERROR] UDP 메시지 전송 실패: " + e.getMessage());
        }
    }

    // UDP 스토리지에 POST 요청
    public void sendPostToUdp(NoteDto note) {
        String noteJson = String.format(
                "{\"method\":\"POST\",\"path\":\"/backup\",\"body\":%s}",
                note.toJson()
        );
        sendUdpMessage(noteJson);
        System.out.println("[DEBUG] UDP 스토리지에 POST 동기화 메시지 전송: " + noteJson);
    }

    // UDP 스토리지에 UPDATE 요청
    public void sendUpdateToUdp(NoteDto note, String method) {
        String noteJson = String.format(
                "{\"method\":\"%s\",\"path\":\"/backup/%d\",\"body\":%s}",
                method, note.getId(), note.toJson()
        );
        sendUdpMessage(noteJson);
        System.out.println("[DEBUG] UDP 스토리지에 " + method + " 동기화 메시지 전송: " + noteJson);
    }

    // UDP 스토리지에 DELETE 요청
    public void sendDeleteToUdp(int id) {
        String deleteMessage = String.format(
                "{\"method\":\"DELETE\",\"path\":\"/backup/%d\"}",
                id
        );
        sendUdpMessage(deleteMessage);
        System.out.println("[DEBUG] UDP 스토리지에 DELETE 동기화 메시지 전송: " + deleteMessage);
    }
}
