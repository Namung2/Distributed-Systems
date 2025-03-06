package Dto;

public class NoteDto {

    private int id;
    private String title;
    private String body;

    // 기본 생성자
    public NoteDto() {}

    // Getter와 Setter
    public int getId() {
        return id;
    }

    public void setID(int id){
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}