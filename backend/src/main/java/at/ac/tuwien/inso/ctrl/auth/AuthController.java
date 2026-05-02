package at.ac.tuwien.inso.ctrl.auth;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.service.AuthService;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
import at.ac.tuwien.inso.ctrl.auth.service.ResetPasswordService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidTokenException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final ResetPasswordService passwordResetService;
  private final EmailVerificationService emailVerificationService;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void register(@Valid @RequestBody RegisterRequest registerRequest) {
    LOGGER.trace("Register request: {}", registerRequest);
    authService.register(registerRequest);
    // emailVerificationService.sendVerificationEmail(registerRequest);

  }

  @PostMapping("/verify")
  @ResponseStatus(HttpStatus.OK)
  public EmailVerificationResponse verify(
      @Valid @RequestBody EmailVerificationRequest emailVerificationRequest) {
    LOGGER.trace("Email Verification request: {}", emailVerificationRequest);
    return emailVerificationService.verifyEmail(emailVerificationRequest);
  }

  @PostMapping("/email")
  @ResponseStatus(HttpStatus.CREATED)
  public void resendVerificationEmail(@Valid @RequestBody SendEmailRequest sendEmailRequest) {
    LOGGER.trace("Resend verification email request: {}", sendEmailRequest);
    emailVerificationService.sendVerificationEmail(sendEmailRequest);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
    return authService.login(loginRequest);
  }

  /**
   * Sends a password reset email to the user with the specified email address.
   *
   * <p>This endpoint initiates the password reset process by sending a reset email to the user. The
   * email contains a link with a unique token that the user can use to reset their password. The
   * token is valid for 30 minutes.
   *
   * @param passwordResetRequest a valid request containing the user's email address
   * @return {@code true} if the password reset email was successfully sent
   * @throws ResourceNotFoundException if no user exists with the provided email address
   * @throws CommunicationException if the email fails to send or if there is an error creating the
   *     email message
   * @see PasswordResetEmailRequest
   * @see ResetPasswordService#sendPasswordResetEmail(PasswordResetEmailRequest)
   */
  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.CREATED)
  public boolean sendPasswordReset(
      @Valid @RequestBody PasswordResetEmailRequest passwordResetRequest) {
    LOGGER.trace("Password reset request: {}", passwordResetRequest);
    passwordResetService.sendPasswordResetEmail(passwordResetRequest);
    return true;
  }

  /**
   * Resets a user's password using a valid password reset token.
   *
   * <p>This endpoint completes the password reset process by validating the reset token and
   * updating the user's password. The token must be valid, not expired, and not yet used. After a
   * successful password reset, the token is marked as used and cannot be reused.
   *
   * @param passwordResetRequest a valid request containing the reset token and new password
   * @return {@code true} if the password was successfully reset
   * @throws InvalidTokenException if the token is not found, has already been used, or has expired
   * @see PasswordResetRequest
   * @see ResetPasswordService#resetPassword(PasswordResetRequest)
   */
  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.CREATED)
  public boolean resetPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
    passwordResetService.resetPassword(passwordResetRequest);
    return true;
  }
}
