package br.com.gfctech.project_manager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "GFC_USER")
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDate joinDate = LocalDate.now();

    @Column(precision = 5, scale = 2)
    private BigDecimal weeklyGoalHours;

    @OneToMany(mappedBy = "user")
    private List<TimeEntryEntity> timeEntries;
    

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;
    
    // codigo para redefinir senha
    @Column(name = "reset_password_code")
    private String resetPasswordCode;

    @Column(name = "code_expiration_date")
    private LocalDateTime codeExpirationDate;

    @OneToMany(mappedBy = "manager")
    private List<ProjectEntity> managedProjects = new ArrayList<>();

    @ManyToMany(mappedBy = "teamMembers")
    private Set<ProjectEntity> projects = new HashSet<>();

    @ManyToMany(mappedBy = "assignedUsers")
    private Set<TaskEntity> tasks = new HashSet<>();



    // Enum de Role dentro da entidade
    public enum Role {
        ADMIN, MANAGER, USER
    }

    @Override
    public int hashCode() {
        return UserEntity.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof UserEntity other && id != null && id.equals(other.getId());
    }
}
