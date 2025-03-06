package distributed.loadbalancer.repository.Servers;

import lombok.Data;
import java.util.Objects;

@Data
public class Server {
    private String ip;
    private int port;
    private String protocol;

    public Server(String ip, int port, String protocol) {
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
    }

    // equals와 hashCode 오버라이드 - 서버 객체 비교를 위해 필수
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return port == server.port &&
                Objects.equals(ip, server.ip) &&
                Objects.equals(protocol, server.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, protocol);
    }
}
