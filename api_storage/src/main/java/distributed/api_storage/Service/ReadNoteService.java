package distributed.api_storage.Service;

import distributed.api_storage.Dto.NoteDto;
import distributed.api_storage.Repository.LocalNoteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReadNoteService {

    private final LocalNoteRepository localStorage = LocalNoteRepository.getInstance();

    public List<NoteDto> getAllNotes() {
        return new ArrayList<>(localStorage.getAllNotes().values());
    }

    //특정 ID 반환
    public NoteDto getNoteById(int id) {
        NoteDto note = localStorage.getNoteById(id);
        if(note != null) {
            return note;
        }else{
            throw new IllegalArgumentException("해당 ID의 노트를 찾을 수 없습니다.");
        }
    }
}
