ALTER TABLE congestion_records ADD COLUMN collected_at TIMESTAMP(6) NULL;
CREATE TABLE congestion_latest (
 area_code VARCHAR(16) NOT NULL PRIMARY KEY,
 area_name VARCHAR(255) NOT NULL,
 congestion_level ENUM('GREEN','YELLOW','ORANGE','RED','UNKNOWN') NOT NULL,
 congestion_message VARCHAR(500) NOT NULL,
 population_min INT NOT NULL,
 population_max INT NOT NULL,
 update_time DATETIME(6) NOT NULL,
 collected_at TIMESTAMP(6) NULL
);
-- Existing history is retained. Application startup backfills the latest row per official area
-- using observation time (not insertion ID), before scheduling begins.
