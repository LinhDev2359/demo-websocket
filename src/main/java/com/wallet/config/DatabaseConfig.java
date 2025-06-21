package com.wallet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * Database configuration for master-slave setup
 * Master: Write operations
 * Slave: Read operations (when available)
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.wallet.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@Profile("prod") // Only enable for production, use Spring Boot defaults for dev
public class DatabaseConfig {

    /**
     * Master DataSource Configuration (Write operations)
     */
    @Bean(name = "masterDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource masterDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl(System.getenv().getOrDefault("DATABASE_URL", 
            "jdbc:mysql://localhost:3306/wallet_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
        config.setUsername(System.getenv().getOrDefault("DATABASE_USERNAME", "wallet_user"));
        config.setPassword(System.getenv().getOrDefault("DATABASE_PASSWORD", "wallet_pass"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool settings optimized for high-traffic
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("WalletMasterHikariCP");
        
        // Performance settings
        config.setAutoCommit(false);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }

    /**
     * Slave DataSource Configuration (Read operations)
     * Falls back to master if slave is not available
     */
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource() {
        String slaveUrl = System.getenv().getOrDefault("DATABASE_SLAVE_URL", 
            System.getenv().getOrDefault("DATABASE_URL", 
                "jdbc:mysql://localhost:3307/wallet_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
        
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl(slaveUrl);
        config.setUsername(System.getenv().getOrDefault("DATABASE_USERNAME", "wallet_user"));
        config.setPassword(System.getenv().getOrDefault("DATABASE_PASSWORD", "wallet_pass"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool settings optimized for read operations
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(900000);
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("WalletSlaveHikariCP");
        
        // Performance settings for read operations
        config.setReadOnly(true);
        config.setAutoCommit(true); // Read operations can auto-commit
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }

    /**
     * Routing DataSource to switch between master and slave
     */
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource) {
        
        DatabaseRoutingDataSource routingDataSource = new DatabaseRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(Map.of(
            "master", masterDataSource,
            "slave", slaveDataSource
        ));
        
        return routingDataSource;
    }

    /**
     * Entity Manager Factory
     */
    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("masterDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.wallet.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        // Hibernate properties
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.jdbc.batch_size", "50");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        
        em.setJpaProperties(properties);
        
        return em;
    }

    /**
     * Transaction Manager
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        
        return transactionManager;
    }
}