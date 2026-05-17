package at.ac.tuwien.inso.ctrl.auth.repository;

import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
  @Modifying
  @Query("DELETE FROM EmailVerification v WHERE v.expiryTime < CURRENT_TIMESTAMP")
  int deleteExpiredTokens();

  Optional<EmailVerification> findEmailVerificationByUser_Id(UUID userId);

  void deleteByUser_Id(UUID id);
}
