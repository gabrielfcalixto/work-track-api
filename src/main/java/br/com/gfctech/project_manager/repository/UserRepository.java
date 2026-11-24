package br.com.gfctech.project_manager.repository;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.gfctech.project_manager.entity.UserEntity;


public interface UserRepository  extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByLogin (String login);


	Optional<UserEntity> findByEmail(String email);

    @Query("select user.weeklyGoalHours from UserEntity user where user.id = :id")
    Optional<BigDecimal> findWeeklyGoalHours(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.email = :email")
    Optional<UserEntity> lockByEmail(@Param("email") String email);



}
