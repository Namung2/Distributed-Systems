package distributed.primarystorage.Controller;

import distributed.primarystorage.DTO.NoteDto;
import distributed.primarystorage.Services.SynService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import distributed.primarystorage.Repository.PrimaryNoteRepository;

import java.util.Map;

@RestController
@RequestMapping("/primary")
public class PrimaryStorageController {

    // 데이터 저장을 위한 간단한 메모리 내 저장소 (ID와 필드를 저장)
    private final PrimaryNoteRepository storage;
    private final SynService synService; // final로 선언하여 불변성을 유지

    // 생성자를 통해 의존성 주입
    @Autowired
    public PrimaryStorageController(SynService synService) {
        this.storage = PrimaryNoteRepository.getInstance();
        this.synService = synService;
    }

    @PostMapping
    public ResponseEntity<?> addNote(@RequestBody NoteDto note) {
        try {
            System.out.println("[DEBUG] POST 요청 수신: /primary, 데이터: " + note);
            int id = storage.addNote(note);
            note.setID(id);

            System.out.println("[DEBUG] 노트 저장 성공. ID: " + id);

            synService.synWithLocalStoragesForPost(note);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "노트가 추가되었습니다.", "id", id));
        } catch (Exception e) {
            System.err.println("[ERROR] POST 요청 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", "노트 추가 중 오류 발생", "details", e.getMessage()));
        }
    }


    // 특정 ID의 노트를 삭제하는 엔드포인트 3개
    // PUT(기존 노트를 새로운 메모로 덮어씀),PATCH(기존 노트중 일부 필드 수정),DELETE(해당 ID 노트를 삭제)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNoteById(@PathVariable int id, @RequestBody NoteDto note) {
        try {
            if (storage.getNoteById(id) != null) {
                note.setID(id);
                storage.updateNote(id, note);
                synService.synWithLocalStoragesForUpdate(note, "PUT");
                return ResponseEntity.ok(Map.of("message", "노트가 업데이트되었습니다.", "id", id));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "노트를 찾을 수 없습니다.", "id", id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "노트 업데이트 중 오류 발생", "details", e.getMessage()));
        }
    }
    // PATCH 메서드: 기존 노트의 일부 필드를 수정
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchNoteById(@PathVariable int id, @RequestBody NoteDto updates) {
        try {
            NoteDto existingNote = storage.getNoteById(id); //id에 맞게 노트 갖고오기
            if (existingNote != null) {
                //json 필드 업데이트
                if (updates.getTitle() != null) {
                    existingNote.setTitle(updates.getTitle());
                }
                if (updates.getBody() != null) {
                    existingNote.setBody(updates.getBody());
                }
                storage.updateNote(id, existingNote);
                synService.synWithLocalStoragesForUpdate(existingNote, "PATCH");
                return ResponseEntity.ok(Map.of("message", "노트가 부분적으로 수정되었습니다.", "id", id));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "노트를 찾을 수 없습니다.", "id", id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "노트 수정 중 오류 발생", "details", e.getMessage()));
        }
    }

    // DELETE 메서드: 노트를 삭제 ID에 맞게 메모 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNoteById(@PathVariable int id) {
        try {
            if (storage.getNoteById(id) != null) {
                storage.removeNoteById(id);//먼저 primaryStorage에서 삭제하고
                synService.synWithLocalStoragesForDelete(id);//동기화 요청 보내기
                return ResponseEntity.ok(Map.of("message", "노트가 삭제되었습니다.", "id", id));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "노트를 찾을 수 없습니다.", "id", id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "노트 삭제 중 오류 발생", "details", e.getMessage()));
        }
    }
}
