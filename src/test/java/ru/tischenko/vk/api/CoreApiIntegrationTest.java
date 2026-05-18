package ru.tischenko.vk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class CoreApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void shouldReturn401WhenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.local","password":"secret123","role":"ADMIN","level":"LEAD"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldReturn401WhenBearerIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/users/1").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn401WhenJwtIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/users/1").header("Authorization", "Bearer not.a.real.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserHitsAdminOnlyEndpoint() throws Exception {
        // register and log in a plain USER
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"plain-user@test.local","password":"secret123","level":"JUNIOR"}
                                """))
                .andExpect(status().isOk());
        String userToken = login("plain-user@test.local", "secret123");

        // DELETE /projects/{id} requires ADMIN -> USER must get 403, not 401
        mockMvc.perform(delete("/api/v1/projects/999").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404ForUnknownEndpoint() throws Exception {
        String token = login("admin@local", "admin12345");
        mockMvc.perform(get("/api/v1/this/does/not/exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn404WhenResourceMissing() throws Exception {
        String token = login("admin@local", "admin12345");
        mockMvc.perform(get("/api/v1/projects/9999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    void shouldReturn409WhenPutCausesDuplicateEmail() throws Exception {
        String token = login("admin@local", "admin12345");

        // create two users
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup-a@test.local","password":"secret123","level":"JUNIOR"}
                                """))
                .andExpect(status().isOk());
        String createdB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup-b@test.local","password":"secret123","level":"JUNIOR"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long bId = objectMapper.readTree(createdB).get("id").asLong();

        // try to rename B -> A's email; commit-time unique violation must map to 409
        mockMvc.perform(put("/api/v1/users/" + bId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup-a@test.local","password":"secret123","role":"USER","level":"JUNIOR"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void bootstrappedAdminShouldBeAbleToLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@local","password":"admin12345"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String body = login.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        String token = json.get("token").asText();
        assertNotNull(token);
        assertTrue(token.length() > 10);

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidPayloadWith400AndFieldErrors() throws Exception {
        // login as admin first
        String token = login("admin@local", "admin12345");

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","startDate":null,"endDate":null,"status":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldRejectSprintWithFourDayDurationWith409() throws Exception {
        String token = login("admin@local", "admin12345");

        // create project (12-day duration)
        String projectJson = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"P-sprint-test","startDate":"2026-05-12","endDate":"2026-05-23","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        mockMvc.perform(post("/api/v1/sprints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":%d,"name":"S4","startDate":"2026-05-12","endDate":"2026-05-15","status":"ACTIVE"}
                                """.formatted(projectId)))
                .andExpect(status().isConflict());
    }

    @Test
    void endToEndHappyPath() throws Exception {
        String token = login("admin@local", "admin12345");

        // create project
        String projectJson = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"E2E-prj","startDate":"2026-05-12","endDate":"2026-05-20","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(projectJson).get("id").asLong();

        // create sprint (2-day duration -> ok)
        String sprintJson = mockMvc.perform(post("/api/v1/sprints")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":%d,"name":"S1","startDate":"2026-05-12","endDate":"2026-05-13","status":"ACTIVE"}
                                """.formatted(projectId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sprintId = objectMapper.readTree(sprintJson).get("id").asLong();

        // create task in sprint
        String taskJson = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sprintId":%d,"title":"design","status":"TODO","priority":"HIGH","estimatedHours":4}
                                """.formatted(sprintId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = objectMapper.readTree(taskJson).get("id").asLong();

        // start task -> should succeed (no dependencies, no overload)
        mockMvc.perform(post("/api/v1/business/task/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":%d}".formatted(taskId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // list tasks with filter
        mockMvc.perform(get("/api/v1/tasks?status=IN_PROGRESS&page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
