package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerAnnotationsTest {

    @Test
    void keyEndpointsShouldHaveOperationAndApiResponse() throws Exception {
        Map<String, Class<?>[]> methods = Map.of(
                "createUser", new Class[]{ru.tischenko.vk.api.dto.Dtos.UserRequest.class},
                "createProject", new Class[]{ru.tischenko.vk.api.dto.Dtos.ProjectRequest.class},
                "createTask", new Class[]{ru.tischenko.vk.api.dto.Dtos.TaskRequest.class},
                "startTask", new Class[]{ru.tischenko.vk.api.dto.Dtos.StartTaskRequest.class},
                "bulkRebalance", new Class[]{ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest.class, String.class}
        );

        for (var entry : methods.entrySet()) {
            Method m = CoreController.class.getMethod(entry.getKey(), entry.getValue());
            Operation operation = m.getAnnotation(Operation.class);
            ApiResponse[] responses = m.getAnnotationsByType(ApiResponse.class);
            assertNotNull(operation, "Missing @Operation on " + entry.getKey());
            assertTrue(responses.length > 0, "Missing @ApiResponse on " + entry.getKey());
        }
    }
}
