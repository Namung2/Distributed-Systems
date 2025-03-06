package distributed.api_server1.Controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/status")
public class StatusController {

    @GetMapping
    public String getStatus() {
        // ApiServer의 현재 상태 반환
        return "connected";
    }
}
