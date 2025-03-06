package distributed.api_storage.Controller;

import distributed.api_storage.Dto.NoteDto;
import distributed.api_storage.Repository.LocalNoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backup")
public class BackupController {

    private static final Logger log = LoggerFactory.getLogger(BackupController.class);
    private final LocalNoteRepository localStorage = LocalNoteRepository.getInstance();

    // POST 요청: 새로운 노트 추가
    @PostMapping
    public String addNoteBackup(@RequestBody NoteDto note) {
        log.info("새로운 노트 추가 요청: {}", note);
        localStorage.addNote(note);
        log.info("노트가 로컬 스토리지에 추가되었습니다. ID: {}", note.getId());
        return "노트가 로컬 스토리지에 추가되었습니다.";
    }

    // PUT 요청: 기존 노트 업데이트
    @PutMapping("/{id}")
    public String updateNoteBackup(@PathVariable int id, @RequestBody NoteDto note) {
        log.info("노트 업데이트 요청 - ID: {}", id);
        NoteDto existingNote = localStorage.getNoteById(id);
        if (existingNote != null) {
            localStorage.addNote(note); // 기존 노트를 새로운 내용으로 덮어씀
            log.info("ID {}의 노트가 로컬 스토리지에서 업데이트되었습니다.", id);
            return "ID " + id + "의 노트가 로컬 스토리지에서 업데이트되었습니다.";
        } else {
            log.warn("ID {}의 노트를 찾을 수 없습니다.", id);
            return "ID " + id + "의 노트를 찾을 수 없습니다.";
        }
    }

    // PATCH 요청: 기존 노트의 일부 필드 수정
    @PatchMapping("/{id}")
    public String patchNoteBackup(@PathVariable int id, @RequestBody NoteDto updates) {
        log.info("노트 부분 수정 요청 - ID: {}", id);
        NoteDto existingNote = localStorage.getNoteById(id);
        if (existingNote != null) {
            // 필요한 필드만 업데이트
            if (updates.getTitle() != null) {
                existingNote.setTitle(updates.getTitle());
                log.info("ID {}의 제목이 업데이트되었습니다.", id);
            }
            if (updates.getBody() != null) {
                existingNote.setBody(updates.getBody());
                log.info("ID {}의 본문이 업데이트되었습니다.", id);
            }
            localStorage.addNote(existingNote); // 수정된 내용으로 업데이트
            log.info("ID {}의 노트가 로컬 스토리지에서 부분적으로 수정되었습니다.", id);
            return "ID " + id + "의 노트가 로컬 스토리지에서 부분적으로 수정되었습니다.";
        } else {
            log.warn("ID {}의 노트를 찾을 수 없습니다.", id);
            return "ID " + id + "의 노트를 찾을 수 없습니다.";
        }
    }

    // DELETE 요청: 노트 삭제
    @DeleteMapping("/{id}")
    public String deleteNoteBackup(@PathVariable int id) {
        log.info("노트 삭제 요청 - ID: {}", id);
        if (localStorage.getNoteById(id) != null) {
            localStorage.removeNoteById(id);
            log.info("ID {}의 노트가 로컬 스토리지에서 삭제되었습니다.", id);
            return "ID " + id + "의 노트가 로컬 스토리지에서 삭제되었습니다.";
        } else {
            log.warn("ID {}의 노트를 찾을 수 없습니다.", id);
            return "ID " + id + "의 노트를 찾을 수 없습니다.";
        }
    }
}
