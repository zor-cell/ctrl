package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerification;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessVerificationRepository extends JpaRepository<BusinessVerification, UUID> {
  boolean existsByBusinessUser(User currentUser);

  List<BusinessVerification> findAllByStatus(BusinessVerificationStatus status);

  BusinessVerification findByBusinessUser(User businessUser);

  @Modifying
  @Query("DELETE FROM BusinessVerification v WHERE v.businessUser = :businessUser")
  int deleteByBusinessUser(@Param("businessUser") User businessUser);
}
