-- Composite index for sprint-scoped task queries (complete-sprint, bulk-rebalance,
-- escalate-critical) which always filter by sprint_id and frequently by status.
create index if not exists idx_tasks_sprint_status on tasks (sprint_id, status);
-- Supports the @Scheduled overdue-task scan: filter by deadline range + status.
create index if not exists idx_tasks_deadline_status on tasks (deadline, status);
-- Backs the startDateFrom/startDateTo filters and sort=startDate of GET /api/v1/projects.
create index if not exists idx_projects_start_date on projects (start_date);
