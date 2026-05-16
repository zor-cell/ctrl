package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerification;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessVerificationRepository extends JpaRepository<BusinessVerification, UUID> {
  boolean existsByBusinessUser(User currentUser);

  List<BusinessVerification> findAllByStatus(BusinessVerificationStatus status);

  BusinessVerification findByBusinessUser(User businessUser);
}
