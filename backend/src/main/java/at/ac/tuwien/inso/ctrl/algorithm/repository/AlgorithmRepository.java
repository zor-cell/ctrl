package at.ac.tuwien.inso.ctrl.algorithm.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlgorithmRepository extends JpaRepository<AlgorithmConfig, UUID> {
  Optional<AlgorithmConfig> findByUserId(UUID userId);

  @Modifying
  @Query("DELETE FROM AlgorithmConfig c WHERE c.userId = :userId")
  int deleteByUserId(@Param("userId") UUID userId);
}
