package distributed.primarystorage.Config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // RequestConfig를 사용하여 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))    // 연결 타임아웃 (5초)
                .setResponseTimeout(Timeout.ofSeconds(10))  // 읽기 타임아웃 (5초)
                .build();

        // HTTP 연결 풀 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(10); // 라우트당 최대 연결 수
        connectionManager.setMaxTotal(50);           // 전체 최대 연결 수
        connectionManager.setValidateAfterInactivity(TimeValue.ofSeconds(5)); // 비활성 연결 검증

        // HTTP 클라이언트 생성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build(); // HTTP/1.1은 기본값

        // RestTemplate에 HTTP 클라이언트 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);

        // 요청/응답 로깅 인터셉터 추가
        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                logRequestDetails(request, body);
                ClientHttpResponse response = execution.execute(request, body);
                logResponseDetails(response);
                return response;
            }
        });

        return restTemplate;
    }

    private void logRequestDetails(HttpRequest request, byte[] body) {
        System.out.println("=========================== Request Begin ===========================");
        System.out.println("URI         : " + request.getURI());
        System.out.println("Method      : " + request.getMethod());
        System.out.println("Headers     : " + request.getHeaders());
        System.out.println("Request Body: " + new String(body));
        System.out.println("============================ Request End ============================");
    }

    private void logResponseDetails(ClientHttpResponse response) throws IOException {
        System.out.println("=========================== Response Begin ===========================");
        System.out.println("Status code  : " + response.getStatusCode());
        System.out.println("Headers      : " + response.getHeaders());
        System.out.println("============================ Response End ============================");
    }
}
