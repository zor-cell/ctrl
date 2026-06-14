package at.ac.tuwien.inso.ctrl.auth.repository;

import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

  @Modifying
  @Query("DELETE FROM PasswordReset t WHERE t.user.id = :userId AND t.used = false")
  void deleteUnusedTokensByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM PasswordReset t WHERE t.user.id = :userId")
  void deleteByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM PasswordReset t WHERE t.expiresAt < CURRENT_TIMESTAMP")
  int deleteExpiredTokens();

  Optional<PasswordReset> findByToken(String rawToken);

  Optional<PasswordReset> findPasswordResetByUser(User user);
}
