package distributed.primarystorage.Services;

import distributed.primarystorage.DTO.LocalStorageInfoDto;
import distributed.primarystorage.Repository.LocalStorageRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LocalStorageMonitorService {
    private final LocalStorageRepository localStorageRepository;

    public LocalStorageMonitorService(LocalStorageRepository localStorageRepository) {
        this.localStorageRepository = localStorageRepository;
    }

    // 모든 로컬 스토리지 정보를 출력하는 메서드
    public void printAllLocalStorageInfo() {
        Map<String, LocalStorageInfoDto> callInfoMap = localStorageRepository.getActiveLocalStorages();
        if (callInfoMap.isEmpty()) {
            System.out.println("등록된 로컬 스토리지가 없습니다.");
        } else {
            callInfoMap.forEach((key, info) -> {
                System.out.println("로컬 스토리지: " + key);
                System.out.println(" - 마지막 호출 시간: " + info.getLastCallTime());
            });
        }
    }

    // 주기적으로 로컬 스토리지 정보를 출력하는 스케줄러
    @Scheduled(fixedRate = 2000) // 1분마다 실행
    public void monitorLocalStorages() {
        System.out.println("로컬 스토리지 상태 모니터링...");
        printAllLocalStorageInfo();
    }
}
