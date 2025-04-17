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
public class RegisterRequest {

    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String password;
    private Role role;
}
