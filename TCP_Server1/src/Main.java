import SendToStorage.TCPClient;
import loadbalance.TcpServer;
import service.SocketConnection;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        TCPClient client = new TCPClient("localhost", 7002); // TCP 스토리지 서버 주소와 포트 설정
        int port = 9001; // HealthCheck 포트 설정

        String loadBalancerHost = "localhost"; // 로드 밸런서 호스트
        int loadBalancerPort = 8080; // 로드 밸런서 포트

        while (true) {
            System.out.println("\n--- 메뉴 선택 ---");
            System.out.println("1. 로드 밸런서 연결 모드");
            System.out.println("2. TCP 스토리지 연결 모드");
            System.out.println("3. 종료");
            System.out.print("번호를 선택하세요: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    TcpServer tcpServer = new TcpServer(loadBalancerHost, loadBalancerPort);
                    Thread tcpServerThread = new Thread(tcpServer::startConsole);

                    // 스레드 시작
                    tcpServerThread.start();

                    SocketConnection socketConnection = new SocketConnection(port);
                    socketConnection.start();

                    System.out.println("로드 밸런서 연결 모드가 실행되었습니다.");
                    break;

                case "2":
                    while (true) {
                        System.out.println("\n--- TCP 스토리지 연결 ---");
                        System.out.println("1. 노트 조회 (GET /notes)");
                        System.out.println("2. 노트 추가 (POST /notes)");
                        System.out.println("3. 노트 수정 (PUT /notes/{id})");
                        System.out.println("4. 노트 일부 수정 (PATCH /notes/{id})");
                        System.out.println("5. 노트 삭제 (DELETE /notes/{id})");
                        System.out.println("6. 종료");
                        System.out.print("작업을 선택하세요: ");
                        String storageInput = scanner.nextLine().trim();

                        switch (storageInput) {
                            case "1": // GET 요청
                                client.sendMessageToStorage("{\"method\": \"GET\", \"path\": \"/notes\"}");
                                break;
                            case "2": // POST 요청
                                System.out.print("추가할 노트의 제목: ");
                                String title = scanner.nextLine();
                                System.out.print("추가할 노트의 내용: ");
                                String body = scanner.nextLine();
                                client.sendMessageToStorage(String.format(
                                        "{\"method\": \"POST\", \"path\": \"/notes\"}|{\"title\": \"%s\", \"body\": \"%s\"}",
                                        title, body));
                                break;
                            case "3": // PUT 요청
                                System.out.print("수정할 노트 ID: ");
                                String putId = scanner.nextLine();
                                System.out.print("새로운 제목: ");
                                String putTitle = scanner.nextLine();
                                System.out.print("새로운 내용: ");
                                String putBody = scanner.nextLine();
                                client.sendMessageToStorage(String.format(
                                        "{\"method\": \"PUT\", \"path\": \"/notes/%s\"}|{\"title\": \"%s\", \"body\": \"%s\"}",
                                        putId, putTitle, putBody));
                                break;
                            case "4": // PATCH 요청
                                System.out.print("일부 수정할 노트 ID: ");
                                String patchId = scanner.nextLine();
                                System.out.print("수정할 필드 (예: title/body): ");
                                String field = scanner.nextLine();
                                System.out.print("수정할 값: ");
                                String patchValue = scanner.nextLine();
                                client.sendMessageToStorage(String.format(
                                        "{\"method\": \"PATCH\", \"path\": \"/notes/%s\"}|{ \"%s\": \"%s\"}}",
                                        patchId, field, patchValue));
                                break;
                            case "5": // DELETE 요청
                                System.out.print("삭제할 노트 ID: ");
                                String deleteId = scanner.nextLine();
                                client.sendMessageToStorage(String.format(
                                        "{\"method\": \"DELETE\", \"path\": \"/notes/%s\"}",
                                        deleteId));
                                break;
                            case "6":
                                System.out.println("TCP 스토리지 연결 종료.");
                                break;
                            default:
                                System.out.println("잘못된 입력입니다. 다시 선택하세요.");
                        }

                        if (storageInput.equals("6")) {
                            break;
                        }
                    }
                    break;

                case "3":
                    System.out.println("프로그램을 종료합니다.");
                    System.exit(0);
                    break;

                default:
                    System.out.println("잘못된 입력입니다. 다시 선택하세요.");
            }
        }
    }
}
