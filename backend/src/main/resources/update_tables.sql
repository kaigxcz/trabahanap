-- ============================================================
-- TrabaHanap Database Update Script
-- Run this in MySQL Workbench on the 'railway' schema
-- ============================================================

USE railway;

-- ── USERS table ──────────────────────────────────────────────
-- Add middle_name (if not already added)
ALTER TABLE users ADD COLUMN IF NOT EXISTS middle_name VARCHAR(255) NULL AFTER first_name;

-- Add suffix
ALTER TABLE users ADD COLUMN IF NOT EXISTS suffix VARCHAR(50) NULL AFTER last_name;

-- Add birthday and age
ALTER TABLE users ADD COLUMN IF NOT EXISTS birthday VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS age INT NULL;

-- Add created_at timestamp
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at DATETIME NULL;

-- ── APPLICATIONS table ────────────────────────────────────────
-- Add employer_note column (for employer notes on applicants)
ALTER TABLE applications ADD COLUMN IF NOT EXISTS employer_note VARCHAR(1000) NULL;

-- ── JOBS table ────────────────────────────────────────────────
-- Add featured column
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS featured BOOLEAN DEFAULT FALSE;

-- ── MESSAGES table ────────────────────────────────────────────
-- Add subject column
ALTER TABLE messages ADD COLUMN IF NOT EXISTS subject VARCHAR(255) NULL;

-- ── INTERVIEWS table ──────────────────────────────────────────
-- Create interviews table if it doesn't exist
CREATE TABLE IF NOT EXISTS interviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    candidate_id   BIGINT NOT NULL,
    job_title   VARCHAR(255),
    company     VARCHAR(255),
    scheduled_at DATETIME,
    location    VARCHAR(255),
    meeting_link VARCHAR(500),
    notes       TEXT,
    status      VARCHAR(50) DEFAULT 'SCHEDULED',
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id)   REFERENCES users(id)        ON DELETE CASCADE
);

-- ── SAVED_JOBS table ──────────────────────────────────────────
-- Create saved_jobs table if it doesn't exist
CREATE TABLE IF NOT EXISTS saved_jobs (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    job_id   BIGINT NOT NULL,
    saved_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id)  REFERENCES jobs(id)  ON DELETE CASCADE
);

-- ── NOTIFICATIONS table ───────────────────────────────────────
-- Already handled by create_tables.sql but ensure columns exist
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS notif_type VARCHAR(50) NULL;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read    BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_at DATETIME NULL;

-- ============================================================
-- Done! All tables are up to date.
-- ============================================================
