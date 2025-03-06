package distributed.api_storage.Controller;


import distributed.api_storage.Dto.NoteDto;
import distributed.api_storage.Service.ReadNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final RestTemplate restTemplate;
    private final ReadNoteService readNoteService;

    @Value("${primary.storage.url}")//5000
    private String primaryStorageUrl;

    @Autowired
    public NoteController(RestTemplate restTemplate,ReadNoteService readNoteService) {
        this.readNoteService = readNoteService;
        this.restTemplate = restTemplate;
    }

    // POST 요청을 Primary Storage로 전달
    @PostMapping
    public String addNoteToPrimary(@RequestBody NoteDto note) {
        return restTemplate.postForObject(primaryStorageUrl + "/primary", note, String.class);
    }

    // PUT 요청을 Primary Storage로 전달
    @PutMapping("/{id}")
    public void updateNoteInPrimary(@PathVariable int id, @RequestBody NoteDto note) {
        restTemplate.put(primaryStorageUrl + "/primary/" + id, note);
    }

    // PATCH 요청을 Primary Storage로 전달
    @PatchMapping("/{id}")
    public void patchNoteInPrimary(@PathVariable int id, @RequestBody NoteDto updates) {
        restTemplate.patchForObject(primaryStorageUrl + "/primary/" + id, updates, String.class);
    }

    // DELETE 요청을 Primary Storage로 전달
    @DeleteMapping("/{id}")
    public void deleteNoteInPrimary(@PathVariable int id) {
        restTemplate.delete(primaryStorageUrl + "/primary/" + id);
    }



    // 전체 메모 반환 (GET 요청)
    @GetMapping
    public List<NoteDto> getAllNotes() {
        return readNoteService.getAllNotes();
    }

    // 특정 ID의 메모 반환 (GET 요청)
    @GetMapping("/{id}")
    public NoteDto getNoteById(@PathVariable int id) {
        return readNoteService.getNoteById(id);
    }
}
