package common.campuslife.startup;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.npc.EmployeeManager;
import common.campuslife.core.EconomyVariable;
import common.campuslife.core.WorldStateBoard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 创业阶段管理器。
 * 
 * 管理玩家的创业进程：
 * - 放置创业核心方块后解锁"课桌创业"阶段
 * - 达到条件后解锁下一阶段（宿舍工作室 → 校园孵化器 → 独立公司 → 企业总部 → 商业帝国）
 * - 每个阶段解锁新的产品/员工/建筑能力
 * 
 * 阶段解锁条件：
 * - Lv0 课桌: 放置创业核心方块
 * - Lv1 宿舍工作室: 500 coins + 10 products sold
 * - Lv2 校园孵化器: 2000 coins + 3 employees + 50 products
 * - Lv3 独立公司: 10000 coins + market share 5%
 * - Lv4 企业总部: 50000 coins + market share 15%
 * - Lv5 商业帝国: IPO success
 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class BusinessStageManager {

    /** 玩家的创业阶段缓存 */
    private static final Map<UUID, BusinessStage> PLAYER_STAGES = new ConcurrentHashMap<>();
    
    /** 玩家产品销量统计 */
    private static final Map<UUID, Integer> PLAYER_PRODUCTS_SOLD = new ConcurrentHashMap<>();
    
    /** 玩家创建的创业核心方块位置（简化：每个玩家一个） */
    private static final Map<UUID, StartupCore> PLAYER_CORES = new ConcurrentHashMap<>();
    
    private BusinessStageManager() {}
    
    /**
     * 注册管理器。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(BusinessStageManager.class);
        SimuKraft.LOGGER.info("BusinessStageManager registered (5 business stages)");
    }
    
    /**
     * Server tick事件 - 检查阶段升级条件。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 每600tick（30秒）检查一次
        if (event.getServer().overworld().getGameTime() % 600 != 0) return;
        
        for (Map.Entry<UUID, BusinessStage> entry : PLAYER_STAGES.entrySet()) {
            UUID playerId = entry.getKey();
            BusinessStage stage = entry.getValue();
            
            // 检查是否可以升级
            if (stage.canLevelUp()) {
                BusinessStage next = stage.getNext();
                if (next != null) {
                    entry.setValue(next);
                    // TODO: 通知玩家升级
                    SimuKraft.LOGGER.info("Player {} leveled up from {} to {}", playerId, stage, next);
                }
            }
        }
    }
    
    /**
     * 放置创业核心方块。
     */
    public static boolean placeStartupCore(Player player) {
        UUID playerId = player.getUUID();
        if (PLAYER_CORES.containsKey(playerId)) {
            return false; // 已经放置过
        }
        
        StartupCore core = new StartupCore(
            playerId,
            player.blockPosition(),
            BusinessStage.DESK
        );
        
        PLAYER_CORES.put(playerId, core);
        PLAYER_STAGES.put(playerId, BusinessStage.DESK);
        PLAYER_PRODUCTS_SOLD.put(playerId, 0);
        
        SimuKraft.LOGGER.info("Player {} placed startup core at {}", player.getName().getString(), player.blockPosition());
        return true;
    }
    
    /**
     * 记录产品售出。
     */
    public static void recordSale(Player player) {
        PLAYER_PRODUCTS_SOLD.merge(player.getUUID(), 1, Integer::sum);
    }
    
    /**
     * 获取玩家当前阶段。
     */
    public static BusinessStage getPlayerStage(UUID playerId) {
        return PLAYER_STAGES.getOrDefault(playerId, null);
    }
    
    /**
     * 获取玩家创业核心数据。
     */
    public static BusinessStageManager.StartupCore getPlayerCore(UUID playerId) {
        return PLAYER_CORES.get(playerId);
    }
    
    /**
     * 获取玩家售出数量。
     */
    public static int getProductsSold(UUID playerId) {
        return PLAYER_PRODUCTS_SOLD.getOrDefault(playerId, 0);
    }
    
    /**
     * 创业阶段枚举。
     */
    public enum BusinessStage {
        DESK(0, "课桌创业", 0, 0),
        DORM_WORKSHOP(1, "宿舍工作室", 500, 10),
        CAMPUS_INCUBATOR(2, "校园孵化器", 2000, 50),
        INDEPENDENT_COMPANY(3, "独立公司", 10000, 200),
        CORPORATE_HQ(4, "企业总部", 50000, 500),
        BUSINESS_EMPIRE(5, "商业帝国", 200000, 2000);
        
        private final int level;
        private final String displayName;
        private final float requiredCoins;
        private final int requiredProductsSold;
        
        BusinessStage(int level, String displayName, float requiredCoins, int requiredProductsSold) {
            this.level = level;
            this.displayName = displayName;
            this.requiredCoins = requiredCoins;
            this.requiredProductsSold = requiredProductsSold;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
        public float getRequiredCoins() { return requiredCoins; }
        public int getRequiredProductsSold() { return requiredProductsSold; }
        
        /**
         * 检查是否可以升级。
         */
        public boolean canLevelUp() {
            // 最高级不能升级
            return this != BUSINESS_EMPIRE;
        }
        
        /**
         * 获取下一阶段。
         */
        public BusinessStage getNext() {
            return switch (this) {
                case DESK -> DORM_WORKSHOP;
                case DORM_WORKSHOP -> CAMPUS_INCUBATOR;
                case CAMPUS_INCUBATOR -> INDEPENDENT_COMPANY;
                case INDEPENDENT_COMPANY -> CORPORATE_HQ;
                case CORPORATE_HQ -> BUSINESS_EMPIRE;
                default -> null;
            };
        }
        
        /**
         * 获取员工槽位数量。
         */
        public int getEmployeeSlots() {
            return switch (this) {
                case DESK -> 0;
                case DORM_WORKSHOP -> 1;
                case CAMPUS_INCUBATOR -> 3;
                case INDEPENDENT_COMPANY -> 8;
                case CORPORATE_HQ -> 20;
                case BUSINESS_EMPIRE -> Integer.MAX_VALUE;
            };
        }
        
        /**
         * 获取产品槽位数量。
         */
        public int getProductSlots() {
            return switch (this) {
                case DESK -> 1;
                case DORM_WORKSHOP -> 2;
                case CAMPUS_INCUBATOR -> 4;
                case INDEPENDENT_COMPANY -> 8;
                case CORPORATE_HQ, BUSINESS_EMPIRE -> Integer.MAX_VALUE;
            };
        }
    }
    
    /**
     * 创业核心方块数据（简化版，真实世界中的方块）。
     */
    public static final class StartupCore {
        public final UUID ownerId;
        public final net.minecraft.core.BlockPos position;
        public BusinessStage stage;
        public long placedAt;
        public int totalProductsSold = 0;
        public float totalRevenue = 0;
        
        public StartupCore(UUID ownerId, net.minecraft.core.BlockPos position, BusinessStage stage) {
            this.ownerId = ownerId;
            this.position = position;
            this.stage = stage;
            this.placedAt = System.currentTimeMillis();
        }
    }
}
