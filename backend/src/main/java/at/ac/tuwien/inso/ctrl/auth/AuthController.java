package at.ac.tuwien.inso.ctrl.auth;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public void register(@RequestBody AuthRequest authRequest) {
        var user = User.builder()
                .username(authRequest.getUsername())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .build();

        userRepository.save(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getUsername(),
                        authRequest.getPassword()
                )
        );

        var user = userRepository.findByUsername(authRequest.getUsername())
                .orElseThrow();

        var token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }
}
