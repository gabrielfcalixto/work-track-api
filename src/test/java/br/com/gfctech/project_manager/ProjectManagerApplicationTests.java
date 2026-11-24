package br.com.gfctech.project_manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ProjectManagerApplicationTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void dashboardShowsPersonalCountsAndCompletedTasks() throws Exception {
        String token = login("dev");
        mvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedTasksCount").value(1))
                .andExpect(jsonPath("$.taskDistribution.CONCLUIDA").value(1))
                .andExpect(jsonPath("$.totalHoursMonth").isNumber());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/time-entries/me']").exists());
    }

    @Test
    void dashboardGroupsOnlyCurrentWeekHoursAndIncludesDaysWithoutEntries() throws Exception {
        String member = login("dev");
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long task = firstAssignedTask(member);
        var older = mvc.perform(post("/time-entries").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("taskId", task, "description", "Semana anterior",
                                "entryDate", monday.minusDays(1), "startTime", "08:00", "endTime", "09:00"))))
                .andExpect(status().isCreated()).andReturn();
        long olderId = json.readTree(older.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyHours.weekStart").value(monday.toString()))
                .andExpect(jsonPath("$.weeklyHours.weekEnd").value(monday.plusDays(6).toString()))
                .andExpect(jsonPath("$.weeklyHours.days.length()").value(7))
                .andExpect(jsonPath("$.weeklyHours.days[0].date").value(monday.toString()))
                .andExpect(jsonPath("$.weeklyHours.days[" + (today.getDayOfWeek().getValue() - 1) + "].hours").value(1.5))
                .andExpect(jsonPath("$.weeklyHours.days[" + (today.getDayOfWeek().getValue() % 7) + "].hours").value(0.0))
                .andExpect(jsonPath("$.weeklyHours.totalHours").value(1.5))
                .andExpect(jsonPath("$.weeklyHours.goalHours").value(40));

        mvc.perform(delete("/time-entries/" + olderId).header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isNoContent());
    }

    @Test
    void weeklyGoalIsValidatedAndSavedForTheAuthenticatedUser() throws Exception {
        String manager = login("gestor");

        mvc.perform(put("/dashboard/weekly-goal").contentType(MediaType.APPLICATION_JSON).content("{\"hours\":36}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/dashboard/weekly-goal").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hours\":37.5}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, login("gestor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyHours.goalHours").value(37.5));
        mvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, login("dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyHours.goalHours").value(40));
        mvc.perform(put("/dashboard/weekly-goal").header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hours\":169}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.hours").exists());
    }

    @Test
    void enforcesPermissionsAndUsesTheAuthenticatedOwner() throws Exception {
        String member = login("dev");
        String admin = login("admin");
        long adminId = profile(admin).path("id").asLong();
        long taskId = firstAssignedTask(member);

        mvc.perform(get("/time-entries/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/profile/" + adminId).header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isForbidden());
        mvc.perform(get("/dashboard/user-hours/" + adminId).header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isForbidden());
        mvc.perform(get("/user").header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isForbidden());
        mvc.perform(post("/task/add").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/time-entries").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("taskId", taskId, "userId", adminId,
                                "description", "Registro", "entryDate", LocalDate.now(), "startTime", "15:00", "endTime", "16:00"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsEditsAndDeletesAnEntryWithoutCorruptingTaskTotals() throws Exception {
        String member = login("dev");
        String admin = login("admin");
        long taskId = firstAssignedTask(member);
        double before = taskHours(admin, taskId);

        mvc.perform(post("/time-entries").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON).content(entry(taskId, "21:00", "20:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());

        var created = mvc.perform(post("/time-entries").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON).content(entry(taskId, "20:00", "21:30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskName").isString())
                .andExpect(jsonPath("$.hoursLogged").value(1.5))
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(post("/time-entries").header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON).content(entry(taskId, "21:00", "22:00")))
                .andExpect(status().isConflict());
        assertThat(taskHours(admin, taskId)).isEqualTo(before + 1.5);

        mvc.perform(put("/time-entries/" + id).header(HttpHeaders.AUTHORIZATION, member)
                        .contentType(MediaType.APPLICATION_JSON).content(entry(taskId, "20:00", "22:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoursLogged").value(2.0));
        assertThat(taskHours(admin, taskId)).isEqualTo(before + 2.0);

        mvc.perform(delete("/time-entries/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/time-entries/" + id).header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isNoContent());
        assertThat(taskHours(admin, taskId)).isEqualTo(before);
    }

    @Test
    void editingAUserKeepsTheExistingPasswordAndHidesCredentials() throws Exception {
        String admin = login("admin");
        long id = profile(login("gestor")).path("id").asLong();
        mvc.perform(get("/user").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(put("/user/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Marina Lopes", "login", "gestor",
                                "email", "gestor@example.test", "role", "MANAGER", "password", "ignored"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
        assertThat(login("gestor")).startsWith("Bearer ");
    }

    @Test
    void projectDateValidationUsesTheApiErrorFormat() throws Exception {
        String admin = login("admin");
        var result = mvc.perform(get("/project").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andReturn();
        var project = json.readTree(result.getResponse().getContentAsString()).get(0);
        var fields = json.convertValue(project, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        fields.put("deadline", LocalDate.now().minusYears(1).toString());
        mvc.perform(put("/project/" + project.path("id").asLong()).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(fields)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    private String login(String login) throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", "WorktrackDemo!"))))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + json.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private JsonNode profile(String token) throws Exception {
        var result = mvc.perform(get("/profile/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private long firstAssignedTask(String token) throws Exception {
        var result = mvc.perform(get("/task/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get(0).path("id").asLong();
    }

    private double taskHours(String token, long id) throws Exception {
        var result = mvc.perform(get("/task/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("totalHours").asDouble();
    }

    private String entry(long taskId, String start, String end) throws Exception {
        return json.writeValueAsString(Map.of("taskId", taskId, "description", "Implementação",
                "entryDate", LocalDate.now(), "startTime", start, "endTime", end));
    }
}
