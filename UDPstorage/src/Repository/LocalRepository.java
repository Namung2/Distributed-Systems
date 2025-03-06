package Repository;

import Dto.NoteDto;
import java.util.HashMap;
import java.util.Map;

public class LocalRepository {
    private static LocalRepository instance; // 싱글톤 인스턴스
    private final Map<Integer, NoteDto> dataStore = new HashMap<>(); // NoteDto 객체를 저장

    // private 생성자: 외부에서 직접 생성하지 못하도록 막음
    private LocalRepository() {}

    // 싱글톤 인스턴스 반환 메서드
    public static synchronized LocalRepository getInstance() {
        if (instance == null) {
            instance = new LocalRepository(); // 인스턴스가 없을 경우 생성
        }
        return instance; // 이미 생성된 경우 기존 인스턴스 반환
    }

    // NoteDto 객체를 저장하는 메서드
    public synchronized void addNote(NoteDto note) {
        dataStore.put(note.getId(), note);
        System.out.println("NoteDto 객체가 로컬 저장소에 추가되었습니다: " + note);
    }

    // 특정 ID의 데이터 조회 메서드
    public synchronized NoteDto getNoteById(int id) {
        return dataStore.get(id);
    }

    // 모든 데이터를 반환하는 메서드
    public synchronized Map<Integer, NoteDto> getAllNotes() {
        return new HashMap<>(dataStore); // 복사본 반환
    }

    // 특정 ID의 데이터를 삭제하는 메서드
    public synchronized boolean removeNoteById(int id) {
        if (dataStore.containsKey(id)) {
            dataStore.remove(id);
            return true;
        }
        return false;
    }

    // 특정 ID의 데이터를 업데이트하는 메서드
    public synchronized void updateNote(int id, NoteDto note) {
        dataStore.put(id, note);
    }
}
