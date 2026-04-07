package at.ac.tuwien.inso.ctrl.algorithm.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String> {
  boolean existsByName(String name);
}
