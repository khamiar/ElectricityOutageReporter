package zeco.suza.eoreporterv1.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String email;
    private String otp;
    private String newPassword;
} 