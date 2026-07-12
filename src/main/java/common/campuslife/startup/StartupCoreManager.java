package common.campuslife.startup;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.core.EconomyVariable;
import common.campuslife.core.WorldStateBoard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * 创业核心方块管理器。
 * 
 * 管理"创业核心"方块的放置、升级和交互。
 * 玩家放置后解锁课桌创业阶段。
 * 
 * 方块功能：
 * - 放置：扣除材料，放置方块，解锁Lv0课桌创业
 * - 升级：满足条件后手动触发升级，旧方块被替换
 * - 交互：右键打开GUI（产品创造/销售/员工管理）
 * 
 * 建筑升级与simukraft建筑系统对接：
 * - 使用simukraft的BuilderConstructionService自动替换建筑
 * - 或者直接放置不同等级的方块
 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class StartupCoreManager {

    private StartupCoreManager() {}
    
    /**
     * 注册管理器。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(StartupCoreManager.class);
        SimuKraft.LOGGER.info("StartupCoreManager registered");
    }
    
    /**
     * Server tick事件 - 更新所有核心方块状态。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 每600tick（30秒）处理产品产出和建筑升级
        if (event.getServer().overworld().getGameTime() % 600 != 0) return;
        // TODO: 实现建筑升级逻辑
    }
    
    /**
     * 获取指定玩家的核心方块位置。
     */
    public static Optional<net.minecraft.core.BlockPos> getCorePos(UUID playerId) {
        BusinessStageManager.StartupCore core = BusinessStageManager.getPlayerCore(playerId);
        return core != null ? Optional.of(core.position) : Optional.empty();
    }
    
    /**
     * 检查玩家是否有核心方块。
     */
    public static boolean hasCore(UUID playerId) {
        return BusinessStageManager.getPlayerCore(playerId) != null;
    }
}
