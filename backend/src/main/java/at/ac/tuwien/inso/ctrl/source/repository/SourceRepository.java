package at.ac.tuwien.inso.ctrl.source.repository;

import at.ac.tuwien.inso.ctrl.source.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, String> {}
