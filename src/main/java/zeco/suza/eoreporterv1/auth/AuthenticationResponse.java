package zeco.suza.eoreporterv1.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import zeco.suza.eoreporterv1.model.Role;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    private String token;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private Role role;
    private Long id;
}
