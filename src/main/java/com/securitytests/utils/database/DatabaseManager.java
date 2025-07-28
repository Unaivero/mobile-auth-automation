package com.securitytests.utils.database;

import com.securitytests.utils.logging.StructuredLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database manager for test data management and result persistence
 * Provides connection pooling and query execution capabilities
 */
public class DatabaseManager {
    private static final StructuredLogger logger = new StructuredLogger(DatabaseManager.class);
    private static volatile DatabaseManager instance;
    private final HikariDataSource dataSource;
    private final Map<String, PreparedStatement> preparedStatementCache = new ConcurrentHashMap<>();
    
    private DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getProperty("db.url", "jdbc:postgresql://localhost:5432/mobile_auth_testing"));
        config.setUsername(getProperty("db.username", "test_user"));
        config.setPassword(getProperty("db.password", "test_password"));
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        this.dataSource = new HikariDataSource(config);
        logger.info("Database connection pool initialized");
    }
    
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get a database connection from the pool
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Execute a query and return results as a list of maps
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... parameters) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
            
            logger.debug("Executed query: {} with {} results", sql, results.size());
        } catch (SQLException e) {
            logger.error("Error executing query: " + sql, e);
            throw new RuntimeException("Database query failed", e);
        }
        
        return results;
    }
    
    /**
     * Execute an update statement (INSERT, UPDATE, DELETE)
     */
    public int executeUpdate(String sql, Object... parameters) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            int affectedRows = stmt.executeUpdate();
            logger.debug("Executed update: {} affecting {} rows", sql, affectedRows);
            return affectedRows;
            
        } catch (SQLException e) {
            logger.error("Error executing update: " + sql, e);
            throw new RuntimeException("Database update failed", e);
        }
    }
    
    /**
     * Execute an insert statement and return the generated key
     */
    public Object executeInsertWithGeneratedKey(String sql, Object... parameters) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Set parameters
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Object key = generatedKeys.getObject(1);
                        logger.debug("Executed insert with generated key: {}", key);
                        return key;
                    }
                }
            }
            
            throw new RuntimeException("Insert statement did not return a generated key");
            
        } catch (SQLException e) {
            logger.error("Error executing insert: " + sql, e);
            throw new RuntimeException("Database insert failed", e);
        }
    }
    
    /**
     * Execute multiple statements in a transaction
     */
    public void executeTransaction(List<String> sqlStatements, List<Object[]> parametersList) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            
            for (int i = 0; i < sqlStatements.size(); i++) {
                String sql = sqlStatements.get(i);
                Object[] parameters = i < parametersList.size() ? parametersList.get(i) : new Object[0];
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int j = 0; j < parameters.length; j++) {
                        stmt.setObject(j + 1, parameters[j]);
                    }
                    stmt.executeUpdate();
                }
            }
            
            conn.commit();
            logger.info("Transaction executed successfully with {} statements", sqlStatements.size());
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    logger.error("Error rolling back transaction", rollbackEx);
                }
            }
            logger.error("Error executing transaction", e);
            throw new RuntimeException("Transaction failed", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
    
    /**
     * Check if the database connection is healthy
     */
    public boolean isHealthy() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("active_connections", dataSource.getHikariPoolMXBean().getActiveConnections());
            stats.put("idle_connections", dataSource.getHikariPoolMXBean().getIdleConnections());
            stats.put("total_connections", dataSource.getHikariPoolMXBean().getTotalConnections());
            stats.put("threads_awaiting_connection", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            
            // Get table row counts
            List<Map<String, Object>> tableStats = executeQuery(
                "SELECT schemaname, tablename, n_tup_ins as inserts, n_tup_upd as updates, " +
                "n_tup_del as deletes, n_live_tup as live_tuples " +
                "FROM pg_stat_user_tables ORDER BY n_live_tup DESC"
            );
            stats.put("table_statistics", tableStats);
            
        } catch (Exception e) {
            logger.error("Error getting database statistics", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Close all connections and shutdown the pool
     */
    public void shutdown() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Database connection pool shutdown completed");
            }
        } catch (Exception e) {
            logger.error("Error shutting down database connection pool", e);
        }
    }
    
    private String getProperty(String key, String defaultValue) {
        return System.getProperty(key, System.getenv(key.toUpperCase().replace(".", "_")) != null 
            ? System.getenv(key.toUpperCase().replace(".", "_")) : defaultValue);
    }
}