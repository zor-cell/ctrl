package at.ac.tuwien.inso.ctrl.source.repository;

import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SingleSourceRepository extends JpaRepository<SingleSource, String> {}
