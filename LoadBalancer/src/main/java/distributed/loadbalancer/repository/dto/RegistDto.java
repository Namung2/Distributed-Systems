package distributed.loadbalancer.repository.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
// Dto form으로 만들어서 객체화
public class RegistDto {
    private String cmd;
    private int port;
    private String protocol;
}

