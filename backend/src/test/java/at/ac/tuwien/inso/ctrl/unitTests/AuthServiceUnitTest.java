package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.service.AuthService;
import at.ac.tuwien.inso.ctrl.exception.EmailNotVerifiedException;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        //set the default value for the @Value field since Spring isn't running to inject it
        ReflectionTestUtils.setField(authService, "requireEmailVerification", true);

        testUser = User.builder()
                .username("testUser")
                .password("encodedPassword")
                .name("Test User")
                .email("test@domain.com")
                .birthdate(LocalDate.of(1990, 1, 1))
                .emailVerified(true)
                .build();

        validRegisterRequest = new RegisterRequest(
                "newUser",
                "rawPassword",
                "new@domain.com",
                "New User",
                LocalDate.of(1995, 5, 5)
        );

        validLoginRequest = new LoginRequest("testUser", "rawPassword");
    }

    @Test
    void register_ShouldSaveUser_WhenRequestIsValid() {
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@domain.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");

        authService.register(validRegisterRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("newUser", savedUser.getUsername());
        assertEquals("new@domain.com", savedUser.getEmail());
        assertEquals("hashedPassword", savedUser.getPassword());
        assertFalse(savedUser.isEmailVerified());
    }

    @Test
    void register_ShouldSetEmailVerifiedToTrue_WhenVerificationIsNotRequired() {
        ReflectionTestUtils.setField(authService, "requireEmailVerification", false);

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@domain.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");

        authService.register(validRegisterRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertTrue(userCaptor.getValue().isEmailVerified());
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        when(userRepository.existsByUsername("newUser")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> authService.register(validRegisterRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@domain.com")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> authService.register(validRegisterRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(validLoginRequest);

        assertEquals("valid.jwt.token", response.token());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ShouldThrowException_WhenBadCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidPasswordException.class, () -> authService.login(validLoginRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenAccountIsDisabled() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Disabled"));

        assertThrows(InvalidPasswordException.class, () -> authService.login(validLoginRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenAccountIsLocked() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("Locked"));

        assertThrows(InvalidPasswordException.class, () -> authService.login(validLoginRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundInDatabase() {
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(validLoginRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenEmailNotVerifiedAndVerificationIsRequired() {
        testUser.setEmailVerified(false);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(validLoginRequest));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldReturnToken_WhenEmailNotVerifiedButVerificationIsNotRequired() {
        ReflectionTestUtils.setField(authService, "requireEmailVerification", false);
        testUser.setEmailVerified(false);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(validLoginRequest);

        assertEquals("valid.jwt.token", response.token());
    }
}