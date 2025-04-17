package zeco.suza.eoreporterv1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zeco.suza.eoreporterv1.model.Users;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Integer> {


    Optional<Users> findByEmail(String email);
}
