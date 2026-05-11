create table users
(
    id              bigserial primary key,
    email           varchar(255) not null unique,
    password_hash   varchar(255) not null,
    role            varchar(30)  not null,
    level           varchar(30)  not null,
    enabled         boolean      not null default true,
    created_at      timestamp    not null default now(),
    version         bigint       not null default 0
);

create table projects
(
    id              bigserial primary key,
    name            varchar(255) not null,
    start_date      date         not null,
    end_date        date         not null,
    status          varchar(30)  not null,
    version         bigint       not null default 0
);

create table teams
(
    id              bigserial primary key,
    project_id       bigint       not null references projects (id) on delete cascade,
    name            varchar(255) not null,
    version         bigint       not null default 0,
    unique (project_id, name)
);

create table team_members
(
    team_id         bigint not null references teams (id) on delete cascade,
    user_id         bigint not null references users (id) on delete cascade,
    primary key (team_id, user_id)
);

create table sub_teams
(
    id              bigserial primary key,
    team_id         bigint       not null references teams (id) on delete cascade,
    direction       varchar(30)  not null,
    name            varchar(255) not null,
    version         bigint       not null default 0,
    unique (team_id, name)
);

create table sub_team_members
(
    sub_team_id     bigint not null references sub_teams (id) on delete cascade,
    user_id         bigint not null references users (id) on delete cascade,
    primary key (sub_team_id, user_id)
);

create table sprints
(
    id              bigserial primary key,
    project_id      bigint      not null references projects (id) on delete cascade,
    name            varchar(255) not null,
    start_date      date        not null,
    end_date        date        not null,
    status          varchar(30) not null,
    version         bigint      not null default 0
);

create table tasks
(
    id              bigserial primary key,
    sprint_id       bigint      not null references sprints (id) on delete cascade,
    title           varchar(255) not null,
    description     text,
    status          varchar(30) not null,
    priority        varchar(30) not null,
    assignee_id     bigint references users (id),
    deadline        timestamp,
    estimated_hours integer     not null default 1,
    version         bigint      not null default 0
);

create table task_dependencies
(
    id              bigserial primary key,
    blocker_task_id bigint not null references tasks (id) on delete cascade,
    blocked_task_id bigint not null references tasks (id) on delete cascade,
    unique (blocker_task_id, blocked_task_id)
);

create table notifications
(
    id              bigserial primary key,
    user_id         bigint      not null references users (id) on delete cascade,
    type            varchar(30) not null,
    text            text        not null,
    delivery_status varchar(30) not null,
    created_at      timestamp   not null default now()
);

create table idempotency_records
(
    id              bigserial primary key,
    idempotency_key varchar(255) not null unique,
    operation       varchar(255) not null,
    response_code   integer      not null,
    response_body   text         not null,
    created_at      timestamp    not null default now()
);

create index idx_tasks_status on tasks (status);
create index idx_tasks_assignee on tasks (assignee_id);
create index idx_projects_status on projects (status);
