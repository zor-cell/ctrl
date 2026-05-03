package at.ac.tuwien.inso.ctrl.algorithm.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlgorithmRepository  extends JpaRepository<AlgorithmConfig, UUID> {
    Optional<AlgorithmConfig> findByUserId(UUID userId);
}
