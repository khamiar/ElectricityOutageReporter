package zeco.suza.eoreporterv1.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zeco.suza.eoreporterv1.config.JwtService;
import zeco.suza.eoreporterv1.model.Role;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zeco.suza.eoreporterv1.model.OtpToken;
import zeco.suza.eoreporterv1.repository.OtpTokenRepository;
import zeco.suza.eoreporterv1.dto.PasswordResetRequest;
import zeco.suza.eoreporterv1.dto.OtpVerifyRequest;
import zeco.suza.eoreporterv1.dto.PasswordChangeRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import zeco.suza.eoreporterv1.service.EmailService;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import zeco.suza.eoreporterv1.dto.MessageResponse;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationResponse register(RegisterRequest registerRequest) {
        logger.info("Attempting to register user with email: {}", registerRequest.getEmail());
        var user = Users.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .phoneNumber(registerRequest.getPhoneNumber())
                .address(registerRequest.getAddress())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .enabled(true)
                .deleted(false)
                .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        logger.info("User registered successfully: {}", registerRequest.getEmail());
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .id(user.getId())
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        logger.info("Attempting to authenticate user: {}", authenticationRequest.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getEmail(),
                            authenticationRequest.getPassword()
                    )
            );
            var user = userRepository.findByEmail(authenticationRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));
            var jwtToken = jwtService.generateToken(user);
            logger.info("User authenticated successfully: {}", authenticationRequest.getEmail());
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .role(user.getRole())
                    .id(user.getId())
                    .build();
        } catch (BadCredentialsException e) {
            logger.error("Authentication failed for user: {}", authenticationRequest.getEmail(), e);
            throw new RuntimeException("Invalid email or password");
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for user: {}", authenticationRequest.getEmail(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> requestPasswordReset(PasswordResetRequest request) {
        Optional<Users> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        // Generate OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        Instant expiresAt = Instant.now().plusSeconds(600); // 10 min

        // Remove any existing OTP for this email
        otpTokenRepository.deleteByEmail(request.getEmail());

        // Save new OTP
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(request.getEmail());
        otpToken.setOtp(otp);
        otpToken.setExpiresAt(expiresAt);
        otpTokenRepository.save(otpToken);

        // Send OTP to user's email
        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to send OTP email.");
        }

        return ResponseEntity.ok(new MessageResponse("OTP sent to email"));
    }

    public ResponseEntity<?> verifyOtp(OtpVerifyRequest request) {
        Optional<OtpToken> otpOpt = otpTokenRepository.findByEmailAndOtp(request.getEmail(), request.getOtp());
        if (otpOpt.isEmpty() || Instant.now().isAfter(otpOpt.get().getExpiresAt())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP"));
        }
        return ResponseEntity.ok(new MessageResponse("OTP verified"));
    }

    public ResponseEntity<?> resetPassword(PasswordChangeRequest request) {
        Optional<OtpToken> otpOpt = otpTokenRepository.findByEmailAndOtp(request.getEmail(), request.getOtp());
        if (otpOpt.isEmpty() || Instant.now().isAfter(otpOpt.get().getExpiresAt())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid or expired OTP"));
        }
        Optional<Users> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("User not found"));
        }
        Users user = userOpt.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpTokenRepository.deleteByEmail(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
}
