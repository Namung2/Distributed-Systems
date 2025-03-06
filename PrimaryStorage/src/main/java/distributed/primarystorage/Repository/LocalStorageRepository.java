package distributed.primarystorage.Repository;

import distributed.primarystorage.DTO.LocalStorageInfoDto;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;



@Repository
public class LocalStorageRepository {

    // 현재 활성화된 로컬 스토리지 정보를 저장하는 ConcurrentHashMap
    private final Map<String, LocalStorageInfoDto> activeLocalStorages = new ConcurrentHashMap<>();

    // 현재 실행 중인 로컬 스토리지를 추가
    public void addLocalStorage(String ipAddress, int port) {
        String key = ipAddress + ":" + port;
        activeLocalStorages.putIfAbsent(key, new LocalStorageInfoDto(ipAddress, port));
    }

    // 현재 활성화된 로컬 스토리지 목록 반환
    public Map<String, LocalStorageInfoDto> getActiveLocalStorages() {
        return new ConcurrentHashMap<>(activeLocalStorages); // 복사본 반환
    }


    // 모든 활성화된 로컬 스토리지를 초기화 (모든 정보를 제거)
    public void clearRegisteredServers() {
        activeLocalStorages.clear();
    }
}
