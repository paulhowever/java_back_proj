package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerAnnotationsTest {

    private record Endpoint(Class<?> controller, String method, Class<?>... params) {}

    @Test
    void keyEndpointsShouldHaveOperationAndApiResponse() throws Exception {
        List<Endpoint> endpoints = List.of(
                new Endpoint(UserController.class, "createUser", ru.tischenko.vk.api.dto.Dtos.UserRequest.class),
                new Endpoint(ProjectController.class, "createProject", ru.tischenko.vk.api.dto.Dtos.ProjectRequest.class),
                new Endpoint(TaskController.class, "createTask", ru.tischenko.vk.api.dto.Dtos.TaskRequest.class),
                new Endpoint(TaskBusinessController.class, "startTask", ru.tischenko.vk.api.dto.Dtos.StartTaskRequest.class),
                new Endpoint(SprintBusinessController.class, "bulkRebalance", ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest.class, String.class)
        );

        for (Endpoint e : endpoints) {
            Method m = e.controller().getMethod(e.method(), e.params());
            Operation operation = m.getAnnotation(Operation.class);
            ApiResponse[] responses = m.getAnnotationsByType(ApiResponse.class);
            assertNotNull(operation, "Missing @Operation on " + e.method());
            assertTrue(responses.length > 0, "Missing @ApiResponse on " + e.method());
        }
    }
}
