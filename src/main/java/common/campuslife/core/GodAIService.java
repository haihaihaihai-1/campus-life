package common.campuslife.core;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.storage.SimuSqliteStorage;
import common.cn.kafei.simukraft.storage.SimuSqliteDatabase;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;

/**
 * 上帝AI经济物理引擎。
 * 每5分钟（6000 tick）执行一次：
 * 1. 执行规则层公式更新
 * 2. 组装经济摘要
 * 3. 异步调用MiMo-V2.5
 * 4. 解析LLM返回的涌现性扰动，应用到WorldStateBoard
 * 5. LLM不可用时用规则降级
 * 每600 tick（30秒）批量持久化到SQLite。
 */
public final class GodAIService {

    private static final int LLM_TICK_INTERVAL = 6000;  // 5分钟
    private static final int SAVE_INTERVAL = 600;       // 30秒
    private static final int INIT_DELAY = 200;          // 10秒后初始化

    private static final AtomicInteger tickCounter = new AtomicInteger(0);
    private static volatile boolean pendingLLMResponse = false;
    private static volatile Connection dbConnection;
    private static volatile boolean dbInitialized = false;

    private GodAIService() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GodAIService::onServerTick);
        SimuKraft.LOGGER.info("GodAIService registered");
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;

        int tick = tickCounter.incrementAndGet();

        // 延迟初始化 (用 >= 防止跳过)
        if (!dbInitialized && tick >= INIT_DELAY) {
            initializeDB(server);
        }

        if (!WorldStateBoard.isInitialized()) return;

        // 每5分钟执行一次经济计算
        if (tick % LLM_TICK_INTERVAL == 0) {
            executeEconomyTick(server);
        }

        // 每30秒持久化
        if (tick % SAVE_INTERVAL == 0) {
            saveToDB();
        }
    }

    private static void initializeDB(MinecraftServer server) {
        try {
            SimuSqliteStorage storage = SimuSqliteStorage.open(server);
            if (storage != null) {
                dbConnection = SimuSqliteDatabase.open(server).openConnection();
                WorldStateBoard.initialize(dbConnection);
                dbInitialized = true;
                SimuKraft.LOGGER.info("GodAIService: DB connection established, WorldStateBoard initialized");
            }
        } catch (Exception e) {
            SimuKraft.LOGGER.error("GodAIService: Failed to initialize DB", e);
        }
    }

    private static void executeEconomyTick(MinecraftServer server) {
        // 1. 执行规则层公式
        VariableInteraction.tickRules();

        // 2. 如果有pending的LLM响应，跳过这次
        if (pendingLLMResponse) {
            SimuKraft.LOGGER.info("GodAI: Skipping tick, previous LLM call still pending");
            return;
        }

        // 3. 检查降级模式
        if (LLMClient.isDegraded()) {
            SimuKraft.LOGGER.info("GodAI: Running in fallback mode (degraded)");
            VariableInteraction.tickFallback();
            logDecision("{}", "{}", "fallback", false, true);
            return;
        }

        // 4. 组装经济摘要（只给关键指标，不是40个完整变量）
        String summary = buildEconomySummary();
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(summary);

        // 5. 异步调用LLM
        pendingLLMResponse = true;
        SimuKraft.LOGGER.info("GodAI: Starting LLM tick, summary size={} bytes", summary.length());

        final String summaryForLog = summary;
        LLMClient.chatAsync(systemPrompt, userPrompt)
                .thenAccept(response -> {
                    try {
                        if (response != null) {
                            applyLLMResponse(response);
                            logDecision(summaryForLog, response.toString(), response.toString(), true, false);
                            SimuKraft.LOGGER.info("GodAI: LLM response applied successfully");
                        } else {
                            VariableInteraction.tickFallback();
                            logDecision(summaryForLog, "{}", "null", false, true);
                            SimuKraft.LOGGER.warn("GodAI: LLM returned null, using fallback");
                        }
                    } catch (Exception e) {
                        SimuKraft.LOGGER.error("GodAI: Failed to apply LLM response", e);
                        VariableInteraction.tickFallback();
                        logDecision(summaryForLog, "{}", "error: " + e.getMessage(), false, true);
                    } finally {
                        pendingLLMResponse = false;
                    }
                });
    }

    /**
     * 上帝AI输出的是涌现性扰动，规则层将这些扰动转化为数值变化。
     */
    private static void applyLLMResponse(JsonObject response) {
        // 外部事件：影响客户流量、购买力、市场吸引力
        if (response.has("external_event")) {
            try {
                String event = response.get("external_event").getAsString();
                if (event != null && !event.isBlank()) {
                    SimuKraft.LOGGER.info("GodAI: External event -> {}", event);
                    WorldStateBoard.applyDelta(EconomyVariable.EXTERNAL_EVENT,
                        (Math.random() - 0.3) * 0.5, "llm_event");
                    WorldStateBoard.applyDelta(EconomyVariable.CUSTOMER_TRAFFIC,
                        (Math.random() - 0.5) * 0.4, "llm_event");
                }
            } catch (Exception ignored) {}
        }

        // 偏好趋势：NPC偏好向量变化
        if (response.has("preference_trend")) {
            try {
                String trend = response.get("preference_trend").getAsString();
                if (trend != null && !trend.isBlank()) {
                    SimuKraft.LOGGER.info("GodAI: Preference trend -> {}", trend);
                    WorldStateBoard.applyDelta(EconomyVariable.CUSTOMER_PREFERENCE_TREND,
                        (Math.random() - 0.5) * 0.2, "llm_trend");
                    WorldStateBoard.applyDelta(EconomyVariable.SUBSTITUTE_AVAILABILITY,
                        Math.random() * 0.15, "llm_trend");
                }
            } catch (Exception ignored) {}
        }

        // 投资人情绪变化
        if (response.has("investor_sentiment_shift")) {
            try {
                String shift = response.get("investor_sentiment_shift").getAsString();
                if (shift != null && !shift.isBlank()) {
                    double delta = 0;
                    if (shift.contains("optimistic")) delta = 0.1;
                    else if (shift.contains("pessimistic")) delta = -0.1;
                    if (delta != 0) {
                        WorldStateBoard.applyDelta(EconomyVariable.INVESTOR_CONFIDENCE, delta, "llm_sentiment");
                        SimuKraft.LOGGER.info("GodAI: Investor sentiment -> {} ({})", shift, delta);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 天气变化
        if (response.has("weather_shift")) {
            try {
                String weather = response.get("weather_shift").getAsString();
                if (weather != null && !weather.isBlank()) {
                    double delta = 0;
                    if (weather.contains("improving")) delta = 0.1;
                    else if (weather.contains("worsening")) delta = -0.1;
                    if (delta != 0) {
                        WorldStateBoard.applyDelta(EconomyVariable.WEATHER, delta, "llm_weather");
                    }
                }
            } catch (Exception ignored) {}
        }

        // 黑天鹅事件
        if (response.has("black_swan")) {
            try {
                String swan = response.get("black_swan").getAsString();
                if (swan != null && !swan.isBlank()) {
                    SimuKraft.LOGGER.warn("GodAI: Black swan event -> {}", swan);
                    WorldStateBoard.applyDelta(EconomyVariable.BLACK_SWAN,
                        (Math.random() > 0.5 ? 1 : -1) * (0.4 + Math.random() * 0.4), "llm_swan");
                    WorldStateBoard.applyDelta(EconomyVariable.CUSTOMER_PURCHASING_POWER,
                        (Math.random() - 0.5) * 0.5, "llm_swan_collateral");
                    WorldStateBoard.applyDelta(EconomyVariable.INVESTOR_CONFIDENCE,
                        (Math.random() - 0.7) * 0.4, "llm_swan_collateral");
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * 生成经济摘要（给上帝AI看的，只包含有意义的趋势指标）。
     */
    private static String buildEconomySummary() {
        double traffic = WorldStateBoard.get(EconomyVariable.CUSTOMER_TRAFFIC);
        double purchasingPower = WorldStateBoard.get(EconomyVariable.CUSTOMER_PURCHASING_POWER);
        double investorConf = WorldStateBoard.get(EconomyVariable.INVESTOR_CONFIDENCE);
        double satisfaction = WorldStateBoard.get(EconomyVariable.NPC_SATISFACTION_AVG);
        double inflation = WorldStateBoard.get(EconomyVariable.INFLATION_RATE);
        double marketAttr = WorldStateBoard.get(EconomyVariable.MARKET_ATTRACTIVENESS);
        double rawPrice = WorldStateBoard.get(EconomyVariable.RAW_MATERIAL_PRICE);
        double productPrice = WorldStateBoard.get(EconomyVariable.PRODUCT_MARKET_PRICE);
        double socialTension = WorldStateBoard.get(EconomyVariable.SOCIAL_TENSION);

        return String.format(
            "Customer traffic: %.2f | Purchasing power: %.1f | Investor confidence: %.2f | " +
            "NPC satisfaction: %.1f | Inflation: %.2f%% | Market attractiveness: %.2f | " +
            "Raw material price: %.1f | Product price: %.1f | Social tension: %.2f",
            traffic, purchasingPower, investorConf, satisfaction,
            inflation * 100, marketAttr, rawPrice, productPrice, socialTension
        );
    }

    private static String buildSystemPrompt() {
        return """
            You are the "God AI" — an emergent economic physics engine for a campus entrepreneurship simulator.

            Your role is NOT to calculate price/supply/deltas. Those are deterministic formulas handled by the rule layer.

            Your role is to generate EMERGENT PERTURBATIONS that the rule layer cannot produce:
            - External events (policy changes, weather shifts, competitor innovations, technology breakthroughs)
            - Consumer preference trend shifts (new tastes, campus culture changes, viral trends)
            - Rare black swan events (supply chain disruptions, sudden market crashes)
            - Investor sentiment shifts (confidence boom or panic)

            Think briefly, then output ONLY a JSON object (no markdown, no explanation):

            {
              "external_event": "brief description of an external event this cycle (omit or empty string if none)",
              "preference_trend": "brief description of how consumer preferences are shifting (omit if no significant shift)",
              "black_swan": "brief description of a rare high-impact event (omit or empty string if none, <5% chance)",
              "investor_sentiment_shift": "slightly more optimistic/slightly more pessimistic/neutral",
              "weather_shift": "improving/worsening/stable"
            }

            Be concise. Omit fields that are not relevant this cycle. Most cycles should be calm.
            The rule layer will take your perturbations and translate them into variable changes.
            """;
    }

    private static String buildUserPrompt(String summary) {
        return "Current economic summary:\n" + summary + "\n\nGenerate emergent perturbations for this cycle. Output JSON only, no markdown.";
    }

    private static void saveToDB() {
        if (dbConnection != null && WorldStateBoard.isInitialized()) {
            try {
                WorldStateBoard.saveAll(dbConnection);
            } catch (Exception e) {
                SimuKraft.LOGGER.error("GodAI: Failed to save to DB", e);
            }
        }
    }

    private static void logDecision(String inputSnapshot, String outputDeltas, String llmResponse, boolean success, boolean fallbackUsed) {
        if (dbConnection != null) {
            try {
                WorldStateBoard.logGodAIDecision(dbConnection, inputSnapshot, outputDeltas, llmResponse, success, fallbackUsed);
            } catch (Exception e) {
                SimuKraft.LOGGER.error("GodAI: Failed to log decision", e);
            }
        }
    }
}
