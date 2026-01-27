-- Video Consultation Tables
-- Created: 2026-01-24
-- Description: Tables for WebRTC video consultation sessions and participant presence

-- ============================================
-- Table: consultation_sessions
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_sessions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    consultation_id BINARY(16) NULL,
    queue_entry_id BINARY(16) NOT NULL,
    room_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    doctor_id BINARY(16) NOT NULL,
    patient_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    last_activity_at DATETIME NOT NULL,
    token_expires_at DATETIME NOT NULL,
    version BIGINT DEFAULT 0,

    -- Audit fields
    created_by VARCHAR(36) NULL,
    updated_at DATETIME NULL,
    updated_by VARCHAR(36) NULL,

    -- Unique constraints
    CONSTRAINT uk_consultation_session UNIQUE (consultation_id),
    CONSTRAINT uk_room_id UNIQUE (room_id),

    -- Indexes for performance
    INDEX idx_room_id (room_id),
    INDEX idx_doctor_status (doctor_id, status),
    INDEX idx_patient_status (patient_id, status),
    INDEX idx_queue_entry (queue_entry_id),
    INDEX idx_status (status),
    INDEX idx_token_expires (token_expires_at),

    -- Foreign key
    CONSTRAINT fk_session_queue_entry FOREIGN KEY (queue_entry_id)
        REFERENCES queue_entries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Video consultation sessions with WebRTC room details';

-- ============================================
-- Table: consultation_participant_presence
-- ============================================
CREATE TABLE IF NOT EXISTS consultation_participant_presence (
    id BINARY(16) NOT NULL PRIMARY KEY,
    session_id BINARY(16) NOT NULL,
    participant_id BINARY(16) NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL,
    left_at DATETIME NULL,
    last_ping_at DATETIME NOT NULL,
    connection_state VARCHAR(20) NOT NULL,
    user_agent VARCHAR(500) NULL,

    -- Audit fields
    created_by VARCHAR(36) NULL,
    updated_at DATETIME NULL,
    updated_by VARCHAR(36) NULL,

    -- Unique constraint
    CONSTRAINT uk_session_participant UNIQUE (session_id, participant_id),

    -- Indexes
    INDEX idx_session_participant (session_id, participant_id),
    INDEX idx_session_state (session_id, connection_state),
    INDEX idx_session_id (session_id),

    -- Foreign key
    CONSTRAINT fk_presence_session FOREIGN KEY (session_id)
        REFERENCES consultation_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Participant presence tracking for video consultations';

-- ============================================
-- Sample Data (Optional - for testing)
-- ============================================
-- Uncomment to insert sample data for development/testing

-- INSERT INTO consultation_sessions
-- (id, queue_entry_id, room_id, status, doctor_id, patient_id,
--  created_at, last_activity_at, token_expires_at)
-- VALUES
-- (UNHEX(REPLACE(UUID(), '-', '')),
--  UNHEX(REPLACE('[QUEUE_ENTRY_ID]', '-', '')),
--  UUID(),
--  'ACTIVE',
--  UNHEX(REPLACE('[DOCTOR_ID]', '-', '')),
--  UNHEX(REPLACE('[PATIENT_ID]', '-', '')),
--  NOW(),
--  NOW(),
--  DATE_ADD(NOW(), INTERVAL 30 MINUTE));

-- ============================================
-- Verification Queries
-- ============================================
-- Run these to verify tables were created successfully

-- Check consultation_sessions structure
-- DESCRIBE consultation_sessions;

-- Check consultation_participant_presence structure
-- DESCRIBE consultation_participant_presence;

-- Count records
-- SELECT COUNT(*) FROM consultation_sessions;
-- SELECT COUNT(*) FROM consultation_participant_presence;

-- Check indexes
-- SHOW INDEX FROM consultation_sessions;
-- SHOW INDEX FROM consultation_participant_presence;
