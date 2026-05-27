package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest;
import ru.tischenko.vk.api.dto.Dtos.CompleteSprintRequest;
import ru.tischenko.vk.api.dto.Dtos.CompleteSprintResponse;
import ru.tischenko.vk.api.dto.Dtos.CriticalEscalationResponse;
import ru.tischenko.vk.api.dto.Dtos.EscalateCriticalRequest;
import ru.tischenko.vk.api.dto.Dtos.IdempotentResultResponse;
import ru.tischenko.vk.service.IdempotentOperationExecutor;
import ru.tischenko.vk.service.ops.SprintOperationsService;
import ru.tischenko.vk.service.ops.TaskOperationsService;

@RestController
@RequestMapping("/api/v1/business/sprint")
public class SprintBusinessController {
    private final SprintOperationsService sprintOperations;
    private final TaskOperationsService taskOperations;
    private final IdempotentOperationExecutor idempotentOperationExecutor;

    public SprintBusinessController(SprintOperationsService sprintOperations,
                                    TaskOperationsService taskOperations,
                                    IdempotentOperationExecutor idempotentOperationExecutor) {
        this.sprintOperations = sprintOperations;
        this.taskOperations = taskOperations;
        this.idempotentOperationExecutor = idempotentOperationExecutor;
    }

    @PostMapping("/escalate-critical")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Auto-escalate priority for tasks that block N+ others in sprint")
    @ApiResponse(responseCode = "200", description = "Returns ids of escalated tasks")
    public CriticalEscalationResponse escalateCritical(@RequestBody @Valid EscalateCriticalRequest req) {
        return taskOperations.escalateCriticalTasks(req.sprintId());
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete sprint: marks DONE, emits notifications for unfinished tasks")
    public CompleteSprintResponse completeSprint(@RequestBody @Valid CompleteSprintRequest req) {
        return sprintOperations.completeSprint(req);
    }

    @PostMapping("/bulk-rebalance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk rebalance sprint tasks across project team members (idempotent via Idempotency-Key header)")
    @ApiResponse(responseCode = "200", description = "Rebalance completed or replayed")
    @ApiResponse(responseCode = "409", description = "Operation already in progress or business rule violation")
    public IdempotentResultResponse bulkRebalance(@RequestBody @Valid BulkRebalanceRequest req,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return idempotentOperationExecutor.execute(idempotencyKey, "bulkRebalance", () -> sprintOperations.bulkRebalance(req));
    }
}
