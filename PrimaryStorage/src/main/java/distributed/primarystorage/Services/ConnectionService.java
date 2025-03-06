package distributed.primarystorage.Services;

import distributed.primarystorage.Repository.PrimaryNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import distributed.primarystorage.Repository.LocalStorageRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConnectionService {

    //노트저장소
    private final PrimaryNoteRepository primaryNoteRepository;

    @Autowired
    public ConnectionService(PrimaryNoteRepository primaryNoteRepository) {
        this.primaryNoteRepository = primaryNoteRepository;
    }

    //ConnectionController의 getAllNotes에서 사용
    public Map<String, Object> giveallnotes() {
        // 상태와 노트 데이터 반환
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY"); // Primary Storage의 상태 정보
        response.put("notes", primaryNoteRepository.getAllNotesAsList()); // 모든 노트 데이터
        return response;
    }

}
