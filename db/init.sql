CREATE SEQUENCE IF NOT EXISTS tasks_id_seq START 1 INCREMENT 1;

CREATE TABLE IF NOT EXISTS tasks (
    id           INTEGER PRIMARY KEY DEFAULT nextval('tasks_id_seq'),
    type         VARCHAR(20) NOT NULL CHECK (type IN ('TASK','EPIC','SUBTASK')),
    name         TEXT        NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL CHECK (status IN ('NEW','IN_PROGRESS','DONE')),
    start_time   TIMESTAMP NULL,
    duration_min INTEGER NULL,
    epic_id      INTEGER NULL
    );

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_epic
        FOREIGN KEY (epic_id) REFERENCES tasks(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_tasks_type        ON tasks(type);
CREATE INDEX IF NOT EXISTS idx_tasks_start_time  ON tasks(start_time);
CREATE INDEX IF NOT EXISTS idx_tasks_epic_id     ON tasks(epic_id);