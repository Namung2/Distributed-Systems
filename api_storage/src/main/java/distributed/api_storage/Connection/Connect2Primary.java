package distributed.api_storage.Connection;

import distributed.api_storage.Dto.NoteDto;
import distributed.api_storage.Repository.LocalNoteRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/connect")
public class Connect2Primary {

    private final RestTemplate restTemplate = new RestTemplate();
    private final LocalNoteRepository localStorage = LocalNoteRepository.getInstance();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${primary.storage.url}")
    private String primaryStorageUrl;

    @PostConstruct
    public void initConnectionAndFetchNotes() {
        try {
            // Primary Storage의 /primary/allnotes 엔드포인트에 접속
            String dataUrl = primaryStorageUrl + "/primary/allnotes";

            System.out.println("Sending Request to Primary Storage:");
            System.out.println("Data URL: " + dataUrl);

            // RestTemplate을 사용하여 GET 요청 보내기
            ResponseEntity<Map> responseEntity = restTemplate.getForEntity(dataUrl, Map.class);
            Map<String, Object> response = responseEntity.getBody();

            // Primaray가 READY일 때 모든 노트 수신
            if (response != null && "READY".equals(response.get("status"))) {
                System.out.println("Primary Storage가 준비되었습니다. 모든 노트를 불러옵니다.");

                // notes 필드를 List<LinkedHashMap>으로 처리한 후 NoteDto로 변환
                List<?> rawNotes = (List<?>) response.get("notes");
                List<NoteDto> notes = rawNotes.stream()
                        .map(note -> objectMapper.convertValue(note, NoteDto.class))
                        .collect(Collectors.toList());

                if (notes.isEmpty()) {
                    System.out.println("현재 저장된 노트는 없습니다.");
                } else {
                    notes.forEach(localStorage::addNote);
                    System.out.println("Primary Storage로부터 전체 데이터를 성공적으로 받았습니다.");
                }
            } else {
                System.out.println("Primary Storage가 아직 준비되지 않았습니다.");
            }
        } catch (Exception e) {
            System.out.println("Primary Storage에 연결할 수 없습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // /status 엔드포인트를 추가하여 READY 상태를 반환
    @GetMapping("/status")
    public String getStatus() {
        return "READY";
    }
}
