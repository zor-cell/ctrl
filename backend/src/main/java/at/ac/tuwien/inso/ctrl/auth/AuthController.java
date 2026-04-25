package at.ac.tuwien.inso.ctrl.auth;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.service.AuthService;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
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
}
