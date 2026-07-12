package common.campuslife.block;

import net.minecraft.world.level.material.MapColor;

/**
 * 员工工位方块。
 * 
 * Lv1+ 建筑中出现的方块，代表玩家雇佣的员工工作位置。
 * 右键查看员工信息（岗位/忠诚度/产出）。
 */
public final class EmployeeStationBlock extends BaseStartupBlock {

    public EmployeeStationBlock() {
        super(MapColor.COLOR_BROWN);
    }
}
