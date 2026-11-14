
package com.example.backend_vitali.user;

import com.example.backend_vitali.auth.AuthDtos;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access
                    .AccessDeniedException(
                    "Debes iniciar sesión para consultar tu usuario."
            );
        }

        Long userId;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            try {
                userId = Long.valueOf(userDetails.getUsername());
            } catch (NumberFormatException exception) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "El usuario autenticado no tiene un identificador válido.",
                        exception
                );
            }
        } else {
            try {
                userId = Long.valueOf(authentication.getName());
            } catch (NumberFormatException exception) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "El usuario autenticado no tiene un identificador válido.",
                        exception
                );
            }
        }

        AppUser user = userService.getById(userId);

        return new AuthDtos.UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}
