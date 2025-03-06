package distributed.loadbalancer.service;

import distributed.loadbalancer.repository.Servers.Server;
import distributed.loadbalancer.repository.Servers.ServerRepository;
import distributed.loadbalancer.repository.dto.RegistDto;
import distributed.loadbalancer.healthcheck.service.HealthCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class RegistrationService {

    private final ServerRepository serverRepository = ServerRepository.getInstance();
    private final HealthCheckService healthCheckService;

    public RegistrationService(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    // 서버 등록 (공통 로직)
    public boolean addServer(RegistDto registDto, String ip) {
        Server server = new Server(ip, registDto.getPort(), registDto.getProtocol());

        if (serverRepository.checkAllServers().contains(server)) {
            log.warn("Server already registered: {}", server);
            return false;
        }

        boolean added = serverRepository.addServer(server);
        if (added) {
            log.info("Server registered successfully: {}", server);
            triggerHealthCheck();
            return true;
        } else {
            log.warn("Failed to register server: {}", server);
            return false;
        }
    }

    // 서버 해제 (공통 로직)
    public boolean removeServer(RegistDto registDto, String ip) {
        Server server = new Server(ip, registDto.getPort(), registDto.getProtocol());

        boolean removed = serverRepository.removeServer(server);
        if (removed) {
            log.info("Server unregistered successfully: {}", server);
            return true;
        } else {
            log.warn("Failed to unregister server: {}", server);
            return false;
        }
    }

    // 등록된 서버 목록 반환
    public Set<Server> getAllServers() {
        return serverRepository.checkAllServers();
    }

    // 헬스 체크 트리거
    private void triggerHealthCheck() {
        if (serverRepository.checkAllServers().isEmpty()) {
            log.warn("No servers available for health check.");
            return;
        }

        log.info("Triggering health check.");
        try {
            healthCheckService.performHealthCheck();
        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}
