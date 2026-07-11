package common.campuslife.core;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.storage.SimuSqliteStorage;
import common.cn.kafei.simukraft.storage.SimuSqliteDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 全局经济变量状态板。
 * 读写50个经济变量，持久化到SQLite。
 * 上帝AI每5分钟读取快照，计算delta，写回。
 * 规则层每tick用公式更新部分变量。
 */
public final class WorldStateBoard {

    private static final ConcurrentMap<String, Double> CURRENT_VALUES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Double> LAST_DELTAS = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    private WorldStateBoard() {}

    /**
     * 初始化：从SQLite加载，不存在的变量用默认值。
     */
    public static void initialize(Connection connection) {
        if (initialized) return;
        try {
            createTablesIfNotExist(connection);
            for (EconomyVariable var : EconomyVariable.values()) {
                Double value = loadVariable(connection, var.key());
                if (value == null) {
                    value = var.defaultValue();
                    saveVariable(connection, var.key(), value, 0.0, "init");
                }
                CURRENT_VALUES.put(var.key(), value);
                LAST_DELTAS.put(var.key(), 0.0);
            }
            initialized = true;
            SimuKraft.LOGGER.info("WorldStateBoard initialized with {} variables", EconomyVariable.values().length);
        } catch (Exception e) {
            SimuKraft.LOGGER.error("Failed to initialize WorldStateBoard", e);
        }
    }

    /**
     * 获取当前变量值（内存缓存，零延迟）。
     */
    public static double get(EconomyVariable var) {
        return CURRENT_VALUES.getOrDefault(var.key(), var.defaultValue());
    }

    public static double get(String key) {
        EconomyVariable var = EconomyVariable.fromKey(key);
        if (var != null) return get(var);
        return CURRENT_VALUES.getOrDefault(key, 0.0);
    }

    /**
     * 设置变量值（内存 + 异步写SQLite）。
     */
    public static void set(EconomyVariable var, double value, String source) {
        double clamped = var.clamp(value);
        double old = CURRENT_VALUES.getOrDefault(var.key(), var.defaultValue());
        double delta = clamped - old;
        CURRENT_VALUES.put(var.key(), clamped);
        LAST_DELTAS.put(var.key(), delta);
        // SQLite写入由GodAIService在tick结束时批量执行
    }

    /**
     * 应用delta变化量（上帝AI输出）。
     */
    public static void applyDelta(EconomyVariable var, double delta, String source) {
        double current = get(var);
        double newValue = var.clamp(current + delta);
        double actualDelta = newValue - current;
        CURRENT_VALUES.put(var.key(), newValue);
        LAST_DELTAS.put(var.key(), actualDelta);
    }

    public static double getLastDelta(EconomyVariable var) {
        return LAST_DELTAS.getOrDefault(var.key(), 0.0);
    }

    /**
     * 生成完整状态快照JSON（给上帝AI的LLM输入）。
     */
    public static String toJsonSnapshot() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (EconomyVariable var : EconomyVariable.values()) {
            if (!first) sb.append(",");
            sb.append("\"").append(var.key()).append("\":").append(String.format("%.4f", get(var)));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 批量持久化到SQLite（服务器关闭或定期保存时调用）。
     */
    public static void saveAll(Connection connection) {
        try {
            for (EconomyVariable var : EconomyVariable.values()) {
                double value = get(var);
                double delta = getLastDelta(var);
                saveVariable(connection, var.key(), value, delta, "tick");
            }
        } catch (Exception e) {
            SimuKraft.LOGGER.error("Failed to save WorldStateBoard", e);
        }
    }

    /**
     * 记录上帝AI决策日志。
     */
    public static void logGodAIDecision(Connection connection, String inputSnapshot, String outputDeltas, String llmResponse, boolean success, boolean fallbackUsed) {
        try {
            String sql = "INSERT INTO god_ai_decisions (timestamp, input_snapshot, output_deltas, llm_response, success, fallback_used) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, inputSnapshot);
                ps.setString(3, outputDeltas);
                ps.setString(4, llmResponse);
                ps.setBoolean(5, success);
                ps.setBoolean(6, fallbackUsed);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            SimuKraft.LOGGER.error("Failed to log GodAI decision", e);
        }
    }

    private static void createTablesIfNotExist(Connection connection) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS world_state (" +
                "  variable_key TEXT PRIMARY KEY," +
                "  variable_value REAL NOT NULL," +
                "  variable_delta REAL DEFAULT 0," +
                "  last_updated INTEGER NOT NULL," +
                "  updated_by TEXT DEFAULT 'rule')"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS world_state_history (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  variable_key TEXT NOT NULL," +
                "  variable_value REAL NOT NULL," +
                "  timestamp INTEGER NOT NULL)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS god_ai_decisions (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  timestamp INTEGER NOT NULL," +
                "  input_snapshot TEXT NOT NULL," +
                "  output_deltas TEXT NOT NULL," +
                "  llm_response TEXT," +
                "  success BOOLEAN DEFAULT TRUE," +
                "  fallback_used BOOLEAN DEFAULT FALSE)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS npc_memory (" +
                "  npc_uuid TEXT PRIMARY KEY," +
                "  personality_json TEXT NOT NULL," +
                "  recent_interactions_json TEXT," +
                "  player_impression INTEGER DEFAULT 0," +
                "  current_goal TEXT," +
                "  relationships_json TEXT," +
                "  last_llm_call INTEGER DEFAULT 0)"
            );
        }
    }

    private static Double loadVariable(Connection connection, String key) throws Exception {
        String sql = "SELECT variable_value FROM world_state WHERE variable_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return null;
    }

    private static void saveVariable(Connection connection, String key, double value, double delta, String source) throws Exception {
        String sql = "INSERT INTO world_state (variable_key, variable_value, variable_delta, last_updated, updated_by) " +
                     "VALUES (?, ?, ?, ?, ?) ON CONFLICT(variable_key) DO UPDATE SET " +
                     "variable_value = excluded.variable_value, variable_delta = excluded.variable_delta, " +
                     "last_updated = excluded.last_updated, updated_by = excluded.updated_by";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setDouble(2, value);
            ps.setDouble(3, delta);
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, source);
            ps.executeUpdate();
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
