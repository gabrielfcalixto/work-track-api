package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.ProfileDTO;
import br.com.gfctech.project_manager.service.ProfileService;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/me")
    public ProfileDTO getMyProfile(@AuthenticationPrincipal UserDetailsImpl user) {
        return profiles.getProfile(user.getId());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public ProfileDTO getProfile(@PathVariable Long userId) {
        return profiles.getProfile(userId);
    }
}
