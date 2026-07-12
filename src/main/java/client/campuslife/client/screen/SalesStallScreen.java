package client.campuslife.client.screen;

import common.campuslife.menu.SalesStallMenu;
import common.campuslife.network.SalesStallPricePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 销售摊位客户端Screen。
 *
 * 布局：
 * - 左侧：1格产品槽 + 玩家背包
 * - 右侧：价格显示 + 4个价格按钮（+1/-1/+5/-5）
 * - 底部：销售统计
 */
public class SalesStallScreen extends AbstractContainerScreen<SalesStallMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND =
        ResourceLocation.parse("textures/gui/container/dispenser.png");

    private Button btnPlus1, btnMinus1, btnPlus5, btnMinus5;
    private int displayedPrice = -1;

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

        int buttonX = this.leftPos + this.imageWidth + 4;
        int buttonY = this.topPos + 30;

        // +1 按钮
        this.btnPlus1 = Button.builder(Component.literal("+1"), b -> changePrice(1))
            .pos(buttonX, buttonY).size(20, 20).build();
        // -1 按钮
        this.btnMinus1 = Button.builder(Component.literal("-1"), b -> changePrice(-1))
            .pos(buttonX + 22, buttonY).size(20, 20).build();
        // +5 按钮
        this.btnPlus5 = Button.builder(Component.literal("+5"), b -> changePrice(5))
            .pos(buttonX, buttonY + 22).size(20, 20).build();
        // -5 按钮
        this.btnMinus5 = Button.builder(Component.literal("-5"), b -> changePrice(-5))
            .pos(buttonX + 22, buttonY + 22).size(20, 20).build();

        this.addRenderableWidget(this.btnPlus1);
        this.addRenderableWidget(this.btnMinus1);
        this.addRenderableWidget(this.btnPlus5);
        this.addRenderableWidget(this.btnMinus5);
    }

    private void changePrice(int delta) {
        var be = this.getMenu().getBlockEntity();
        int newPrice = be.getPrice() + delta;
        newPrice = Math.max(1, Math.min(999, newPrice));
        // 发送网络包
        PacketDistributor.sendToServer(new SalesStallPricePayload(be.getBlockPos(), newPrice));
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

        var be = this.getMenu().getBlockEntity();

        // 价格显示
        graphics.drawString(this.font, "§e价格: " + be.getPrice() + " 硬币", 8, 30, 0x404040, false);

        // 销售统计
        graphics.drawString(this.font, "已售: " + be.getTotalSales() + " 个", 8, 45, 0x606060, false);
        graphics.drawString(this.font, "总收入: " + be.getTotalRevenue() + " 硬币", 8, 57, 0x606060, false);

        if (be.getPendingCoins() > 0) {
            graphics.drawString(this.font, "§7待领取: " + be.getPendingCoins() + " 硬币", 8, 69, 0x808080, false);
        }
    }
}
