-- Database initialization for Mobile Auth Testing Framework
-- Create tables for test data management, results tracking, and reporting

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Test Users Table
CREATE TABLE test_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    is_locked BOOLEAN DEFAULT false,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login_attempt TIMESTAMP,
    account_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    biometric_enabled BOOLEAN DEFAULT false,
    two_factor_enabled BOOLEAN DEFAULT false,
    password_last_changed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    security_questions JSONB,
    user_role VARCHAR(50) DEFAULT 'user'
);

-- Test Execution Results Table
CREATE TABLE test_executions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id VARCHAR(255) UNIQUE NOT NULL,
    test_suite_name VARCHAR(255) NOT NULL,
    test_class_name VARCHAR(255) NOT NULL,
    test_method_name VARCHAR(255) NOT NULL,
    test_status VARCHAR(50) NOT NULL, -- PASSED, FAILED, SKIPPED, PENDING
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    stack_trace TEXT,
    test_data JSONB,
    environment VARCHAR(100) NOT NULL,
    browser_version VARCHAR(100),
    platform VARCHAR(100),
    device_name VARCHAR(255),
    app_version VARCHAR(100),
    build_number VARCHAR(100),
    git_commit_hash VARCHAR(255),
    jenkins_build_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Security Test Results Table
CREATE TABLE security_test_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID REFERENCES test_executions(id),
    vulnerability_type VARCHAR(255) NOT NULL,
    severity_level VARCHAR(50) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW, INFO
    confidence_level VARCHAR(50) NOT NULL, -- HIGH, MEDIUM, LOW
    affected_url VARCHAR(1000),
    affected_parameter VARCHAR(255),
    cwe_id INTEGER,
    owasp_category VARCHAR(100),
    description TEXT NOT NULL,
    solution TEXT,
    reference_urls TEXT[],
    proof_of_concept TEXT,
    zap_alert_id INTEGER,
    risk_score INTEGER,
    found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Performance Metrics Table
CREATE TABLE performance_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_id UUID REFERENCES test_executions(id),
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    metric_unit VARCHAR(50) NOT NULL, -- ms, seconds, bytes, percentage
    metric_type VARCHAR(100) NOT NULL, -- response_time, memory_usage, cpu_usage, etc.
    endpoint VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test Data Scenarios Table
CREATE TABLE test_scenarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scenario_name VARCHAR(255) UNIQUE NOT NULL,
    scenario_type VARCHAR(100) NOT NULL, -- login, registration, password_reset, etc.
    test_data JSONB NOT NULL,
    expected_result VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tags VARCHAR(255)[],
    priority INTEGER DEFAULT 1
);

-- Biometric Test Data Table
CREATE TABLE biometric_test_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES test_users(id),
    biometric_type VARCHAR(50) NOT NULL, -- fingerprint, face_id, voice
    template_data BYTEA,
    is_enrolled BOOLEAN DEFAULT false,
    device_id VARCHAR(255),
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Authentication Events Log Table
CREATE TABLE auth_events_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES test_users(id),
    event_type VARCHAR(100) NOT NULL, -- login_attempt, login_success, login_failure, logout, password_change
    ip_address INET,
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    location_data JSONB,
    additional_data JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    risk_score INTEGER DEFAULT 0
);

-- Test Environment Configuration Table
CREATE TABLE test_environments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    environment_name VARCHAR(100) UNIQUE NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    database_config JSONB,
    api_endpoints JSONB,
    test_accounts JSONB,
    environment_variables JSONB,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Historical Trend Data Table for Advanced Reporting
CREATE TABLE test_trend_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    execution_date DATE NOT NULL,
    test_suite VARCHAR(255) NOT NULL,
    total_tests INTEGER NOT NULL,
    passed_tests INTEGER NOT NULL,
    failed_tests INTEGER NOT NULL,
    skipped_tests INTEGER NOT NULL,
    success_rate DECIMAL(5,2) NOT NULL,
    avg_execution_time_ms BIGINT,
    critical_vulnerabilities INTEGER DEFAULT 0,
    high_vulnerabilities INTEGER DEFAULT 0,
    medium_vulnerabilities INTEGER DEFAULT 0,
    low_vulnerabilities INTEGER DEFAULT 0,
    build_number VARCHAR(100),
    git_commit_hash VARCHAR(255)
);

-- Insert default test users
INSERT INTO test_users (username, email, password_hash, salt, is_active, biometric_enabled, two_factor_enabled, user_role) VALUES
('valid_user', 'valid@example.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj92N/YVYK4i', 'randomsalt123', true, false, false, 'user'),
('admin_user', 'admin@example.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj92N/YVYK4i', 'randomsalt456', true, true, true, 'admin'),
('locked_user', 'locked@example.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj92N/YVYK4i', 'randomsalt789', true, false, false, 'user'),
('biometric_user', 'biometric@example.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj92N/YVYK4i', 'randomsalt101', true, true, false, 'user'),
('inactive_user', 'inactive@example.com', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj92N/YVYK4i', 'randomsalt102', false, false, false, 'user');

-- Set the locked user as locked
UPDATE test_users SET is_locked = true, failed_login_attempts = 5 WHERE username = 'locked_user';

-- Insert test scenarios
INSERT INTO test_scenarios (scenario_name, scenario_type, test_data, expected_result, tags, priority) VALUES
('valid_login_standard', 'login', '{"username": "valid_user", "password": "Valid1Password!"}', 'success', ARRAY['smoke', 'regression'], 1),
('invalid_login_wrong_password', 'login', '{"username": "valid_user", "password": "WrongPassword123!"}', 'failure', ARRAY['regression'], 2),
('invalid_login_wrong_username', 'login', '{"username": "invalid_user", "password": "Valid1Password!"}', 'failure', ARRAY['regression'], 2),
('account_lockout_scenario', 'login', '{"username": "lockout_test", "password": "WrongPassword", "attempts": 5}', 'account_locked', ARRAY['security'], 1),
('biometric_login_success', 'biometric', '{"username": "biometric_user", "biometric_type": "fingerprint"}', 'success', ARRAY['biometric', 'regression'], 2),
('password_recovery_valid_email', 'password_reset', '{"email": "valid@example.com"}', 'reset_email_sent', ARRAY['regression'], 2);

-- Insert test environments
INSERT INTO test_environments (environment_name, base_url, database_config, api_endpoints, test_accounts, environment_variables) VALUES
('development', 'http://mock-api:8081', 
 '{"host": "postgres-db", "port": 5432, "database": "mobile_auth_testing"}',
 '{"auth": "/api/auth", "users": "/api/users", "reset": "/api/password-reset"}',
 '{"valid_user": "valid@example.com", "admin_user": "admin@example.com"}',
 '{"debug": true, "log_level": "DEBUG"}'),
('staging', 'https://staging-api.example.com',
 '{"host": "staging-db.example.com", "port": 5432, "database": "mobile_auth_staging"}',
 '{"auth": "/api/v1/auth", "users": "/api/v1/users", "reset": "/api/v1/password-reset"}',
 '{"test_user": "test@staging.example.com"}',
 '{"debug": false, "log_level": "INFO"}');

-- Create indexes for better performance
CREATE INDEX idx_test_executions_status ON test_executions(test_status);
CREATE INDEX idx_test_executions_execution_date ON test_executions(DATE(start_time));
CREATE INDEX idx_test_executions_suite_name ON test_executions(test_suite_name);
CREATE INDEX idx_security_test_results_severity ON security_test_results(severity_level);
CREATE INDEX idx_security_test_results_execution_id ON security_test_results(execution_id);
CREATE INDEX idx_performance_metrics_execution_id ON performance_metrics(execution_id);
CREATE INDEX idx_auth_events_log_user_id ON auth_events_log(user_id);
CREATE INDEX idx_auth_events_log_timestamp ON auth_events_log(timestamp);
CREATE INDEX idx_test_trend_data_execution_date ON test_trend_data(execution_date);

-- Create views for reporting
CREATE VIEW test_execution_summary AS
SELECT 
    DATE(start_time) as execution_date,
    test_suite_name,
    COUNT(*) as total_tests,
    COUNT(CASE WHEN test_status = 'PASSED' THEN 1 END) as passed_tests,
    COUNT(CASE WHEN test_status = 'FAILED' THEN 1 END) as failed_tests,
    COUNT(CASE WHEN test_status = 'SKIPPED' THEN 1 END) as skipped_tests,
    ROUND(
        COUNT(CASE WHEN test_status = 'PASSED' THEN 1 END) * 100.0 / COUNT(*), 2
    ) as success_rate,
    AVG(duration_ms) as avg_duration_ms
FROM test_executions
GROUP BY DATE(start_time), test_suite_name
ORDER BY execution_date DESC;

CREATE VIEW security_vulnerability_summary AS
SELECT 
    DATE(found_at) as scan_date,
    vulnerability_type,
    severity_level,
    COUNT(*) as vulnerability_count,
    AVG(risk_score) as avg_risk_score
FROM security_test_results
GROUP BY DATE(found_at), vulnerability_type, severity_level
ORDER BY scan_date DESC, severity_level;

-- Function to automatically update test trend data
CREATE OR REPLACE FUNCTION update_test_trend_data()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO test_trend_data (
        execution_date, test_suite, total_tests, passed_tests, 
        failed_tests, skipped_tests, success_rate, avg_execution_time_ms,
        build_number, git_commit_hash
    )
    SELECT 
        DATE(NEW.start_time),
        NEW.test_suite_name,
        COUNT(*),
        COUNT(CASE WHEN test_status = 'PASSED' THEN 1 END),
        COUNT(CASE WHEN test_status = 'FAILED' THEN 1 END),
        COUNT(CASE WHEN test_status = 'SKIPPED' THEN 1 END),
        ROUND(COUNT(CASE WHEN test_status = 'PASSED' THEN 1 END) * 100.0 / COUNT(*), 2),
        AVG(duration_ms),
        NEW.build_number,
        NEW.git_commit_hash
    FROM test_executions 
    WHERE DATE(start_time) = DATE(NEW.start_time) 
    AND test_suite_name = NEW.test_suite_name
    ON CONFLICT (execution_date, test_suite) DO UPDATE SET
        total_tests = EXCLUDED.total_tests,
        passed_tests = EXCLUDED.passed_tests,
        failed_tests = EXCLUDED.failed_tests,
        skipped_tests = EXCLUDED.skipped_tests,
        success_rate = EXCLUDED.success_rate,
        avg_execution_time_ms = EXCLUDED.avg_execution_time_ms;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for automatic trend data updates
CREATE TRIGGER trigger_update_test_trend_data
    AFTER INSERT OR UPDATE ON test_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_test_trend_data();

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO test_user;