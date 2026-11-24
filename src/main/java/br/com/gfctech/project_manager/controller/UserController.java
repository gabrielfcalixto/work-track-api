package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.ChangePasswordRequest;
import br.com.gfctech.project_manager.dto.UserDTO;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import br.com.gfctech.project_manager.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserDTO> getAllUsers() {
        return users.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        return users.getUserById(id);
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public void addUser(@Valid @RequestBody UserDTO request) {
        users.addUser(request);
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(@AuthenticationPrincipal UserDetailsImpl actor, @PathVariable Long id,
                              @Valid @RequestBody UserDTO request) {
        return users.updateUser(actor.getId(), id, request);
    }

    @PatchMapping("/permissions/{id}")
    public UserDTO updatePermissions(@AuthenticationPrincipal UserDetailsImpl actor, @PathVariable Long id,
                                     @RequestBody Map<String, String> request) {
        return users.updatePermissions(actor.getId(), id, request.get("role"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@AuthenticationPrincipal UserDetailsImpl actor, @PathVariable Long id) {
        users.deleteUser(actor.getId(), id);
    }

    @PutMapping("/{id}/change-password")
    @PreAuthorize("#id == principal.id")
    public Map<String, String> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(id, request.oldPassword(), request.newPassword());
        return Map.of("message", "Senha alterada com sucesso.");
    }
}
