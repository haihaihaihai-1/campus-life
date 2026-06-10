package client.cn.kafei.simukraft.client.logistics;

import client.cn.kafei.simukraft.client.freecamera.FreeCameraManager;
import client.cn.kafei.simukraft.client.freecamera.FreeCameraScreen;
import client.cn.kafei.simukraft.client.input.SimuKraftKeyMappings;
import client.cn.kafei.simukraft.client.selection.TwoPointSelectionManager;
import client.cn.kafei.simukraft.client.toast.ClientInfoToast;
import client.cn.kafei.simukraft.client.ui.SimuKraftUiTheme;
import common.cn.kafei.simukraft.logistics.LogisticsDirection;
import common.cn.kafei.simukraft.network.logistics.LogisticsBoxActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("null")
@OnlyIn(Dist.CLIENT)
public final class LogisticsAreaSelectionScreen extends Screen implements FreeCameraScreen {
    private static final double REACH_DISTANCE = 128.0D;

    private final BlockPos boxPos;
    private final LogisticsBoxActionPacket.Action action;

    private LogisticsAreaSelectionScreen(BlockPos boxPos, LogisticsBoxActionPacket.Action action) {
        super(Component.translatable("gui.simukraft.logistics.area_selection.title"));
        this.boxPos = boxPos.immutable();
        this.action = action;
    }

    /** openWarehouseBinding: 打开仓库容器批量绑定选区。 */
    public static void openWarehouseBinding(BlockPos boxPos) {
        open(boxPos, LogisticsBoxActionPacket.Action.BIND_WAREHOUSE_AREA);
    }

    /** openClientBinding: 打开客户端端口批量绑定选区。 */
    public static void openClientBinding(BlockPos boxPos) {
        open(boxPos, LogisticsBoxActionPacket.Action.BIND_CLIENT_AREA);
    }

    /** open: 在客户端线程打开物流选区界面。 */
    private static void open(BlockPos boxPos, LogisticsBoxActionPacket.Action action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> minecraft.setScreen(new LogisticsAreaSelectionScreen(boxPos, action)));
        }
    }

    /** init: 启动两点选区和自由相机。 */
    @Override
    protected void init() {
        TwoPointSelectionManager.start(TwoPointSelectionManager.SelectionMode.LOGISTICS, boxPos, null);
        FreeCameraManager.activate();
    }

    /** renderBackground: 保持世界画面可见，不绘制暗底。 */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** render: 绘制旧版选区提示文本。 */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int y = 8;
        graphics.drawCenteredString(this.font, Component.translatable("gui.simukraft.logistics.area_selection.header", actionName()), centerX, y, SimuKraftUiTheme.TEXT_WARNING_COLOR);
        y += 14;
        graphics.drawCenteredString(this.font, Component.translatable("gui.simukraft.logistics.area_selection.controls",
                SimuKraftKeyMappings.display(SimuKraftKeyMappings.SELECTION_POINT_1),
                SimuKraftKeyMappings.display(SimuKraftKeyMappings.SELECTION_POINT_2),
                SimuKraftKeyMappings.display(SimuKraftKeyMappings.SELECTION_CONFIRM),
                SimuKraftKeyMappings.display(SimuKraftKeyMappings.SELECTION_CANCEL)), centerX, y, SimuKraftUiTheme.TEXT_SECONDARY_COLOR);
        y += 12;
        graphics.drawCenteredString(this.font, Component.translatable("gui.simukraft.area_selection.camera_hint"), centerX, y, SimuKraftUiTheme.TEXT_INFO_COLOR);
        y += 15;
        TwoPointSelectionManager.SelectionState state = TwoPointSelectionManager.state();
        graphics.drawCenteredString(this.font, pointLine("gui.simukraft.area_selection.point1", state.point1()), centerX, y, SimuKraftUiTheme.TEXT_SECONDARY_COLOR);
        y += 12;
        graphics.drawCenteredString(this.font, pointLine("gui.simukraft.area_selection.point2", state.point2()), centerX, y, SimuKraftUiTheme.TEXT_SECONDARY_COLOR);
        if (state.point1() != null && state.point2() != null) {
            y += 14;
            BlockPos min = TwoPointSelectionManager.min(state.point1(), state.point2());
            BlockPos max = TwoPointSelectionManager.max(state.point1(), state.point2());
            graphics.drawCenteredString(this.font, Component.translatable("gui.simukraft.logistics.area_selection.bounds",
                    min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), selectedVolume(min, max)), centerX, y, SimuKraftUiTheme.TEXT_SUCCESS_COLOR);
        }
    }

    /** keyPressed: 处理选点、确认、取消和 ESC。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || SimuKraftKeyMappings.matches(SimuKraftKeyMappings.SELECTION_CANCEL, keyCode, scanCode)) {
            closeSelection();
            return true;
        }
        if (SimuKraftKeyMappings.matches(SimuKraftKeyMappings.SELECTION_POINT_1, keyCode, scanCode)) {
            setPoint(true);
            return true;
        }
        if (SimuKraftKeyMappings.matches(SimuKraftKeyMappings.SELECTION_POINT_2, keyCode, scanCode)) {
            setPoint(false);
            return true;
        }
        if (SimuKraftKeyMappings.matches(SimuKraftKeyMappings.SELECTION_CONFIRM, keyCode, scanCode)) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** mouseClicked: 兼容旧版左键点一、右键点二。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SimuKraftKeyMappings.matchesMouse(SimuKraftKeyMappings.SELECTION_POINT_1, button)) {
            setPoint(true);
            return true;
        }
        if (SimuKraftKeyMappings.matchesMouse(SimuKraftKeyMappings.SELECTION_POINT_2, button)) {
            setPoint(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** removed: 退出选区时清理自由相机和选区状态。 */
    @Override
    public void removed() {
        FreeCameraManager.deactivate();
        TwoPointSelectionManager.clear();
        if (this.minecraft != null && this.minecraft.mouseHandler.isMouseGrabbed()) {
            this.minecraft.mouseHandler.releaseMouse();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** setPoint: 从自由相机视线命中的方块设置选区点。 */
    private void setPoint(boolean first) {
        BlockPos hit = raycastBlock();
        if (hit == null) {
            ClientInfoToast.show(Component.translatable("toast.simukraft.title"), Component.translatable("gui.simukraft.area_selection.no_target"), "warning");
            return;
        }
        if (first) {
            TwoPointSelectionManager.setPoint1(hit);
        } else {
            TwoPointSelectionManager.setPoint2(hit);
        }
    }

    /** confirm: 发送选区绑定请求并关闭选区界面。 */
    private void confirm() {
        TwoPointSelectionManager.SelectionState state = TwoPointSelectionManager.state();
        if (state.point1() == null || state.point2() == null) {
            ClientInfoToast.show(Component.translatable("toast.simukraft.title"), Component.translatable("gui.simukraft.area_selection.need_points"), "warning");
            return;
        }
        BlockPos min = TwoPointSelectionManager.min(state.point1(), state.point2());
        BlockPos max = TwoPointSelectionManager.max(state.point1(), state.point2());
        PacketDistributor.sendToServer(new LogisticsBoxActionPacket(boxPos, action, null, null, BlockPos.ZERO, "",
                LogisticsDirection.WAREHOUSE_TO_CLIENT, min, max, List.of()));
        closeSelection();
    }

    /** closeSelection: 清理状态并关闭当前选区界面。 */
    private void closeSelection() {
        TwoPointSelectionManager.clear();
        FreeCameraManager.deactivate();
        Minecraft minecraft = this.minecraft;
        if (minecraft != null && minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    /** raycastBlock: 用自由相机方向射线检测目标方块。 */
    @Nullable
    private BlockPos raycastBlock() {
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return null;
        }
        Vec3 cameraPos = FreeCameraManager.getPosition();
        double yawRad = Math.toRadians(FreeCameraManager.getYaw());
        double pitchRad = Math.toRadians(FreeCameraManager.getPitch());
        Vec3 look = new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad)).normalize();
        BlockHitResult result = this.minecraft.level.clip(new ClipContext(
                cameraPos,
                cameraPos.add(look.scale(REACH_DISTANCE)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                this.minecraft.player));
        return result.getType() == HitResult.Type.BLOCK ? result.getBlockPos() : null;
    }

    /** actionName: 返回当前选区动作名称。 */
    private Component actionName() {
        return action == LogisticsBoxActionPacket.Action.BIND_CLIENT_AREA
                ? Component.translatable("gui.simukraft.logistics.area_selection.action.client")
                : Component.translatable("gui.simukraft.logistics.area_selection.action.warehouse");
    }

    /** pointLine: 格式化选区点提示文本。 */
    private Component pointLine(String key, @Nullable BlockPos pos) {
        if (pos == null) {
            return Component.translatable(key + ".empty");
        }
        return Component.translatable(key + ".set", pos.getX(), pos.getY(), pos.getZ());
    }

    /** selectedVolume: 计算选区体积，使用 long 防止大区域溢出。 */
    private long selectedVolume(BlockPos min, BlockPos max) {
        return ((long) max.getX() - min.getX() + 1L)
                * ((long) max.getY() - min.getY() + 1L)
                * ((long) max.getZ() - min.getZ() + 1L);
    }
}
