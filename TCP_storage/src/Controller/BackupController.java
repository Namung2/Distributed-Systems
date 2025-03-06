package Controller;

import Dto.NoteDto;
import Repository.LocalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
public class BackupController {
    private final LocalRepository localRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BackupController(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }
    public void handleRequest(String method, String path, String body) {
        try {
            switch (method.toUpperCase()) {
                case "POST":
                    handlePost(body);
                    break;
                case "PUT":
                    handlePut(path, body);
                    break;
                case "PATCH":
                    handlePatch(path, body);
                    break;
                case "DELETE":
                    handleDelete(path);
                    break;
                default:
                    System.out.println("지원되지 않는 메서드: " + method);
                    break;
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 요청 처리 중 오류 발생: " + e.getMessage());
        }
    }

    private void handlePost(String body) throws IOException {
        if (body == null || body.isEmpty()) {
            System.err.println("[ERROR] 요청 본문이 비어 있습니다.");
            return;
        }
        try {
            NoteDto note = objectMapper.readValue(body, NoteDto.class);
            localRepository.addNote(note);
            System.out.println("[INFO] POST 요청 처리 완료: " + note);
        } catch (Exception e) {
            System.err.println("[ERROR] JSON 파싱 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void handlePut(String path, String body) throws IOException {
        Integer id = extractIdFromPath(path);
        if (id == null) {
            System.err.println("[ERROR] 유효하지 않은 경로: " + path);
            return;
        }
        NoteDto note = objectMapper.readValue(body, NoteDto.class);
        localRepository.updateNote(id, note);
    }

    private void handlePatch(String path, String body) throws IOException {
        Integer id = extractIdFromPath(path);
        if (id == null) {
            System.err.println("[ERROR] 유효하지 않은 경로: " + path);
            return;
        }
        NoteDto existingNote = localRepository.getNoteById(id);
        if (existingNote == null) {
            System.err.println("[ERROR] PATCH 요청: 노트를 찾을 수 없습니다.");
            return;
        }
        NoteDto patchData = objectMapper.readValue(body, NoteDto.class);
        if (patchData.getTitle() != null) {
            existingNote.setTitle(patchData.getTitle());
        }
        if (patchData.getBody() != null) {
            existingNote.setBody(patchData.getBody());
        }
        localRepository.updateNote(id, existingNote);
    }

    private void handleDelete(String path) {
        Integer id = extractIdFromPath(path);
        if (id == null) {
            System.err.println("[ERROR] 유효하지 않은 경로: " + path);
            return;
        }
        if (localRepository.removeNoteById(id)) {
            System.out.println("[DEBUG] DELETE 요청 처리 완료: ID " + id);
        } else {
            System.err.println("[ERROR] DELETE 요청: 노트를 찾을 수 없습니다.");
        }
    }

    private Integer extractIdFromPath(String path) {
        String[] parts = path.split("/");
        try {
            return parts.length > 2 ? Integer.parseInt(parts[2]) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
