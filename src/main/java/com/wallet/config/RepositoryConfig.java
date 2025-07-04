package com.wallet.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Repository Configuration
 * 
 * Chức năng:
 * 1. Configure Read/Write repository separation
 * 2. Setup master/slave datasource routing
 * 3. Optimize cho millions users
 * 4. Support horizontal scaling
 */
@Configuration
@ConditionalOnProperty(name = "app.repository.read-write-separation.enabled", havingValue = "true", matchIfMissing = false)
public class RepositoryConfig {
    
    /**
     * Master DataSource (Write operations)
     */
    @Primary
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * Slave DataSource (Read operations)
     */
    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    @ConditionalOnProperty(name = "spring.datasource.slave.url")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * Entity Manager Factory cho Write operations
     */
    @Primary
    @Bean(name = "writeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean writeEntityManagerFactory(
            @Qualifier("masterDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.wallet.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.jdbc.batch_size", "50");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    /**
     * Entity Manager Factory cho Read operations
     */
    @Bean(name = "readEntityManagerFactory")
    @ConditionalOnProperty(name = "spring.datasource.slave.url")
    public LocalContainerEntityManagerFactoryBean readEntityManagerFactory(
            @Qualifier("slaveDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.wallet.entity");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.show_sql", "false");
        // Read-optimized settings
        properties.setProperty("hibernate.jdbc.fetch_size", "100");
        properties.setProperty("hibernate.cache.use_second_level_cache", "true");
        properties.setProperty("hibernate.cache.use_query_cache", "true");
        em.setJpaProperties(properties);
        
        return em;
    }
    
    /**
     * Transaction Manager cho Write operations
     */
    @Primary
    @Bean(name = "writeTransactionManager")
    public PlatformTransactionManager writeTransactionManager(
            @Qualifier("writeEntityManagerFactory") LocalContainerEntityManagerFactoryBean writeEntityManagerFactory) {
        return new JpaTransactionManager(writeEntityManagerFactory.getObject());
    }
    
    /**
     * Transaction Manager cho Read operations (read-only)
     */
    @Bean(name = "readTransactionManager")
    @ConditionalOnProperty(name = "spring.datasource.slave.url")
    public PlatformTransactionManager readTransactionManager(
            @Qualifier("readEntityManagerFactory") LocalContainerEntityManagerFactoryBean readEntityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(readEntityManagerFactory.getObject());
        // Set read-only by default
        transactionManager.setDefaultTimeout(30); // 30 seconds timeout for reads
        return transactionManager;
    }
}

/**
 * NOTE: Read/Write repository separation has been removed
 * All repositories now use the main repository package: com.wallet.repository
 * This configuration is kept for future reference but is disabled by default
 * 
 * To use read/write separation:
 * 1. Set app.repository.read-write-separation.enabled=true
 * 2. Configure slave datasource properties
 * 3. Recreate read/write repository packages if needed
 */