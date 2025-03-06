package distributed.api_server1;

import distributed.api_server1.Dto.NoteDto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;

@SpringBootApplication
public class ApiServer1Application {
	public static void main(String[] args) {
		SpringApplication.run(ApiServer1Application.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public CommandLineRunner run(RestTemplate restTemplate) {
		return args -> {
			Scanner scanner = new Scanner(System.in);
			boolean running = true;

			String apiUrl = "http://localhost:7001/notes";

			System.out.println("=== API Storage 클라이언트 ===");
			System.out.println("1. GET 요청 (/notes)");
			System.out.println("2. POST 요청 (/notes)");
			System.out.println("3. PUT 요청 (/notes/{id})");
			System.out.println("4. PATCH 요청 (/notes/{id})");
			System.out.println("5. DELETE 요청 (/notes/{id})");
			System.out.println("6. 종	료");
			System.out.println("=============================");

			while (running) {
				System.out.print("원하는 작업의 번호를 입력하세요 (1~6): ");
				String input = scanner.nextLine();

				try {
					switch (input) {
						case "1": // GET 요청
							System.out.println("[INFO] GET 요청 테스트");
							String response = restTemplate.getForObject(apiUrl, String.class);
							System.out.println("응답: " + response);
							break;

						case "2": // POST 요청
							System.out.println("[INFO] POST 요청 테스트");
							System.out.print("노트 제목 입력: ");
							String title = scanner.nextLine();
							System.out.print("노트 내용 입력: ");
							String body = scanner.nextLine();
							NoteDto newNote = new NoteDto(title, body);

							NoteDto postResponse = restTemplate.postForObject(apiUrl, newNote, NoteDto.class);
							System.out.println("응답: 노트 저장 완료");
							break;

						case "3": // PUT 요청
							System.out.println("[INFO] PUT 요청 테스트");
							System.out.print("수정할 노트 ID 입력: ");
							String putId = scanner.nextLine();
							System.out.print("새 제목 입력: ");
							String newTitle = scanner.nextLine();
							System.out.print("새 내용 입력: ");
							String newBody = scanner.nextLine();
							NoteDto updatedNote = new NoteDto(newTitle, newBody);

							restTemplate.put(apiUrl + "/" + putId, updatedNote);
							System.out.println("PUT 요청이 성공적으로 완료되었습니다.");
							break;

						case "4": // PATCH 요청
							System.out.println("[INFO] PATCH 요청 테스트");
							System.out.print("수정할 노트 ID 입력: ");
							String patchId = scanner.nextLine();
							System.out.print("수정할 필드와 값 입력 (예: \"title\":\"새 제목\"): ");
							String patchField = scanner.nextLine();

							restTemplate.patchForObject(apiUrl + "/" + patchId, patchField, String.class);
							System.out.println("PATCH 요청이 성공적으로 완료되었습니다.");
							break;

						case "5": // DELETE 요청
							System.out.println("[INFO] DELETE 요청 테스트");
							System.out.print("삭제할 노트 ID 입력: ");
							String deleteId = scanner.nextLine();

							restTemplate.delete(apiUrl + "/" + deleteId);
							System.out.println("DELETE 요청이 성공적으로 완료되었습니다.");
							break;

						case "6": // 종료
							System.out.println("[INFO] 프로그램을 종료합니다.");
							running = false;
							break;

						default:
							System.out.println("[ERROR] 잘못된 입력입니다. 1~6 중에서 선택해주세요.");
					}
				} catch (Exception e) {

					System.err.println("[ERROR] 요청 처리 중 오류 발생: " + e.getMessage());
				}
			}
		};
	}
}