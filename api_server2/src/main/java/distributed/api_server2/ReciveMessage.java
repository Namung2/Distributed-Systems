package distributed.api_server2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class ReciveMessage {
    @PostMapping("/message")
    public String receiveMessage(@RequestBody String message){
        System.out.println(message);
        return message;
    }
}
