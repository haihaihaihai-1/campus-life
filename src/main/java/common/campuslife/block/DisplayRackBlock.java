package common.campuslife.block;

import net.minecraft.world.level.material.MapColor;

/**
 * 展示架方块。
 * 
 * Lv2+ 建筑中出现的方块，有以下功能：
 * - 提示附近的NPC这里有商品（增加客流量）
 * - 右键收集客户反馈（获取满意度数据）
 * - 放置产品进行展示
 */
public final class DisplayRackBlock extends BaseStartupBlock {

    public DisplayRackBlock() {
        super(MapColor.COLOR_ORANGE);
    }
}
