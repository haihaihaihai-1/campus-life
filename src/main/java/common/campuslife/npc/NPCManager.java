package common.campuslife.npc;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.core.EconomyVariable;
import common.campuslife.core.WorldStateBoard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tier 2 客户NPC管理器。
 * 
 * 不创建新的EntityType，而是复用simukraft的CitizenEntity，
 * 通过外部服务控制其行为（与simukraft的CitizenEmploymentService相同架构模式）。
 * 
 * 客户NPC行为：
 * - 饥饿度上升 → 寻找最近食品摊位 → 购买 → 满意度更新 → 口碑传播
 * - 所有决策使用规则（WorldStateBoard参数 + 性格向量），零LLM调用
 * - 每个游戏tick更新所有NPC的状态
 * 
 * 活跃数量：30个（可通过配置调整）
 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class NPCManager {

    /** 客户NPC数据缓存（不持久化到CitizenData，管理在独立的SQLite表中） */
    private static final Map<UUID, CustomerData> CUSTOMERS = new ConcurrentHashMap<>();
    
    /** 当前活跃NPC数量 */
    private static final int MAX_CUSTOMERS = 30;
    
    /** 生成间隔（tick） */
    private static final int SPAWN_INTERVAL = 24000; // 每天一次
    
    /** 上次生成时间 */
    private static long lastSpawnTick = 0;
    
    /** 初始资金（每个客户NPC每天） */
    private static final float CUSTOMER_DAILY_BUDGET = 100.0f;
    
    private NPCManager() {}
    
    /**
     * 注册管理器，监听ServerTick事件。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(NPCManager.class);
        SimuKraft.LOGGER.info("NPCManager registered (Tier 2 customer NPCs: max={})", MAX_CUSTOMERS);
    }
    
    /**
     * Server tick事件 - 驱动所有NPC行为。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        
        long tick = level.getGameTime();
        
        // 每天生成新客户NPC
        if (tick - lastSpawnTick >= SPAWN_INTERVAL) {
            spawnCustomers(level);
            lastSpawnTick = tick;
        }
        
        // 每20tick（1秒）更新所有NPC状态
        if (tick % 20 == 0) {
            tickAllCustomers(level);
        }
    }
    
    /**
     * 生成客户NPC。
     * 复用simukraft的CitizenService.spawnCitizen来创建基础实体，
     * 然后附加我们的CustomerData。
     */
    private static void spawnCustomers(ServerLevel level) {
        int currentCount = CUSTOMERS.size();
        int toSpawn = Math.min(5, MAX_CUSTOMERS - currentCount); // 每天最多生成5个
        
        if (toSpawn <= 0) return;
        
        var random = level.random;
        
        for (int i = 0; i < toSpawn; i++) {
            // 随机生成位置（在玩家附近）
            Player player = level.getRandomPlayer();
            if (player == null) continue;
            
            var spawnPos = player.blockPosition().offset(
                random.nextInt(20) - 10, 0, random.nextInt(20) - 10
            );
            
            // 使用simukraft的API创建CitizenEntity
            // CitizenService.spawnCitizen 要求 cityId 非 null，否则返回 Optional.empty()
            // 传入一个随机 UUID 作为"虚拟城市"ID，让 NPC 能实际生成
            var citizenOpt = common.cn.kafei.simukraft.citizen.CitizenService.spawnCitizen(
                level,
                new Vec3(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ()),
                UUID.randomUUID(),
                true
            );
            
            if (citizenOpt.isPresent()) {
                UUID npcId = citizenOpt.get().getUUID();
                
                // 创建客户NPC数据
                CustomerData data = new CustomerData(
                    npcId,
                    generateRandomName(),
                    0.3f + random.nextFloat() * 0.7f, // priceSensitivity
                    random.nextFloat(),                 // brandLoyalty
                    random.nextFloat(),                 // innovationOpenness
                    random.nextFloat()                  // socialInfluence
                );
                
                CUSTOMERS.put(npcId, data);
                
                SimuKraft.LOGGER.info("Spawned customer NPC '{}' ({} of {})", 
                    data.name, CUSTOMERS.size(), MAX_CUSTOMERS);
            }
        }
    }
    
    /**
     * 更新所有客户NPC的状态。
     */
    private static void tickAllCustomers(ServerLevel level) {
        if (CUSTOMERS.isEmpty()) return;
        
        // 获取经济环境参数
        float traffic = (float) WorldStateBoard.get(EconomyVariable.CUSTOMER_TRAFFIC);
        float avgSatisfaction = (float) WorldStateBoard.get(EconomyVariable.NPC_SATISFACTION_AVG);
        
        for (Map.Entry<UUID, CustomerData> entry : CUSTOMERS.entrySet()) {
            CustomerData customer = entry.getValue();
            
            // 更新状态
            customer.tick();
            
            // 饥饿度 > 60 且冷却结束 → 尝试购买
            if (customer.hunger > 60 && customer.purchaseCooldown <= 0) {
                tryPurchase(level, customer);
            }
            
            // 更新世界状态板中的平均满意度
            customer.updateWorldState(traffic, avgSatisfaction);
        }
    }
    
    /**
     * 尝试购买食品。
     * 实际游戏中，NPC会移动到最近的交易机器位置并触发购买。
     * 当前简化的模拟：直接计算购买结果。
     */
    private static void tryPurchase(ServerLevel level, CustomerData customer) {
        // 评估附近的"商店"（真实实现需要检测附近的LC交易机器）
        float[] shopScore = evaluateNearbyShops(customer);
        
        if (shopScore[0] < 0.2f) {
            // 没有合适的商店
            customer.hunger = Math.max(0, customer.hunger - 5); // 等待后饥饿度部分降低
            customer.purchaseCooldown = 600; // 30秒冷却
            return;
        }
        
        String shopName = "shop_" + customer.id.toString().substring(0, 4);
        float price = 5 + (1 - customer.priceSensitivity) * 10;
        float quality = shopScore[1];
        
        // 计算满意度变化
        float expectedQuality = 0.5f + price / 20.0f; // 价格越高，期望越高
        float actualQuality = quality * (0.8f + level.random.nextFloat() * 0.4f);
        
        if (actualQuality >= expectedQuality) {
            customer.satisfaction = Math.min(100, customer.satisfaction + 10);
        } else {
            customer.satisfaction = Math.max(0, customer.satisfaction - 5);
        }
        
        customer.hunger = Math.max(0, customer.hunger - 30);
        customer.purchaseCooldown = 6000; // 5分钟冷却
        customer.totalPurchases++;
        customer.lastShopVisited = shopName;
        
        SimuKraft.LOGGER.debug("Customer {}: purchased from {} (price={}, sat={})",
            customer.name, shopName, String.format("%.1f", price), 
            String.format("%.1f", customer.satisfaction));
    }
    
    /**
     * 评估附近商店。
     * 返回数组：[评分, 品质]
     * 真实实现需要扫描附近的LC交易机器。
     */
    private static float[] evaluateNearbyShops(CustomerData customer) {
        // 基于WorldStateBoard参数生成商店品质
        float marketQuality = (float) WorldStateBoard.get(EconomyVariable.PRODUCT_MARKET_PRICE) / 10.0f;
        float traffic = (float) WorldStateBoard.get(EconomyVariable.CUSTOMER_TRAFFIC);
        
        float score = marketQuality * traffic * (0.5f + (float) Math.random() * 0.5f);
        float quality = Math.max(0.1f, Math.min(1.0f, score));
        
        return new float[]{Math.max(0, score), quality};
    }
    
    /**
     * 获取所有客户NPC的UUID列表（用于渲染层查找实体）。
     */
    public static Collection<UUID> getCustomerUUIDs() {
        return Collections.unmodifiableCollection(CUSTOMERS.keySet());
    }
    
    /**
     * 根据UUID获取客户数据。
     */
    public static CustomerData getCustomer(UUID uuid) {
        return CUSTOMERS.get(uuid);
    }
    
    /**
     * 获取当前客户数量。
     */
    public static int getCustomerCount() {
        return CUSTOMERS.size();
    }
    
    /**
     * 玩家右键点击客户NPC时触发。
     */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Entity target = event.getTarget();
        UUID targetUUID = target.getUUID();

        CustomerData customer = CUSTOMERS.get(targetUUID);
        if (customer == null) return;

        Player player = event.getEntity();

        // 显示NPC信息
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "顾客 " + customer.name + " | 满意度: " + (int) customer.satisfaction + " | 今日购买: " + customer.totalPurchases
            ),
            true
        );
    }
    
    /**
     * 随机生成中文名字（用于NPC显示）。
     */
    private static final String[] FAMILY_NAMES = {"张", "王", "李", "赵", "陈", "刘", "杨", "黄", "周", "吴"};
    private static final String[] GIVEN_NAMES = {"伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋", 
                              "勇", "艳", "杰", "涛", "明", "超", "秀英", "华", "慧", "鑫"};
    
    private static String generateRandomName() {
        return FAMILY_NAMES[(int) (Math.random() * FAMILY_NAMES.length)] + 
               GIVEN_NAMES[(int) (Math.random() * GIVEN_NAMES.length)];
    }
    
    /**
     * 客户NPC的运行时数据（存储在内存，定期存到SQLite）。
     */
    public static final class CustomerData {
        public final UUID id;
        public final String name;
        
        // 性格向量（不变）
        public final float priceSensitivity;
        public final float brandLoyalty;
        public final float innovationOpenness;
        public final float socialInfluence;
        
        // 动态状态
        public float hunger = 50f;
        public float satisfaction = 50f;
        public float budget = CUSTOMER_DAILY_BUDGET;
        public float purchaseCooldown = 0f;
        public String lastShopVisited = "";
        public int totalPurchases = 0;
        public long age = 0; // NPC存在天数
        
        public CustomerData(UUID id, String name, float priceSensitivity, 
                           float brandLoyalty, float innovationOpenness, float socialInfluence) {
            this.id = id;
            this.name = name;
            this.priceSensitivity = priceSensitivity;
            this.brandLoyalty = brandLoyalty;
            this.innovationOpenness = innovationOpenness;
            this.socialInfluence = socialInfluence;
        }
        
        /**
         * 每20tick更新一次。
         */
        public void tick() {
            // 饥饿度上升
            hunger = Math.min(100f, hunger + 0.02f);
            // 购买冷却减少
            if (purchaseCooldown > 0) purchaseCooldown--;
            // 自然满意度衰减
            satisfaction = Math.max(0, satisfaction - 0.001f);
            // 年龄增长（每天+1）
            age++;
            // 每天预算重置
            if (age % 24000 == 0 && age > 0) {
                budget = CUSTOMER_DAILY_BUDGET;
            }
        }
        
        /**
         * 更新WorldStateBoard中的全局参数。
         */
        public void updateWorldState(float traffic, float avgSatisfaction) {
            // 通过加权平均更新全局满意度
            float currentAvg = (float) WorldStateBoard.get(EconomyVariable.NPC_SATISFACTION_AVG);
            float newAvg = currentAvg * 0.99f + satisfaction * 0.01f;
            WorldStateBoard.set(EconomyVariable.NPC_SATISFACTION_AVG, newAvg, "customer_npc");
        }
    }
}
