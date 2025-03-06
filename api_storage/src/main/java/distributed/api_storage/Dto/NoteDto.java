package distributed.api_storage.Dto;


import com.fasterxml.jackson.annotation.JsonInclude; // Import for excluding null values
import com.fasterxml.jackson.annotation.JsonProperty; // Import for custom JSON property names
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값은 JSON 출력에서 제외
@Getter
public class NoteDto {

    // Getter와 Setter
    private int id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("body")
    private String body;

    // 기본 생성자
    public NoteDto() {}

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