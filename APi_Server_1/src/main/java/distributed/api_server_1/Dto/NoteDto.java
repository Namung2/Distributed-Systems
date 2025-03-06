package distributed.api_server_1.Dto;
public class NoteDto {
    private final String title;
    private final String body;
    public NoteDto(String title, String body) {
        this.title = title;
        this.body = body;
    }

    @Override
    public String toString() {
        return "NoteDto{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}