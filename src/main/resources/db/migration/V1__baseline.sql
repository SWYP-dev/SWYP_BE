-- Baseline schema generated from the current Hibernate entity metadata.
-- Existing databases must register version 1 using SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
-- on the first deployment instead of executing this script.

CREATE TABLE application_postings (deadline date, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, source_job_posting_id bigint, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, category varchar(50), region varchar(100), thumbnail_url varchar(500), original_url varchar(2048), company_name varchar(255) NOT NULL, title varchar(255) NOT NULL, career_type enum ('EXPERIENCED','NEW'), platform enum ('DIRECT','EXTERNAL','JOBABA','PUBLIC','PUBLIC_PERSONNEL','ROCKETPUNCH','SARAMIN','WORKNET') DEFAULT 'DIRECT' NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE bookmarks (is_active bit NOT NULL, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, job_posting_id bigint NOT NULL, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE documents (version integer NOT NULL, application_posting_id bigint NOT NULL, created_at datetime(6) NOT NULL, deleted_at datetime(6), file_size bigint, id bigint NOT NULL AUTO_INCREMENT, user_id bigint NOT NULL, version_group varchar(100), file_url varchar(500), link_url varchar(500), file_name varchar(255), memo text, original_name varchar(255), doc_type enum ('FILE','LINK','MEMO') NOT NULL, link_category enum ('OTHER','PERSONAL_CHANNEL','PORTFOLIO','RESUME'), PRIMARY KEY (id), CHECK ((doc_type = 'FILE' AND file_url IS NOT NULL) OR (doc_type = 'LINK' AND link_url IS NOT NULL) OR (doc_type = 'MEMO' AND memo IS NOT NULL))) ENGINE=InnoDB;
CREATE TABLE job_feed_career_types (job_feed_id bigint NOT NULL, career_type enum ('EXPERIENCED','NEW') NOT NULL, PRIMARY KEY (job_feed_id, career_type)) ENGINE=InnoDB;
CREATE TABLE job_feeds (deadline date, crawled_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, region varchar(100), region_raw varchar(500), thumbnail_url varchar(500), original_url varchar(1000), category varchar(255), company_name varchar(255) NOT NULL, external_id varchar(255) NOT NULL, title varchar(255) NOT NULL, platform enum ('DIRECT','EXTERNAL','JOBABA','PUBLIC','PUBLIC_PERSONNEL','ROCKETPUNCH','SARAMIN','WORKNET') NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE job_postings (deadline date, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, category varchar(50), region varchar(100), thumbnail_url varchar(500), original_url varchar(2048), company_name varchar(255) NOT NULL, source_external_id varchar(255), title varchar(255) NOT NULL, career_type enum ('EXPERIENCED','NEW'), platform enum ('DIRECT','EXTERNAL','JOBABA','PUBLIC','PUBLIC_PERSONNEL','ROCKETPUNCH','SARAMIN','WORKNET') NOT NULL, source_platform enum ('DIRECT','EXTERNAL','JOBABA','PUBLIC','PUBLIC_PERSONNEL','ROCKETPUNCH','SARAMIN','WORKNET'), PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE kanban_cards (deadline_changed bit NOT NULL, deadline_position integer, position integer NOT NULL, application_posting_id bigint NOT NULL, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, stage_id bigint NOT NULL, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, memo text, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE kanban_stages (is_default bit NOT NULL, position integer NOT NULL, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, stage_name varchar(50) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE notification_settings (email_enabled bit NOT NULL, in_app_enabled bit NOT NULL, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, updated_at datetime(6) NOT NULL, user_id bigint NOT NULL, remind_days json NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE notifications (days_before_deadline integer NOT NULL, is_read bit NOT NULL, created_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, kanban_card_id bigint NOT NULL, sent_at datetime(6), user_id bigint NOT NULL, message varchar(500) NOT NULL, status enum ('FAILED','SUCCESS') NOT NULL, type enum ('EMAIL','IN_APP') NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE refresh_tokens (created_at datetime(6) NOT NULL, expires_at datetime(6) NOT NULL, id bigint NOT NULL AUTO_INCREMENT, user_id bigint NOT NULL, token varchar(255) NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;
CREATE TABLE users (created_at datetime(6) NOT NULL, deleted_at datetime(6), id bigint NOT NULL AUTO_INCREMENT, updated_at datetime(6) NOT NULL, nickname varchar(50) NOT NULL, profile_image_url varchar(500), email varchar(255) NOT NULL, provider_id varchar(255), provider enum ('KAKAO') NOT NULL, PRIMARY KEY (id)) ENGINE=InnoDB;

CREATE INDEX idx_application_postings_user_id ON application_postings (user_id);
CREATE INDEX idx_application_postings_source ON application_postings (source_job_posting_id);
CREATE INDEX idx_application_postings_deadline ON application_postings (deadline);
ALTER TABLE application_postings ADD CONSTRAINT uk_application_postings_user_source UNIQUE (user_id, source_job_posting_id);
ALTER TABLE bookmarks ADD CONSTRAINT uk_bookmarks_user_job_posting UNIQUE (user_id, job_posting_id);
CREATE INDEX idx_documents_user_id ON documents (user_id);
CREATE INDEX idx_documents_application_posting_id ON documents (application_posting_id);
CREATE INDEX idx_documents_version_group ON documents (application_posting_id, version_group);
CREATE INDEX idx_job_fed_deadline ON job_feeds (deadline);
CREATE INDEX idx_job_feed_region ON job_feeds (region);
ALTER TABLE job_feeds ADD CONSTRAINT uk_job_feed_platform_external_id UNIQUE (platform, external_id);
CREATE INDEX idx_job_posting_user_id ON job_postings (user_id);
CREATE INDEX idx_job_posting_deadline ON job_postings (deadline);
CREATE INDEX idx_job_posting_source ON job_postings (source_platform, source_external_id);
ALTER TABLE kanban_cards ADD CONSTRAINT uk_kanban_cards_application_posting UNIQUE (application_posting_id);
ALTER TABLE kanban_cards ADD CONSTRAINT uk_kanban_cards_stage_position UNIQUE (stage_id, position);
ALTER TABLE kanban_cards ADD CONSTRAINT uk_kanban_cards_user_deadline_position UNIQUE (user_id, deadline_position);
ALTER TABLE kanban_stages ADD CONSTRAINT uk_kanban_stages_user_position UNIQUE (user_id, position);
ALTER TABLE kanban_stages ADD CONSTRAINT uk_kanban_stages_user_name UNIQUE (user_id, stage_name);
ALTER TABLE notification_settings ADD CONSTRAINT uk_notification_settings_user UNIQUE (user_id);
CREATE INDEX idx_notifications_user_id_is_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_kanban_card_id ON notifications (kanban_card_id);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens (user_id);
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_token UNIQUE (token);
ALTER TABLE users ADD CONSTRAINT idx_users_provider_id UNIQUE (provider_id, provider);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

ALTER TABLE application_postings ADD CONSTRAINT fk_application_postings_source FOREIGN KEY (source_job_posting_id) REFERENCES job_postings (id) ON DELETE SET NULL;
ALTER TABLE application_postings ADD CONSTRAINT fk_application_postings_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE bookmarks ADD CONSTRAINT fk_bookmarks_job_posting FOREIGN KEY (job_posting_id) REFERENCES job_postings (id);
ALTER TABLE bookmarks ADD CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE documents ADD CONSTRAINT fk_documents_application_posting FOREIGN KEY (application_posting_id) REFERENCES application_postings (id);
ALTER TABLE documents ADD CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE job_feed_career_types ADD CONSTRAINT fk_job_feed_career_types_job_feed FOREIGN KEY (job_feed_id) REFERENCES job_feeds (id);
ALTER TABLE job_postings ADD CONSTRAINT fk_job_postings_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE kanban_cards ADD CONSTRAINT fk_kanban_cards_application_posting FOREIGN KEY (application_posting_id) REFERENCES application_postings (id);
ALTER TABLE kanban_cards ADD CONSTRAINT fk_kanban_cards_stage FOREIGN KEY (stage_id) REFERENCES kanban_stages (id);
ALTER TABLE kanban_cards ADD CONSTRAINT fk_kanban_cards_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE kanban_stages ADD CONSTRAINT fk_kanban_stages_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE notification_settings ADD CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_kanban_card FOREIGN KEY (kanban_card_id) REFERENCES kanban_cards (id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id);
