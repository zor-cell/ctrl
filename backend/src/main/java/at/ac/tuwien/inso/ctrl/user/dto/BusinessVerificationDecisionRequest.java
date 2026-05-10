package at.ac.tuwien.inso.ctrl.user.dto;

import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;

public record BusinessVerificationDecisionRequest(
    BusinessVerificationStatus status
) {
}
