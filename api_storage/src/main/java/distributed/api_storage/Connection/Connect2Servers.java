package distributed.api_storage.Connection;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/connect")
public class Connect2Servers {
    private final RestTemplate restTemplate=new RestTemplate();

    @Value("${apiserver1.url}")
    private String apiServer1url;

    @Value("${apiserver2.url}")
    private String apiServer2url;

    @PostConstruct
    public void initConnection2Servers() {
        System.out.println("8081은 apiserver1의 포트 번호이고,8082는 apiserver2의 포트 번호입니다.");
        connect2ApiServers(apiServer1url);
        connect2ApiServers(apiServer2url);
    }

    private void connect2ApiServers(String apiServerurl) {
        try {
            String statusUrl = apiServerurl + "/status";//apiserver의 상태 표현 엔드포인트
            String status = restTemplate.getForObject(statusUrl, String.class);

            if ("connected".equals(status)) {
                System.out.println(apiServerurl + "와 연결되었습니다.");
            } else {
                System.out.println(apiServerurl + "가 아직 준비되지 않았습니다.");
            }
        } catch (Exception e) {
            System.out.println(apiServerurl + "에 연결할 수 없습니다." + e.getMessage());
        }
    }
}

