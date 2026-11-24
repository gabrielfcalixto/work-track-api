package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.ProfileDTO;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    public ProfileDTO getProfile(Long id) {
        var user = users.findById(id).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        return new ProfileDTO(user.getId(), user.getName(), user.getLogin(), user.getEmail(),
                user.getRole().name(), user.getJoinDate());
    }
}
