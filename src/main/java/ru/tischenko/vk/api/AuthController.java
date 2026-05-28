package ru.tischenko.vk.api;

import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.*;
import ru.tischenko.vk.domain.Enums;
import ru.tischenko.vk.security.JwtService;
import ru.tischenko.vk.security.UserDetailsServiceImpl;
import ru.tischenko.vk.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl uds;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, UserDetailsServiceImpl uds, JwtService jwtService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.uds = uds;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody @Valid RegisterRequest req) {
        var user = userService.createUser(new UserRequest(req.email(), req.password(), Enums.Role.USER, req.level()));
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getLevel());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        return new AuthResponse(jwtService.generate(uds.loadUserByUsername(req.email())));
    }
}
