package br.com.gfctech.project_manager.repository;

import br.com.gfctech.project_manager.entity.ProjectEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    @EntityGraph(attributePaths = {"manager", "client", "teamMembers"})
    List<ProjectEntity> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from ProjectEntity project where project.id = :id")
    Optional<ProjectEntity> lockById(@Param("id") Long id);

    long countByManagerId(Long managerId);
    boolean existsByManagerIdOrTeamMembersId(Long managerId, Long teamMemberId);
}
