package distributed.api_storage.Repository;

import distributed.api_storage.Dto.NoteDto;

import java.util.concurrent.ConcurrentHashMap;

public class LocalNoteRepository {
    private static final LocalNoteRepository INSTANCE = new LocalNoteRepository();
    private final ConcurrentHashMap<Integer, NoteDto> notes = new ConcurrentHashMap<>();

    private LocalNoteRepository() {}

    public static LocalNoteRepository getInstance() {
        return INSTANCE;
    }

    public ConcurrentHashMap<Integer, NoteDto> getAllNotes() {
        return notes;
    }

    public void addNote(NoteDto note) {
        notes.put(note.getId(), note);
    }

    public NoteDto getNoteById(int id) {
        return notes.get(id);
    }

    public void removeNoteById(int id) {
        notes.remove(id);
    }
}
