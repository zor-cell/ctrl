package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record BlockedResponse (UUID id, String username, String profilePicture){
}
