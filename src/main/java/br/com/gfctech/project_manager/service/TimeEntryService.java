package br.com.gfctech.project_manager.service;

import br.com.gfctech.project_manager.dto.TimeEntryDTO;
import br.com.gfctech.project_manager.dto.TimeEntryRequest;
import br.com.gfctech.project_manager.entity.TaskEntity;
import br.com.gfctech.project_manager.entity.TimeEntryEntity;
import br.com.gfctech.project_manager.exceptions.ApiException;
import br.com.gfctech.project_manager.repository.TaskRepository;
import br.com.gfctech.project_manager.repository.TimeEntryRepository;
import br.com.gfctech.project_manager.repository.UserRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TimeEntryService {
    private final TimeEntryRepository entries;
    private final TaskRepository tasks;
    private final UserRepository users;

    public TimeEntryService(TimeEntryRepository entries, TaskRepository tasks, UserRepository users) {
        this.entries = entries;
        this.tasks = tasks;
        this.users = users;
    }

    public List<TimeEntryDTO> getTimeEntriesByUserId(Long userId) {
        return entries.findByUserIdOrderByEntryDateDescStartTimeDescIdDesc(userId)
                .stream().map(TimeEntryDTO::new).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TimeEntryDTO create(Long userId, TimeEntryRequest request) {
        checkOwner(userId, request);
        if (request.taskId() == null) {
            throw new IllegalArgumentException("Selecione uma tarefa.");
        }
        var user = users.lockById(userId).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        var task = lockTask(request.taskId());
        if (!tasks.existsByIdAndAssignedUsersId(task.getId(), userId)) {
            throw new AccessDeniedException("A tarefa precisa estar atribuída a você.");
        }
        var entry = new TimeEntryEntity();
        entry.setUser(user);
        entry.setTaskEntity(task);
        return save(entry, task, request);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TimeEntryDTO update(Long userId, Long entryId, TimeEntryRequest request) {
        checkOwner(userId, request);
        var entry = findOwnedEntryForWrite(userId, entryId);
        var task = lockTask(entry.getTaskEntity().getId());
        if (request.taskId() != null && !Objects.equals(request.taskId(), task.getId())) {
            throw new IllegalArgumentException("A tarefa de um lançamento existente não pode ser alterada.");
        }
        return save(entry, task, request);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long userId, Long entryId) {
        var entry = findOwnedEntryForWrite(userId, entryId);
        var task = lockTask(entry.getTaskEntity().getId());
        entries.delete(entry);
        entries.flush();
        task.setTotalHours(entries.sumHoursByTask(task.getId()));
    }

    private TimeEntryDTO save(TimeEntryEntity entry, TaskEntity task, TimeEntryRequest request) {
        var start = request.startTime().truncatedTo(ChronoUnit.MINUTES);
        var end = request.endTime().truncatedTo(ChronoUnit.MINUTES);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("O horário final deve ser posterior ao inicial.");
        }
        if (entries.overlaps(entry.getUser().getId(), request.entryDate(), start, end, entry.getId())) {
            throw ApiException.conflict("Já existe um lançamento nesse intervalo.");
        }
        entry.setDescription(request.description().strip());
        entry.setEntryDate(request.entryDate());
        entry.setStartTime(start);
        entry.setEndTime(end);
        var saved = entries.saveAndFlush(entry);
        task.setTotalHours(entries.sumHoursByTask(task.getId()));
        return new TimeEntryDTO(saved);
    }

    private TimeEntryEntity findOwnedEntryForWrite(Long userId, Long entryId) {
        users.lockById(userId).orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
        return entries.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ApiException.notFound("Lançamento não encontrado."));
    }

    private TaskEntity lockTask(Long taskId) {
        return tasks.lockById(taskId).orElseThrow(() -> ApiException.notFound("Tarefa não encontrada."));
    }

    private static void checkOwner(Long userId, TimeEntryRequest request) {
        if (request.userId() != null && !Objects.equals(userId, request.userId())) {
            throw new AccessDeniedException("Acesso negado.");
        }
    }
}
