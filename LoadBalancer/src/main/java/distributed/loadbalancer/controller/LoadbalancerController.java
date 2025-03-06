package distributed.loadbalancer.controller;

import distributed.loadbalancer.service.LoadbalancingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/loadbalancer")
@RequiredArgsConstructor
public class LoadbalancerController {

    private final LoadbalancingService loadbalancingService;

    // /sendmessage 엔드포인트를 통해 메시지를 처리
    @PostMapping("/sendmessage")
    public ResponseEntity<String> sendMessage(@RequestBody String message) {
        log.info("Received message to send: {}", message);

        String response = loadbalancingService.handleSendingMessage(message);

        log.info("Message sending result: {}", response);
        return ResponseEntity.ok(response);
    }
}