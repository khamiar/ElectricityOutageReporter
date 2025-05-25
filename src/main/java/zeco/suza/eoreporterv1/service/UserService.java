package zeco.suza.eoreporterv1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zeco.suza.eoreporterv1.model.Users;
import zeco.suza.eoreporterv1.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository usersRepository;

    public List<Users> getAllUsers() {
        return usersRepository.findByDeletedFalse();
    }

    public List<Users> getActiveUsers() {
        return usersRepository.findByDeletedFalse();
    }

    public Optional<Users> getUserById(Long id) {
        return usersRepository.findById(id);
    }

    public Users updateUser(Long id, Users updatedUser) {
        return usersRepository.findById(id)
                .map(user -> {
                    user.setFullName(updatedUser.getFullName());
                    user.setPhoneNumber(updatedUser.getPhoneNumber());
                    user.setAddress(updatedUser.getAddress());
                    // Avoid updating email/password unless explicitly allowed
                    return usersRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        usersRepository.findById(id)
            .ifPresent(user -> {
                user.setDeleted(true);
                usersRepository.save(user);
            });
    }
}
