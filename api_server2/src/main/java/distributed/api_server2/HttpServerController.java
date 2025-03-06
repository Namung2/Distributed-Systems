package distributed.api_server2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class HttpServerController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String loadBalancerUrl = "http://localhost:8080/loadbalancer";
    private static int serverPort=8082;

    // 서버 등록 및 해제 요청을 처리
    @GetMapping("/register")
    public String registerOrUnregister(@RequestParam("action") String action) {
        log.info("API server_1 request: {}" + action);
        String jsonRequest;

        //register OR unregister request
        if("register".equalsIgnoreCase(action)) {
            jsonRequest = String.format("{\"cmd\":\"register\",\"protocol\":\"api\",\"port\":%d}",serverPort);
        }else if("unregister".equalsIgnoreCase(action)) {
            jsonRequest = String.format("{\"cmd\":\"unregister\",\"protocol\":\"api\",\"port\":%s}",serverPort);
        }else {
            return "wrong action";
        }
        log.info("JSON request: {}", jsonRequest);
        return sendRequest2LoadBalancer(jsonRequest,action);
    }
    private String sendRequest2LoadBalancer(String jsonRequest,String action) {
        try{
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequest, headers);

            String endpoint = "/register";
            if("unregister".equalsIgnoreCase(action)) {endpoint = "/unregister";}
            String response = restTemplate.postForObject(
                    loadBalancerUrl + endpoint, requestEntity, String.class
            );

            log.info("Response from LoadBalancer: {}", response);
            return "Response from Loadbalancer" + response;
        }catch (Exception e){
            log.error("Failed to send loadbalancer", e);
            return "{\"ack\":\"failed\",\"msg\":\"Failed to communicate with LoadBalancer\"}";
        }
    }

    // 헬스 체크 엔드포인트 (POST 요청 처리)
    @PostMapping("/health")
    public Map<String, String> healthCheck(@RequestBody Map<String, String> request) {
        log.info("Received health check request: {}", request);
        if ("hello".equals(request.get("cmd"))) {
            log.info("ACK sent.");
            return Map.of("ack", "hello");
        } else {
            log.warn("Invalid health check command received.");
            return Map.of("ack", "invalid");
        }
    }
}
//postman사용시 클라이언트가 get으로 보내면 localhost:8082/api/register?action=register
//post로 보내면 localhost:8080/loadbalancer/regisger {"protocol":"api","port":"8081"} 라고등록

//브라우저 사용시 localhost:사용할 포트번호/api/register?action=register 이렇게 등록 해제는 action=unregister