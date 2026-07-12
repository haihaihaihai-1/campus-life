package client.campuslife.client.screen;

import common.campuslife.menu.WorkstationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 工作台客户端Screen。
 * 
 * 渲染容器GUI：
 * - 上方27格方块容器（3行x9列）
 * - 下方36格玩家背包（3行+1行快捷栏）
 * - 标题：创业核心
 * 
 * 使用原版generic_54.png纹理（箱子风格）。
 */
public class WorkstationScreen extends AbstractContainerScreen<WorkstationMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND =
        ResourceLocation.parse("textures/gui/container/generic_54.png");

    public WorkstationScreen(WorkstationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 标题位置
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 渲染容器背景
        int x = this.leftPos;
        int y = this.topPos;

        // generic_54.png 有6行版本，每行高18px。
        // 我们需要3行容器 + 玩家背包区域。
        // 渲染顶部容器区域（3行）
        graphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, 72);

        // 渲染玩家背包区域
        graphics.blit(CONTAINER_BACKGROUND, x, y + 72, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
