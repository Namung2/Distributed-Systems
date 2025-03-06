package distributed.primarystorage.Repository;

import distributed.primarystorage.DTO.NoteDto;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;


@Repository
public class PrimaryNoteRepository {
    //싱클톤 인스턴스
    private static final PrimaryNoteRepository INSTANCE = new PrimaryNoteRepository();

    //데이터 저장을
    private final ConcurrentHashMap<Integer, NoteDto> notes = new ConcurrentHashMap<>();
    private int currentID = 1; //id부여 변수

    private PrimaryNoteRepository() {}

    public static PrimaryNoteRepository getInstance() {
        return INSTANCE;
    }
    // 모든 노트를 반환하는 메서드
    public List<NoteDto> getAllNotesAsList() {
        return new ArrayList<>(notes.values());
    }

    public int addNote(NoteDto note) {
        notes.put(currentID,note);
        return currentID++;
    }

    public NoteDto getNoteById(int id) {
        return notes.get(id);
    }

    public void updateNote(int id, NoteDto note) {
        notes.put(id, note);
    }

    public void removeNoteById(int id) {
        notes.remove(id);
    }

}
