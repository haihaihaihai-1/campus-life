package common.campuslife.block;

import net.minecraft.world.level.material.MapColor;

/**
 * 研发台方块。
 * 
 * Lv2+ 建筑中出现的方块，用于解锁新产品/升级产品。
 * 右键打开研发GUI。
 */
public final class ResearchStationBlock extends BaseStartupBlock {

    public ResearchStationBlock() {
        super(MapColor.COLOR_BLUE);
    }
}
