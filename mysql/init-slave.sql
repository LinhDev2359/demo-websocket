-- Configure slave to replicate from master
-- Note: This will be executed after master is ready
-- The actual slave configuration will be done via environment variables and Docker networking

-- Create application database if not exists
CREATE DATABASE IF NOT EXISTS wallet_system;

-- Create application user with read privileges
CREATE USER IF NOT EXISTS 'wallet_user'@'%' IDENTIFIED BY 'wallet_pass';
GRANT SELECT ON wallet_system.* TO 'wallet_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;