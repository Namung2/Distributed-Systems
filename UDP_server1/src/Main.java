import SendToStorage.UdpClient;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // UDP 클라이언트 설정
        UdpClient udpClient = new UdpClient("localhost", 7003);

        // 입력받기 위한 Scanner 객체 생성
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== UDP Client 테스트 프로그램 ===");
        System.out.println("1. GET 요청 (/notes)");
        System.out.println("2. POST 요청 (/notes)");
        System.out.println("3. PUT 요청 (/notes/{id})");
        System.out.println("4. PATCH 요청 (/notes/{id})");
        System.out.println("5. DELETE 요청 (/notes/{id})");
        System.out.println("6. 종료");
        System.out.println("=================================");

        while (running) {
            System.out.print("원하는 작업의 번호를 입력하세요 (1~6): ");
            String input = scanner.nextLine();

            switch (input) {
                case "1": // GET 요청
                    System.out.println("[INFO] GET 요청 테스트");
                    udpClient.sendMessageToStorage("GET", "/notes", null, null);
                    break;

                case "2": // POST 요청
                    System.out.println("[INFO] POST 요청 테스트");
                    System.out.print("노트 제목 입력: ");
                    String postTitle = scanner.nextLine();
                    System.out.print("노트 내용 입력: ");
                    String postBody = scanner.nextLine();
                    udpClient.sendMessageToStorage("POST", "/notes", postTitle, postBody);
                    break;

                case "3": // PUT 요청
                    System.out.println("[INFO] PUT 요청 테스트");
                    System.out.print("수정할 노트 ID 입력: ");
                    String putId = scanner.nextLine();
                    System.out.print("수정할 제목 입력: ");
                    String putTitle = scanner.nextLine();
                    System.out.print("수정할 내용 입력: ");
                    String putBody = scanner.nextLine();
                    udpClient.sendMessageToStorage("PUT", "/notes/" + putId, putTitle, putBody);
                    break;

                case "4": // PATCH 요청
                    System.out.println("[INFO] PATCH 요청 테스트");
                    System.out.print("수정할 노트 ID 입력: ");
                    String patchId = scanner.nextLine();
                    System.out.print("수정할 필드 (title/body): ");
                    String patchField = scanner.nextLine();
                    System.out.print("수정할 값 입력: ");
                    String patchValue = scanner.nextLine();
                    udpClient.sendMessageToStorage("PATCH", "/notes/" + patchId, patchField, patchValue);
                    break;

                case "5": // DELETE 요청
                    System.out.println("[INFO] DELETE 요청 테스트");
                    System.out.print("삭제할 노트 ID 입력: ");
                    String deleteId = scanner.nextLine();
                    udpClient.sendMessageToStorage("DELETE", "/notes/" + deleteId, null, null);
                    break;

                case "6": // 종료
                    System.out.println("[INFO] 프로그램을 종료합니다.");
                    running = false;
                    break;

                default: // 잘못된 입력 처리
                    System.out.println("[ERROR] 잘못된 입력입니다. 1~6 중에서 선택해주세요.");
            }
        }

        scanner.close();
    }
}
