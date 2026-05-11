create index if not exists idx_tasks_sprint_status on tasks (sprint_id, status);
create index if not exists idx_tasks_deadline_status on tasks (deadline, status);
create index if not exists idx_projects_start_date on projects (start_date);
