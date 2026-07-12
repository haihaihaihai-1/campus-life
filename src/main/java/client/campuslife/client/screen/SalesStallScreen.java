package client.campuslife.client.screen;

import common.campuslife.menu.SalesStallMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 销售摊位客户端Screen。
 * 
 * 显示：
 * - 中间：产品槽（1格）
 * - 右侧：价格调整按钮（+1/-1/+5/-5）
 * - 下方：玩家背包
 * - 底部：销售统计（已售X个，总收入Y硬币）
 */
public class SalesStallScreen extends AbstractContainerScreen<SalesStallMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND =
        ResourceLocation.parse("textures/gui/container/dispenser.png");

    public SalesStallScreen(SalesStallMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 显示价格
        var be = this.getMenu().getBlockEntity();
        graphics.drawString(this.font, "价格: " + be.getPrice() + " 硬币", 8, 40, 0x404040, false);
        graphics.drawString(this.font, "已售: " + be.getTotalSales() + " 个", 8, 52, 0x606060, false);
        graphics.drawString(this.font, "总收入: " + be.getTotalRevenue() + " 硬币", 8, 64, 0x606060, false);
    }
}
