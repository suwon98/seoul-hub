CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 email VARCHAR(255) NOT NULL,
 name VARCHAR(255) NOT NULL,
 password VARCHAR(255) NOT NULL,
 role ENUM('USER','ADMIN') NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT uk_users_email UNIQUE(email)
);
CREATE TABLE questions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 title VARCHAR(100) NOT NULL,
 content TEXT NOT NULL,
 user_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_question_user FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE answers (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 content TEXT NOT NULL,
 question_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_answer_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT fk_answer_question FOREIGN KEY(question_id) REFERENCES questions(id)
);
CREATE TABLE congestion_records (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 area_name VARCHAR(255) NOT NULL,
 congestion_level ENUM('GREEN','YELLOW','ORANGE','RED','UNKNOWN') NOT NULL,
 congestion_message VARCHAR(500) NOT NULL,
 population_min INT NOT NULL,
 population_max INT NOT NULL,
 update_time DATETIME(6) NOT NULL,
 CONSTRAINT uk_observation UNIQUE(area_name,update_time)
);
CREATE INDEX idx_observation_time ON congestion_records(update_time);
CREATE TABLE congestion_hourly_stats (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 area_name VARCHAR(255) NOT NULL,
 congestion_level ENUM('GREEN','YELLOW','ORANGE','RED','UNKNOWN') NOT NULL,
 avg_population_min INT NOT NULL,
 avg_population_max INT NOT NULL,
 stat_hour DATETIME(6) NOT NULL,
 CONSTRAINT uk_hourly UNIQUE(area_name,stat_hour)
);
CREATE INDEX idx_hourly_time ON congestion_hourly_stats(stat_hour);
CREATE TABLE login_sessions (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 email VARCHAR(255) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_session_expiry ON login_sessions(expires_at);
