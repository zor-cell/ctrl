package at.ac.tuwien.inso.ctrl.auth.repository;

import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

  Optional<EmailVerification> findEmailVerificationByUser_Id(UUID userId);
}
