package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.user.dto.BusinessVerificationDecisionRequest;
import at.ac.tuwien.inso.ctrl.user.dto.BusinessVerificationResponse;
import at.ac.tuwien.inso.ctrl.user.service.BusinessVerificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final BusinessVerificationService businessVerificationService;

  @GetMapping("/verifications/pending")
  public List<BusinessVerificationResponse> getPendingRequests() {
    return businessVerificationService.getAllPendingVerifications();
  }

  @PutMapping("/verifications/{requestId}/decision")
  public void reviewRequest(
      @PathVariable UUID requestId,
      @RequestBody BusinessVerificationDecisionRequest verificationRequest) {
    businessVerificationService.updateVerification(requestId, verificationRequest);
  }
}
