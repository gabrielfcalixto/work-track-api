package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.entity.UserEntity;
import br.com.gfctech.project_manager.entity.UserEntity.Role;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailsImpl implements UserDetails {
    private static final DateTimeFormatter JOIN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final Long id;
    private final String name;
    private final String login;
    private final String email;
    private final String password;
    private final Role role;
    private final LocalDate joinDate;

    public UserDetailsImpl(UserEntity user) {
        id = user.getId();
        name = user.getName();
        login = user.getLogin();
        email = user.getEmail();
        password = user.getPassword();
        role = user.getRole();
        joinDate = user.getJoinDate();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getJoinDate() {
        return joinDate == null ? null : joinDate.format(JOIN_DATE_FORMAT);
    }
}
