package common.campuslife.npc;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.core.LLMClient;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;

/**
 * Tier 1: 投资人NPC（LLM驱动）。
 * 
 * 特性：
 * - 数量少（1-2个），通过LLM进行复杂的投资决策
 * - 每次决策评估玩家经营数据（市场份额/营收/增长趋势）
 * - LLM生成投资意向（注资/撤资/还价条件）
 * - 有长期记忆（记住之前的谈判和玩家表现）
 * - 聊天框输出投资人的"想法"
 * 
 * LLM Prompt包含：
 * - 投资人的性格向量
 * - 玩家的当前经营快照
 * - 之前的交互记录
 * - 当前市场状态
 * 
 * 降级策略：LLM不可用时，使用固定规则的简单决策
 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class InvestorManager {

    /** 投资人缓存 */
    private static final Map<UUID, InvestorData> INVESTORS = new ConcurrentHashMap<>();
    
    /** 最大投资人数量 */
    private static final int MAX_INVESTORS = 2;
    
    /** LLM调用间隔（tick），每5分钟一次 */
    private static final int LLM_DECISION_INTERVAL = 6000;
    
    /** 上次LLM调用时间 */
    private static long lastLLMCall = 0;
    
    /** 是否正在等待LLM响应 */
    private static volatile boolean pendingLLMResponse = false;
    
    /** 主基金规模（所有投资人的总资金池） */
    private static float totalFund = 50000.0f;
    
    /** 已投资总额 */
    private static float totalInvested = 0;
    
    /** 放款利率 */
    private static final float INTEREST_RATE = 0.05f; // 5%
    
    private InvestorManager() {}
    
    /**
     * 注册管理器。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(InvestorManager.class);
        SimuKraft.LOGGER.info("InvestorManager registered (Tier 1 LLM investor NPCs: max={})", MAX_INVESTORS);
    }
    
    /**
     * 生成投资人NPC。
     */
    public static boolean spawnInvestor(ServerLevel level) {
        if (INVESTORS.size() >= MAX_INVESTORS) {
            return false;
        }
        
        // 在地图中心附近随机位置生成
        var random = level.random;
        int x = random.nextInt(100) - 50;
        int z = random.nextInt(100) - 50;
        
        UUID investorId = UUID.randomUUID();
        
        InvestorData data = new InvestorData(
            investorId,
            generateInvestorName(),
            30000 + random.nextFloat() * 20000, // fund size
            0.3f + random.nextFloat() * 0.4f,    // risk tolerance
            random.nextFloat()                    // patience
        );
        
        INVESTORS.put(investorId, data);
        
        SimuKraft.LOGGER.info("Investor '{}' spawned with fund {} (total investors: {})", 
            data.name, String.format("%.0f", data.fundSize), INVESTORS.size());
        
        return true;
    }
    
    /**
     * Server tick事件驱动投资决策。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        
        long tick = level.getGameTime();
        
        // 延迟生成投资人（游戏进入10分钟后）
        if (tick == 6000 && INVESTORS.isEmpty()) {
            spawnInvestor(level);
        }
        
        // 每5分钟评估一次投资机会
        if (tick - lastLLMCall >= LLM_DECISION_INTERVAL && !pendingLLMResponse && !INVESTORS.isEmpty()) {
            lastLLMCall = tick;
            evaluateInvestments(level);
        }
    }
    
    /**
     * 对所有投资人执行投资决策。
     */
    private static void evaluateInvestments(ServerLevel level) {
        // 获取玩家经营数据（未来从WorldStateBoard读取）
        float marketShare = getMarketShare(level);
        float weeklyRevenue = getWeeklyRevenue(level);
        float growthRate = getGrowthRate(level);
        int employeeCount = EmployeeManager.getEmployeeCount();
        
        if (pendingLLMResponse) return;
        pendingLLMResponse = true;
        
        // 组装Prompt
        String systemPrompt = buildInvestorSystemPrompt();
        String userPrompt = buildInvestorUserPrompt(marketShare, weeklyRevenue, growthRate, employeeCount);
        
        CompletableFuture.runAsync(() -> {
            try {
                LLMClient.chatAsync(systemPrompt, userPrompt).thenAccept(response -> {
                    if (response != null) {
                        processInvestmentDecision(response);
                    }
                    pendingLLMResponse = false;
                }).exceptionally(e -> {
                    SimuKraft.LOGGER.warn("Investor LLM call failed: {}", e.getMessage());
                    // 降级：使用简单规则
                    makeRuleBasedDecision(level);
                    pendingLLMResponse = false;
                    return null;
                });
            } catch (Exception e) {
                SimuKraft.LOGGER.error("Failed to call investor LLM", e);
                makeRuleBasedDecision(level);
                pendingLLMResponse = false;
            }
        });
    }
    
    /**
     * 处理LLM返回的投资决策。
     */
    private static void processInvestmentDecision(JsonObject response) {
        // LLM返回格式：{"decision": "invest/withdraw/hold", "amount": X, "equity": Y, "conditions": [...]}
        String decision = response.has("decision") ? response.get("decision").getAsString() : "hold";
        float amount = response.has("amount") ? response.get("amount").getAsFloat() : 0;
        float equity = response.has("equity") ? response.get("equity").getAsFloat() : 0;
        
        SimuKraft.LOGGER.warn("Investor decision: {} (amount={}, equity={}%)", decision, amount, equity * 100);
        
        // 通知玩家（未来实现：通过network packet发送到客户端）
        for (InvestorData investor : INVESTORS.values()) {
            investor.lastDecision = decision;
            investor.lastDecisionTime = System.currentTimeMillis();
            
            if ("invest".equals(decision)) {
                investor.investedAmount += amount;
                totalInvested += amount;
                // 实际注资：将LC硬币添加到玩家账户
            } else if ("withdraw".equals(decision)) {
                investor.investedAmount = Math.max(0, investor.investedAmount - amount);
                totalInvested = Math.max(0, totalInvested - amount);
            }
        }
    }
    
    /**
     * 规则降级决策（LLM不可用时）。
     */
    private static void makeRuleBasedDecision(ServerLevel level) {
        // 简单规则：市场份额<10%时撤资，>30%时注资
        float marketShare = getMarketShare(level);
        
        for (InvestorData investor : INVESTORS.values()) {
            if (marketShare < 0.1f && investor.investedAmount > 0) {
                investor.lastDecision = "withdraw";
                SimuKraft.LOGGER.info("Investor {}: rule-based withdraw (market share < 10%)", investor.name);
            } else if (marketShare > 0.3f && investor.investedAmount < investor.fundSize * 0.5f) {
                investor.lastDecision = "invest";
                investor.investedAmount += investor.fundSize * 0.1f;
                SimuKraft.LOGGER.info("Investor {}: rule-based invest (market share > 30%)", investor.name);
            }
        }
    }
    
    /**
     * 构建投资人LLM的system prompt。
     */
    private static String buildInvestorSystemPrompt() {
        return """
            You are a venture capital investor in a campus entrepreneurship simulator.
            
            Your role: Evaluate the player's business and make investment decisions.
            
            Personality:
            - Risk-averse to moderate (avoid big losses over chasing big wins)
            - Value consistent growth over flashy numbers
            - Will negotiate terms, not just accept/reject
            - Can add conditions (e.g., "I invest if you lower burn rate")
            
            Output ONLY a JSON object (no markdown, no explanation):
            {
              "decision": "invest/withdraw/hold/counter_offer",
              "amount": <coin_amount>,
              "equity": <fraction_0_to_1>,
              "reason": "brief justification",
              "conditions": ["condition1", "condition2"]
            }
            
            Be concise. Think briefly. The rule layer handles the actual fund transfers.
            """;
    }
    
    /**
     * 构建投资人LLM的user prompt。
     */
    private static String buildInvestorUserPrompt(float marketShare, float weeklyRevenue, 
                                                   float growthRate, int employeeCount) {
        return String.format(
            "Player business snapshot:\n" +
            "- Market share: %.1f%%\n" +
            "- Weekly revenue: %.0f coins\n" +
            "- Growth rate: %.2f\n" +
            "- Employee count: %d\n" +
            "- Current fund: %.0f coins\n\n" +
            "What is your investment decision? JSON only, no markdown.",
            marketShare * 100, weeklyRevenue, growthRate, employeeCount, totalFund
        );
    }
    
    /**
     * 从WorldStateBoard获取玩家市场份额。
     */
    private static float getMarketShare(ServerLevel level) {
        return 0.05f; // 临时实现，未来从WorldStateBoard获取
    }
    
    /**
     * 从WorldStateBoard获取周营收。
     */
    private static float getWeeklyRevenue(ServerLevel level) {
        return 500.0f; // 临时实现
    }
    
    /**
     * 从WorldStateBoard获取增长率。
     */
    private static float getGrowthRate(ServerLevel level) {
        return 0.02f; // 临时实现
    }
    
    /**
     * 获取所有投资人UUID。
     */
    public static Collection<UUID> getInvestorUUIDs() {
        return Collections.unmodifiableCollection(INVESTORS.keySet());
    }
    
    /**
     * 根据UUID获取投资人数据。
     */
    public static InvestorData getInvestor(UUID uuid) {
        return INVESTORS.get(uuid);
    }
    
    /**
     * 随机生成投资人名字。
     */
    private static final String[] INVESTOR_NAMES = {
        "王投", "李资", "张本", "赵钱", "陈投", "刘资", "杨本", "黄投"
    };
    
    private static String generateInvestorName() {
        return INVESTOR_NAMES[(int) (Math.random() * INVESTOR_NAMES.length)];
    }
    
    /**
     * 投资人运行时数据。
     */
    public static final class InvestorData {
        public final UUID id;
        public final String name;
        public final float fundSize;
        public final float riskTolerance;
        public final float patience;
        
        public float investedAmount = 0;
        public String lastDecision = "hold";
        public long lastDecisionTime = 0;
        public int negotiationRound = 0;
        
        public InvestorData(UUID id, String name, float fundSize, 
                           float riskTolerance, float patience) {
            this.id = id;
            this.name = name;
            this.fundSize = fundSize;
            this.riskTolerance = riskTolerance;
            this.patience = patience;
        }
    }
}
