package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {
    private final UserRepository users;

    public UserDetailServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetailsImpl loadUserByUsername(String login) {
        return users.findByLogin(login).map(UserDetailsImpl::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
