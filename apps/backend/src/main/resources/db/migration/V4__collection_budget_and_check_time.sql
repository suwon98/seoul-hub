ALTER TABLE congestion_latest ADD COLUMN last_checked_at TIMESTAMP(6) NULL;
CREATE TABLE seoul_api_budget (
 id VARCHAR(32) NOT NULL PRIMARY KEY,
 budget_date DATE NULL,
 used BIGINT NOT NULL,
    next_area_index INTEGER NOT NULL DEFAULT 0,
 next_allowed_at TIMESTAMP(6) NULL
);
