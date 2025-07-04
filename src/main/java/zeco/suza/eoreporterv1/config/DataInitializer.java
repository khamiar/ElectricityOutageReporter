package zeco.suza.eoreporterv1.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import zeco.suza.eoreporterv1.model.Role;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.UserRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");
        fixEnabledUsers();
        createDefaultAdminUser();
        upgradeFirstUserToAdmin();
        logger.info("Data initialization completed.");
    }
    
    private void fixEnabledUsers() {
        List<Users> allUsers = userRepository.findAll();
        int updatedCount = 0;
        
        logger.info("Found {} users in database", allUsers.size());
        
        for (Users user : allUsers) {
            if (user.getEnabled() == null || !user.getEnabled()) {
                logger.info("Fixing enabled field for user: {}", user.getEmail());
                user.setEnabled(true);
                userRepository.save(user);
                updatedCount++;
            }
        }
        
        logger.info("Fixed enabled field for {} users", updatedCount);
    }
    
    private void createDefaultAdminUser() {
        String adminEmail = "admin@eoreporter.com";
        
        if (!userRepository.findByEmail(adminEmail).isPresent()) {
            logger.info("Creating default admin user...");
            
            Users adminUser = Users.builder()
                    .fullName("System Administrator")
                    .email(adminEmail)
                    .phoneNumber("0000000000")
                    .address("System")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .deleted(false)
                    .build();
            
            userRepository.save(adminUser);
            logger.info("Default admin user created with email: {} and password: admin123", adminEmail);
        } else {
            logger.info("Admin user already exists");
        }
    }

    private void upgradeFirstUserToAdmin() {
        // Check if there are any admin users
        List<Users> adminUsers = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .toList();
        
        if (adminUsers.isEmpty()) {
            // Find the first user (oldest by ID) and make them admin
            List<Users> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                Users firstUser = allUsers.get(0);
                logger.info("No admin users found. Upgrading first user to admin: {}", firstUser.getEmail());
                firstUser.setRole(Role.ADMIN);
                userRepository.save(firstUser);
                logger.info("User {} is now an admin", firstUser.getEmail());
            }
        } else {
            logger.info("Admin users already exist: {}", adminUsers.size());
        }
    }
} 