-- Create replication user
CREATE USER IF NOT EXISTS 'replica_user'@'%' IDENTIFIED BY 'replica_pass';
GRANT REPLICATION SLAVE ON *.* TO 'replica_user'@'%';

-- Create application database if not exists
CREATE DATABASE IF NOT EXISTS wallet_system;

-- Create application user with proper privileges
CREATE USER IF NOT EXISTS 'wallet_user'@'%' IDENTIFIED BY 'wallet_pass';
GRANT ALL PRIVILEGES ON wallet_system.* TO 'wallet_user'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Set GTID purged for new slaves
SET @@GLOBAL.GTID_PURGED = '';

-- Show master status for slave configuration
-- SHOW MASTER STATUS;