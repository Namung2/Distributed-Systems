package distributed.primarystorage.Services;

import distributed.primarystorage.DTO.LocalStorageInfoDto;
import distributed.primarystorage.DTO.NoteDto;
import distributed.primarystorage.Repository.LocalStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SynService {
    private final RestTemplate restTemplate;
    private final UDPSynService udpSynService;
    private final LocalStorageRepository localStorageRepository;

    @Autowired
    public SynService(UDPSynService udpSynService,RestTemplate restTemplate, LocalStorageRepository localStorageRepository) {
        this.restTemplate = restTemplate;
        this.udpSynService = udpSynService;
        this.localStorageRepository = localStorageRepository;
    }

    // 현재 살아있는 로컬 스토리지를 LocalStorageRepository에 저장
    public void updateActiveLocalStorages() {

        String[] localStorageUrls = {
                "http://localhost:7001",//APIstorage
                "http://localhost:7002",//TCPstorage
        };

        // LocalStorageRepository를 초기화
        localStorageRepository.clearRegisteredServers();
        udpSynService.checkUdpStorageStatus();
        for (String baseUrl : localStorageUrls) {
            String statusUrl = baseUrl + "/connect/status";
            try {
                System.out.println("Sending GET request to: " + statusUrl);
                ResponseEntity<String> response = restTemplate.getForEntity(statusUrl, String.class);
                System.out.println("[DEBUG] /connect/status 응답 수신: " + response);
                String responseBody = response.getBody() != null ? response.getBody().trim() : "";
                System.out.println("Response from " + statusUrl + ": " + responseBody);

                if ("READY".equals(responseBody)) {
                    String ipAddress = baseUrl.split("//")[1].split(":")[0];
                    int port = Integer.parseInt(baseUrl.split(":")[2]);
                    localStorageRepository.addLocalStorage(ipAddress, port);//활성화된 로컬스토리지를 넣어
                    System.out.println("로컬 스토리지 서버가 준비되었습니다: " + ipAddress + ":" + port);
                }
            } catch (Exception e) {
                System.err.println("Error while sending GET request to " + statusUrl + ": " + e.getMessage());
            }
        }
    }

    // 각 로컬 스토리지에 POST 요청을 보내는 메서드
    public void synWithLocalStoragesForPost(NoteDto note) {
        updateActiveLocalStorages(); // 현재 활성화된 로컬 스토리지 갱신
        if (localStorageRepository.getActiveLocalStorages().containsKey("localhost:7003")) {
            try {
                udpSynService.sendPostToUdp(note);
                System.out.println("[DEBUG] UDP 스토리지 노트 추가 동기화 성공: " );
            } catch (Exception e) {
                System.err.println("[ERROR] UDP 스토리지 추가 동기화 실패: " + e.getMessage());
            }
        } else {
            System.out.println("[WARN] UDP 스토리지가 활성화되지 않아 동기화 요청을 보낼 수 없습니다.");
        }
        if (localStorageRepository.getActiveLocalStorages().isEmpty()) {
            System.out.println("[WARN] 활성화된 API,TCP 로컬 스토리지가 없어 동기화 요청을 보낼 수 없습니다.");
            return; // 동기화 중단
        }
        for (LocalStorageInfoDto storageInfo : localStorageRepository.getActiveLocalStorages().values()) {
            if (storageInfo.getPort() == 7003) {
                continue; // HTTP 요청에서 제외
            }

            String url = "http://" + storageInfo.getIpAddress() + ":" + storageInfo.getPort() + "/backup";
            try {
                restTemplate.postForObject(url, note, String.class);
                System.out.println("[DEBUG] 동기화 성공: " + url);
            } catch (Exception e) {
                System.err.println("[ERROR] 로컬 스토리지 서버에 POST 요청 실패: " + url);
            }
        }
    }


    // PUT, PATCH, DELETE 요청을 보내는 메서드
    public void synWithLocalStoragesForUpdate(NoteDto note, String method) {
        updateActiveLocalStorages();  // 먼저 살아있는 로컬 스토리지를 업데이트
        // UDP 스토리지가 활성 상태인지 확인
        if (localStorageRepository.getActiveLocalStorages().containsKey("localhost:7003")) {
            try {
                udpSynService.sendUpdateToUdp(note, method);
                System.out.println("[DEBUG] UDP 스토리지 업데이트 동기화 성공: " + method);
            } catch (Exception e) {
                System.err.println("[ERROR] UDP 스토리지 업데이트 동기화 실패: " + e.getMessage());
            }
        } else {
            System.out.println("[WARN] UDP 스토리지가 활성화되지 않아 동기화 요청을 보낼 수 없습니다.");
        }

        if (localStorageRepository.getActiveLocalStorages().isEmpty()) {
            System.out.println("활성화된 로컬 스토리지가 없어 동기화 요청을 보낼 수 없습니다.");
            return; // 동기화 중단
        }

        for (LocalStorageInfoDto storageInfo : localStorageRepository.getActiveLocalStorages().values()) {
            if (storageInfo.getPort() == 7003) {
                continue; // HTTP 요청에서 제외
            }
            String endpoint = "http://" + storageInfo.getIpAddress() + ":" + storageInfo.getPort() + "/backup/" + note.getId();

            try {
                switch (method) {
                    case "PUT":
                        restTemplate.put(endpoint, note);
                        break;
                    case "PATCH":
                        restTemplate.patchForObject(endpoint, note, String.class);
                        break;
                }
                System.out.println("동기화 성공: " + endpoint);
            } catch (Exception e) {
                System.out.println("로컬 스토리지 서버에 " + method + " 요청 실패: " + endpoint);
            }
        }
    }

    public void synWithLocalStoragesForDelete(int id) {
        updateActiveLocalStorages();  // 먼저 살아있는 로컬 스토리지를 업데이트
        if (localStorageRepository.getActiveLocalStorages().containsKey("localhost:7003")) {
            try {
                udpSynService.sendDeleteToUdp(id);
                System.out.println("[DEBUG] UDP 스토리지 삭제 동기화 성공: " + id);
            } catch (Exception e) {
                System.err.println("[ERROR] UDP 스토리지 삭제 동기화 실패: " + e.getMessage());
            }
        } else {
            System.out.println("[WARN] UDP 스토리지가 활성화되지 않아 동기화 요청을 보낼 수 없습니다.");
        }


        if (localStorageRepository.getActiveLocalStorages().isEmpty()) {
            System.out.println("활성화된 로컬 스토리지가 없어 동기화 요청을 보낼 수 없습니다.");
            return; // 동기화 중단
        }

        for (LocalStorageInfoDto storageInfo : localStorageRepository.getActiveLocalStorages().values()) {
            if (storageInfo.getPort() == 7003) {
                continue; // HTTP 요청에서 제외
            }
            String url = "http://" + storageInfo.getIpAddress() + ":" + storageInfo.getPort() + "/backup/" + id;

            try {
                restTemplate.delete(url);
                System.out.println("삭제 요청 성공: " + url);
            } catch (Exception e) {
                System.out.println("로컬 스토리지 서버에 DELETE 요청 실패: " + url);
            }
        }
    }
}
