package zeco.suza.eoreporterv1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserRepository userRepository;
    
    @PostMapping("/fix-enabled-users")
    public ResponseEntity<String> fixEnabledUsers() {
        List<Users> allUsers = userRepository.findAll();
        int updatedCount = 0;
        
        for (Users user : allUsers) {
            if (user.getEnabled() == null || !user.getEnabled()) {
                user.setEnabled(true);
                userRepository.save(user);
                updatedCount++;
            }
        }
        
        return ResponseEntity.ok("Fixed " + updatedCount + " users. Total users: " + allUsers.size());
    }
    
    @GetMapping("/users-status")
    public ResponseEntity<List<Users>> getUsersStatus() {
        return ResponseEntity.ok(userRepository.findAll());
    }
} 