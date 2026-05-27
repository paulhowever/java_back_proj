package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.UserRequest;
import ru.tischenko.vk.api.dto.Dtos.UserResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.UserMapper;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "409", description = "User conflict")
    public UserResponse createUser(@RequestBody @Valid UserRequest req) {
        return userMapper.toResponse(userService.createUser(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public UserResponse getUser(@PathVariable Long id) {
        return userMapper.toResponse(userService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody @Valid UserRequest req) {
        return userMapper.toResponse(userService.updateUser(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users with filters (role, level, enabled)")
    public Page<UserResponse> listUsers(@RequestParam(required = false) Role role,
                                        @RequestParam(required = false) UserLevel level,
                                        @RequestParam(required = false) Boolean enabled,
                                        Pageable pageable) {
        return userService.listUsers(role, level, enabled, pageable).map(userMapper::toResponse);
    }
}
