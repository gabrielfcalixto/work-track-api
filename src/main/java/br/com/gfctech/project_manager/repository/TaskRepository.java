package br.com.gfctech.project_manager.repository;

import br.com.gfctech.project_manager.entity.TaskEntity;
import br.com.gfctech.project_manager.enums.TaskStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    @EntityGraph(attributePaths = "assignedUsers")
    List<TaskEntity> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "assignedUsers")
    @Query("""
            select task from TaskEntity task
            where exists (select member.id from task.assignedUsers member where member.id = :userId)
            order by task.id desc
            """)
    List<TaskEntity> findByUserId(@Param("userId") Long userId);

    boolean existsByIdAndAssignedUsersId(Long id, Long userId);
    boolean existsByAssignedUsersId(Long userId);
    boolean existsByProjectId(Long projectId);
    boolean existsByProjectIdAndStatusNot(Long projectId, TaskStatus status);
    long countByProjectManagerId(Long managerId);

    @Query("select task.project.id from TaskEntity task where task.id = :id")
    Optional<Long> findProjectId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from TaskEntity task where task.id = :id")
    Optional<TaskEntity> lockById(@Param("id") Long id);

    @Query("""
            select task.status, count(task) from TaskEntity task join task.assignedUsers member
            where member.id = :userId group by task.status
            """)
    List<Object[]> countTasksByStatus(@Param("userId") Long userId);
}
