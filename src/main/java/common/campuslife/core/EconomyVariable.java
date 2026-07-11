package common.campuslife.core;

/**
 * 50个核心经济变量的定义。
 * 每个变量有一个key、初始值、最小值、最大值。
 * 上帝AI每5分钟计算这些变量的delta变化量。
 */
public enum EconomyVariable {

    // === 供给变量 (10个) ===
    RAW_MATERIAL_SUPPLY("raw_material_supply", 100.0, 0.0, 1000.0),
    PRODUCT_VARIETY("product_variety", 3.0, 0.0, 100.0),
    EMPLOYEE_SUPPLY("employee_supply", 20.0, 0.0, 500.0),
    BUILDING_SPACE_SUPPLY("building_space_supply", 10.0, 0.0, 1000.0),
    COMPETITOR_PRODUCT_SUPPLY("competitor_product_supply", 0.0, 0.0, 1000.0),
    FOOD_SUPPLY("food_supply", 50.0, 0.0, 1000.0),
    TOOL_SUPPLY("tool_supply", 10.0, 0.0, 500.0),
    SERVICE_SUPPLY("service_supply", 5.0, 0.0, 500.0),
    TECH_SUPPLY("tech_supply", 0.0, 0.0, 500.0),
    LUXURY_SUPPLY("luxury_supply", 0.0, 0.0, 500.0),

    // === 需求变量 (8个) ===
    CUSTOMER_PURCHASING_POWER("customer_purchasing_power", 100.0, 0.0, 1000.0),
    CUSTOMER_PREFERENCE_TREND("customer_preference_trend", 0.5, 0.0, 1.0),
    CUSTOMER_TRAFFIC("customer_traffic", 1.0, 0.0, 5.0),
    SUBSTITUTE_AVAILABILITY("substitute_availability", 0.0, 0.0, 1.0),
    NPC_POPULATION("npc_population", 30.0, 0.0, 200.0),
    NPC_AVG_WEALTH("npc_avg_wealth", 100.0, 0.0, 10000.0),
    NPC_CONSUMER_CONFIDENCE("npc_consumer_confidence", 0.7, 0.0, 1.0),
    MARKET_ATTRACTIVENESS("market_attractiveness", 0.3, 0.0, 1.0),

    // === 价格变量 (6个) ===
    RAW_MATERIAL_PRICE("raw_material_price", 5.0, 0.1, 1000.0),
    PRODUCT_MARKET_PRICE("product_market_price", 10.0, 0.1, 10000.0),
    EMPLOYEE_MARKET_SALARY("employee_market_salary", 20.0, 1.0, 5000.0),
    RENT_COST("rent_cost", 50.0, 0.0, 50000.0),
    COMPETITOR_AVG_PRICE("competitor_avg_price", 0.0, 0.0, 10000.0),
    PRICE_VOLATILITY("price_volatility", 0.1, 0.0, 1.0),

    // === 资本变量 (6个) ===
    PLAYER_CASHFLOW("player_cashflow", 1000.0, -100000.0, 1000000.0),
    COMPETITOR_CAPITAL("competitor_capital", 0.0, 0.0, 1000000.0),
    INVESTOR_CONFIDENCE("investor_confidence", 0.5, 0.0, 1.0),
    INFLATION_RATE("inflation_rate", 0.02, -0.1, 5.0),
    MONEY_SUPPLY("money_supply", 50000.0, 0.0, 10000000.0),
    CREDIT_AVAILABILITY("credit_availability", 0.8, 0.0, 1.0),

    // === 社交变量 (6个) ===
    NPC_RELATIONSHIP_DENSITY("npc_relationship_density", 0.3, 0.0, 1.0),
    WORD_OF_MOUTH_SPEED("word_of_mouth_speed", 0.5, 0.0, 1.0),
    INFORMATION_ASYMMETRY("information_asymmetry", 0.6, 0.0, 1.0),
    NPC_SATISFACTION_AVG("npc_satisfaction_avg", 50.0, 0.0, 100.0),
    NPC_SATISFACTION_VARIANCE("npc_satisfaction_variance", 10.0, 0.0, 50.0),
    SOCIAL_TENSION("social_tension", 0.2, 0.0, 1.0),

    // === 随机扰动 (4个) ===
    WEATHER("weather", 0.7, 0.0, 1.0),
    EXTERNAL_EVENT("external_event", 0.0, -1.0, 1.0),
    NPC_PERSONAL_EVENT("npc_personal_event", 0.0, -1.0, 1.0),
    BLACK_SWAN("black_swan", 0.0, -1.0, 1.0);

    private final String key;
    private final double defaultValue;
    private final double minValue;
    private final double maxValue;

    EconomyVariable(String key, double defaultValue, double minValue, double maxValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String key() { return key; }
    public double defaultValue() { return defaultValue; }
    public double minValue() { return minValue; }
    public double maxValue() { return maxValue; }

    public double clamp(double value) {
        return Math.max(minValue, Math.min(maxValue, value));
    }

    public static EconomyVariable fromKey(String key) {
        for (EconomyVariable v : values()) {
            if (v.key.equals(key)) return v;
        }
        return null;
    }
}
