package zeco.suza.eoreporterv1.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.UserRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");
        fixEnabledUsers();
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
} 