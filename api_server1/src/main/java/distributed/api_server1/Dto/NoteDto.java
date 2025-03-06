package distributed.api_server1.Dto;

public class NoteDto {
    private String title;
    private String body;

    public NoteDto() {}

    public NoteDto(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
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
