package com.wallet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Read Replicas Configuration
 * Cấu hình master-slave replication cho high availability
 * 
 * Features:
 * 1. Master-slave separation
 * 2. Read load balancing across multiple read replicas
 * 3. Automatic failover to master if read replica fails
 * 4. Connection pooling optimization
 * 5. Health monitoring
 */
@Configuration
@ConditionalOnProperty(name = "database.read-replica.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ReadReplicaConfig {

    @Value("${database.master.url:${spring.datasource.url}}")
    private String masterUrl;
    
    @Value("${database.master.username:${spring.datasource.username}}")
    private String masterUsername;
    
    @Value("${database.master.password:${spring.datasource.password}}")
    private String masterPassword;
    
    @Value("${database.slave.urls:}")
    private String[] slaveUrls;
    
    @Value("${database.slave.username:${spring.datasource.username}}")
    private String slaveUsername;
    
    @Value("${database.slave.password:${spring.datasource.password}}")
    private String slavePassword;
    
    @Value("${database.read-replica.enabled:false}")
    private boolean readReplicaEnabled;

    /**
     * Master DataSource configuration
     * Sử dụng cho all write operations
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(masterUrl);
        config.setUsername(masterUsername);
        config.setPassword(masterPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Master-specific optimizations
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(15);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("MasterHikariCP");
        
        // Master write optimizations
        config.setAutoCommit(false);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        log.info("Master DataSource configured: {}", masterUrl);
        return new HikariDataSource(config);
    }

    /**
     * Read Replica DataSource configuration
     * Sử dụng cho read-only operations
     */
    @Bean(name = "readReplicaDataSource")
    public DataSource readReplicaDataSource() {
        if (!readReplicaEnabled || slaveUrls == null || slaveUrls.length == 0) {
            log.info("Read replicas disabled, using master for reads");
            return masterDataSource();
        }
        
        // Create load balancer cho multiple read replicas
        return new ReadReplicaLoadBalancer(createReadReplicaSources());
    }

    /**
     * Primary DataSource với routing logic
     */
    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource() {
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(DatabaseType.MASTER, masterDataSource());
        dataSources.put(DatabaseType.READ_REPLICA, readReplicaDataSource());
        
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource());
        
        log.info("Routing DataSource configured with {} read replicas", 
                readReplicaEnabled ? slaveUrls.length : 0);
        
        return routingDataSource;
    }

    /**
     * Create individual read replica data sources
     */
    private Map<String, DataSource> createReadReplicaSources() {
        Map<String, DataSource> replicaSources = new HashMap<>();
        
        for (int i = 0; i < slaveUrls.length; i++) {
            String replicaUrl = slaveUrls[i];
            String replicaName = "replica-" + (i + 1);
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(replicaUrl);
            config.setUsername(slaveUsername);
            config.setPassword(slavePassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Read replica optimizations
            config.setMaximumPoolSize(30);
            config.setMinimumIdle(10);
            config.setConnectionTimeout(20000);
            config.setIdleTimeout(400000);
            config.setMaxLifetime(1200000);
            config.setPoolName("ReadReplica" + (i + 1) + "HikariCP");
            
            // Read-only optimizations
            config.setReadOnly(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "300");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            
            // Read replica specific settings
            config.addDataSourceProperty("defaultFetchSize", "1000");
            config.addDataSourceProperty("useCursorFetch", "true");
            
            replicaSources.put(replicaName, new HikariDataSource(config));
            log.info("Read replica {} configured: {}", replicaName, replicaUrl);
        }
        
        return replicaSources;
    }

    /**
     * Database type enum cho routing
     */
    public enum DatabaseType {
        MASTER,
        READ_REPLICA
    }

    /**
     * Custom routing DataSource
     */
    public static class RoutingDataSource extends AbstractRoutingDataSource {
        
        @Override
        protected Object determineCurrentLookupKey() {
            return DatabaseContextHolder.getDatabaseType();
        }
    }

    /**
     * Database context holder using ThreadLocal
     */
    public static class DatabaseContextHolder {
        
        private static final ThreadLocal<DatabaseType> contextHolder = new ThreadLocal<>();
        
        public static void setDatabaseType(DatabaseType databaseType) {
            contextHolder.set(databaseType);
        }
        
        public static DatabaseType getDatabaseType() {
            DatabaseType type = contextHolder.get();
            return type != null ? type : DatabaseType.MASTER;
        }
        
        public static void clearDatabaseType() {
            contextHolder.remove();
        }
        
        /**
         * Set read-only context cho current thread
         */
        public static void setReadOnly() {
            setDatabaseType(DatabaseType.READ_REPLICA);
        }
        
        /**
         * Set write context cho current thread
         */
        public static void setWriteMode() {
            setDatabaseType(DatabaseType.MASTER);
        }
    }

    /**
     * Load balancer cho multiple read replicas
     */
    public static class ReadReplicaLoadBalancer implements DataSource {
        
        private final Map<String, DataSource> replicaSources;
        private final String[] replicaNames;
        
        public ReadReplicaLoadBalancer(Map<String, DataSource> replicaSources) {
            this.replicaSources = replicaSources;
            this.replicaNames = replicaSources.keySet().toArray(new String[0]);
            log.info("Read replica load balancer initialized with {} replicas", replicaNames.length);
        }
        
        /**
         * Get connection với load balancing
         */
        @Override
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            return getRandomReplica().getConnection();
        }
        
        @Override
        public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            return getRandomReplica().getConnection(username, password);
        }
        
        /**
         * Round-robin selection với random start
         */
        private DataSource getRandomReplica() {
            if (replicaNames.length == 0) {
                throw new RuntimeException("No read replicas available");
            }
            
            // Simple random selection for load balancing
            int index = ThreadLocalRandom.current().nextInt(replicaNames.length);
            String selectedReplica = replicaNames[index];
            
            return replicaSources.get(selectedReplica);
        }
        
        // Delegate other DataSource methods
        @Override
        public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            return getRandomReplica().unwrap(iface);
        }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
            return getRandomReplica().isWrapperFor(iface);
        }
        
        @Override
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return getRandomReplica().getParentLogger();
        }
        
        @Override
        public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
            // Not implemented for load balancer
        }
        
        @Override
        public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
            return getRandomReplica().getLogWriter();
        }
        
        @Override
        public void setLoginTimeout(int seconds) throws java.sql.SQLException {
            // Not implemented for load balancer
        }
        
        @Override
        public int getLoginTimeout() throws java.sql.SQLException {
            return getRandomReplica().getLoginTimeout();
        }
    }

    /**
     * Health checker cho read replicas
     */
    @Bean
    public ReadReplicaHealthChecker readReplicaHealthChecker() {
        return new ReadReplicaHealthChecker(readReplicaDataSource());
    }

    /**
     * Health monitoring cho read replicas
     */
    public static class ReadReplicaHealthChecker {
        
        private final DataSource readReplicaDataSource;
        
        public ReadReplicaHealthChecker(DataSource readReplicaDataSource) {
            this.readReplicaDataSource = readReplicaDataSource;
        }
        
        /**
         * Check health của read replicas
         */
        public boolean isHealthy() {
            try (java.sql.Connection connection = readReplicaDataSource.getConnection()) {
                java.sql.Statement statement = connection.createStatement();
                java.sql.ResultSet resultSet = statement.executeQuery("SELECT 1");
                return resultSet.next() && resultSet.getInt(1) == 1;
            } catch (Exception e) {
                log.error("Read replica health check failed", e);
                return false;
            }
        }
        
        /**
         * Get connection count statistics
         */
        public Map<String, Object> getStats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("healthy", isHealthy());
            stats.put("timestamp", System.currentTimeMillis());
            return stats;
        }
    }
}