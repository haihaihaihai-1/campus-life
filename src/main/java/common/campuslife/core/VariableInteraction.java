package common.campuslife.core;

/**
 * 变量交互规则（公式层，非LLM）。
 * 每个tick由GodAIService调用，用确定性公式更新部分变量。
 * 这些公式模拟基本的供给-需求-价格关系。
 */
public final class VariableInteraction {

    private VariableInteraction() {}

    /**
     * 执行一次规则层计算，更新依赖性变量。
     * 被GodAIService在每个5分钟tick的LLM调用前执行。
     */
    public static void tickRules() {
        // === 供给-价格关系 ===
        double rawSupply = WorldStateBoard.get(EconomyVariable.RAW_MATERIAL_SUPPLY);
        double rawDemand = WorldStateBoard.get(EconomyVariable.FOOD_SUPPLY) + WorldStateBoard.get(EconomyVariable.TOOL_SUPPLY);
        double volatility = WorldStateBoard.get(EconomyVariable.PRICE_VOLATILITY);
        if (rawSupply > 0 && rawDemand > 0) {
            double ratio = rawDemand / rawSupply;
            double newPrice = 5.0 * (1.0 + (ratio - 1.0) * 0.1 * (1.0 + volatility));
            WorldStateBoard.set(EconomyVariable.RAW_MATERIAL_PRICE, newPrice, "rule");
        }

        // === 客户流量 ===
        double weather = WorldStateBoard.get(EconomyVariable.WEATHER);
        double externalEvent = WorldStateBoard.get(EconomyVariable.EXTERNAL_EVENT);
        double blackSwan = WorldStateBoard.get(EconomyVariable.BLACK_SWAN);
        double baseTraffic = 1.0 * weather * (1.0 + externalEvent * 0.3) * (1.0 + blackSwan * 0.4);
        baseTraffic = Math.max(0.1, baseTraffic);
        WorldStateBoard.set(EconomyVariable.CUSTOMER_TRAFFIC, baseTraffic, "rule");

        // === NPC购买力 ===
        double wealth = WorldStateBoard.get(EconomyVariable.NPC_AVG_WEALTH);
        double confidence = WorldStateBoard.get(EconomyVariable.NPC_CONSUMER_CONFIDENCE);
        WorldStateBoard.set(EconomyVariable.CUSTOMER_PURCHASING_POWER, wealth * confidence, "rule");

        // === 员工薪资市场 ===
        double empSupply = WorldStateBoard.get(EconomyVariable.EMPLOYEE_SUPPLY);
        double empDemand = 10.0; // 基础需求
        if (empSupply > 0) {
            double ratio = empDemand / empSupply;
            double newSalary = 20.0 * (1.0 + (ratio - 1.0) * 0.2);
            WorldStateBoard.set(EconomyVariable.EMPLOYEE_MARKET_SALARY, newSalary, "rule");
        }

        // === 通胀 ===
        double moneySupply = WorldStateBoard.get(EconomyVariable.MONEY_SUPPLY);
        double goodsSupply = rawSupply + WorldStateBoard.get(EconomyVariable.FOOD_SUPPLY) + WorldStateBoard.get(EconomyVariable.TOOL_SUPPLY);
        if (goodsSupply > 0) {
            double inflation = (moneySupply - goodsSupply * 100) / (goodsSupply * 100) * 0.01;
            inflation = Math.max(-0.05, Math.min(0.5, inflation));
            WorldStateBoard.set(EconomyVariable.INFLATION_RATE, inflation, "rule");
        }

        // === 投资人信心 ===
        double playerPerf = 0.5; // 默认中性
        double playerCashflow = WorldStateBoard.get(EconomyVariable.PLAYER_CASHFLOW);
        if (playerCashflow > 1000) playerPerf = 0.8;
        else if (playerCashflow < 0) playerPerf = 0.2;
        double macroEconomy = 0.5 + externalEvent * 0.2;
        double industryTrend = WorldStateBoard.get(EconomyVariable.MARKET_ATTRACTIVENESS);
        double investorConfidence = macroEconomy * 0.3 + industryTrend * 0.3 + playerPerf * 0.4;
        WorldStateBoard.set(EconomyVariable.INVESTOR_CONFIDENCE, investorConfidence, "rule");

        // === 市场吸引力 ===
        double totalActivity = WorldStateBoard.get(EconomyVariable.CUSTOMER_TRAFFIC) * WorldStateBoard.get(EconomyVariable.NPC_POPULATION);
        double attractiveness = Math.min(1.0, totalActivity / 200.0);
        WorldStateBoard.set(EconomyVariable.MARKET_ATTRACTIVENESS, attractiveness, "rule");

        // === 社交变量 ===
        double satAvg = WorldStateBoard.get(EconomyVariable.NPC_SATISFACTION_AVG);
        double satVar = WorldStateBoard.get(EconomyVariable.NPC_SATISFACTION_VARIANCE);
        double socialTension = Math.max(0, (50 - satAvg) / 50.0) * 0.5 + satVar / 50.0 * 0.5;
        WorldStateBoard.set(EconomyVariable.SOCIAL_TENSION, socialTension, "rule");

        double womSpeed = (satAvg / 100.0) * WorldStateBoard.get(EconomyVariable.NPC_RELATIONSHIP_DENSITY);
        WorldStateBoard.set(EconomyVariable.WORD_OF_MOUTH_SPEED, womSpeed, "rule");

        // === 天气随机变化 ===
        double weatherDelta = (Math.random() - 0.5) * 0.2;
        WorldStateBoard.set(EconomyVariable.WEATHER, weather + weatherDelta, "rule");

        // === NPC个人事件随机 ===
        if (Math.random() < 0.1) {
            double event = (Math.random() - 0.5) * 0.4;
            WorldStateBoard.set(EconomyVariable.NPC_PERSONAL_EVENT, event, "rule");
        } else {
            WorldStateBoard.set(EconomyVariable.NPC_PERSONAL_EVENT, 0.0, "rule");
        }

        // === 黑天鹅（5%概率）===
        if (Math.random() < 0.05) {
            double swan = (Math.random() - 0.5) * 1.0;
            WorldStateBoard.set(EconomyVariable.BLACK_SWAN, swan, "rule");
            if (Math.abs(swan) > 0.5) {
                common.cn.kafei.simukraft.SimuKraft.LOGGER.info("Black swan event triggered: {}", swan);
            }
        } else {
            WorldStateBoard.set(EconomyVariable.BLACK_SWAN, 0.0, "rule");
        }
    }

    /**
     * 降级模式下的计算（LLM不可用时）。
     * 用更保守的规则公式模拟上帝AI的决策。
     */
    public static void tickFallback() {
        tickRules();

        // 降级模式额外处理：趋势漂移
        double trend = WorldStateBoard.get(EconomyVariable.CUSTOMER_PREFERENCE_TREND);
        double trendDelta = (Math.random() - 0.5) * 0.1;
        WorldStateBoard.set(EconomyVariable.CUSTOMER_PREFERENCE_TREND, trend + trendDelta, "fallback");

        // 外部事件降级生成
        if (Math.random() < 0.15) {
            double event = (Math.random() - 0.5) * 0.6;
            WorldStateBoard.set(EconomyVariable.EXTERNAL_EVENT, event, "fallback");
        } else {
            WorldStateBoard.set(EconomyVariable.EXTERNAL_EVENT, 0.0, "fallback");
        }
    }
}
