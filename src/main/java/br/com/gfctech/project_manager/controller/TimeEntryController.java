package br.com.gfctech.project_manager.controller;

import br.com.gfctech.project_manager.dto.TimeEntryDTO;
import br.com.gfctech.project_manager.dto.TimeEntryRequest;
import br.com.gfctech.project_manager.service.TimeEntryService;
import br.com.gfctech.project_manager.service.UserDetailsImpl;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/time-entries")
public class TimeEntryController {
    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<TimeEntryDTO> listMine(@AuthenticationPrincipal UserDetailsImpl user) {
        return service.getTimeEntriesByUserId(user.getId());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("#userId == principal.id or hasAuthority('ADMIN')")
    public List<TimeEntryDTO> listByUser(@PathVariable Long userId) {
        return service.getTimeEntriesByUserId(userId);
    }

    @PostMapping({"", "/add"})
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryDTO create(@AuthenticationPrincipal UserDetailsImpl user, @Valid @RequestBody TimeEntryRequest request) {
        return service.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    public TimeEntryDTO update(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable Long id,
                               @Valid @RequestBody TimeEntryRequest request) {
        return service.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable Long id) {
        service.delete(user.getId(), id);
    }
}
