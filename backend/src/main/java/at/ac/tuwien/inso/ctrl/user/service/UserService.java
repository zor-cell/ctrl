package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;

public interface UserService {

  /**
   * Loads the current authenticated user's details for the frontend.
   *
   * @param username the authenticated username
   * @return the current user's details response
   */
  UserDetailsResponse getCurrentUserDetails(String username);
}
