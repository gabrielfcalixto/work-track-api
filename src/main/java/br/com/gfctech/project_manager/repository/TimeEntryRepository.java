package br.com.gfctech.project_manager.repository;

import br.com.gfctech.project_manager.entity.TimeEntryEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeEntryRepository extends JpaRepository<TimeEntryEntity, Long> {
    @EntityGraph(attributePaths = "taskEntity")
    List<TimeEntryEntity> findByUserIdOrderByEntryDateDescStartTimeDescIdDesc(Long userId);

    Optional<TimeEntryEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByTaskEntityId(Long taskId);
    boolean existsByUserId(Long userId);

    @Query("""
            select count(entry) > 0 from TimeEntryEntity entry
            where entry.user.id = :userId and entry.entryDate = :date
              and entry.startTime < :endTime and entry.endTime > :startTime
              and (:entryId is null or entry.id <> :entryId)
            """)
    boolean overlaps(@Param("userId") Long userId, @Param("date") LocalDate date,
                     @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
                     @Param("entryId") Long entryId);

    @Query("select coalesce(sum(entry.totalHours), 0.0) from TimeEntryEntity entry where entry.user.id = :userId")
    double sumHoursByUser(@Param("userId") Long userId);

    @Query("""
            select coalesce(sum(entry.totalHours), 0.0) from TimeEntryEntity entry
            where entry.user.id = :userId and entry.entryDate >= :start and entry.entryDate < :end
            """)
    double sumHoursInPeriod(@Param("userId") Long userId, @Param("start") LocalDate start,
                            @Param("end") LocalDate end);

    @Query("""
            select entry.entryDate as entryDate, coalesce(sum(entry.totalHours), 0.0) as loggedHours
            from TimeEntryEntity entry
            where entry.user.id = :userId and entry.entryDate >= :start and entry.entryDate < :end
            group by entry.entryDate
            """)
    List<DailyHours> sumHoursByDate(@Param("userId") Long userId, @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("select coalesce(sum(entry.totalHours), 0.0) from TimeEntryEntity entry where entry.taskEntity.id = :taskId")
    double sumHoursByTask(@Param("taskId") Long taskId);

    interface DailyHours {
        LocalDate getEntryDate();
        double getLoggedHours();
    }
}
