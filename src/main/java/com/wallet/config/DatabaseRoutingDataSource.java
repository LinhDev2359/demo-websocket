package com.wallet.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Custom DataSource router for master-slave configuration
 * Routes to slave for read-only transactions, master for write transactions
 */
public class DatabaseRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // Route to slave for read-only transactions
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        if (isReadOnly) {
            return "slave";
        } else {
            return "master";
        }
    }
}