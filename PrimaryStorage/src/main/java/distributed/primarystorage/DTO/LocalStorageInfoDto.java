package distributed.primarystorage.DTO;

import lombok.Setter;

import java.time.LocalDateTime;

public class LocalStorageInfoDto {
    @Setter
    private String ipAddress;
    @Setter
    private int port;
    private LocalDateTime lastCallTime; //로컬 스토리지가 호출한 시간을 저장

    public LocalStorageInfoDto(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastCallTime = LocalDateTime.now(); //호출 시간 설정
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public LocalDateTime getLastCallTime() {
        return lastCallTime;
    }

    public void updateLastCallTime() {
        this.lastCallTime = LocalDateTime.now();
    }
}
