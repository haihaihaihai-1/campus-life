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
 * 布局（使用generic_54.png箱子纹理）：
 * - 上半部分：左侧1格产品槽 + 右侧价格信息&按钮
 * - 下半部分：玩家背包（3行+快捷栏）
 *
 * 按钮放在GUI内部 x=130~170 区域，不超出窗口。
 */
public class SalesStallScreen extends AbstractContainerScreen<SalesStallMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND =
        ResourceLocation.parse("textures/gui/container/generic_54.png");

    public SalesStallScreen(SalesStallMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;

        // 4个价格按钮放在GUI内部右侧
        // 产品槽在 x=80, y=20（见SalesStallMenu）
        // 按钮放在 x=118~158 区域
        int btnX1 = this.leftPos + 118; // +1/-1 左列
        int btnX2 = this.leftPos + 140; // +5/-5 右列
        int btnY1 = this.topPos + 20;   // 上行
        int btnY2 = this.topPos + 42;   // 下行

        this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> changePrice(1))
            .pos(btnX1, btnY1).size(20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> changePrice(-1))
            .pos(btnX1, btnY2).size(20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+5"), b -> changePrice(5))
            .pos(btnX2, btnY1).size(20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("-5"), b -> changePrice(-5))
            .pos(btnX2, btnY2).size(20, 18).build());
    }

    private void changePrice(int delta) {
        var be = this.getMenu().getBlockEntity();
        int newPrice = be.getPrice() + delta;
        newPrice = Math.max(1, Math.min(999, newPrice));
        PacketDistributor.sendToServer(new SalesStallPricePayload(be.getBlockPos(), newPrice));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 顶部容器区域（3行=72px）
        graphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, 72);
        // 玩家背包区域
        graphics.blit(CONTAINER_BACKGROUND, x, y + 72, 0, 126, this.imageWidth, 96);
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

        // 价格显示在按钮左侧
        graphics.drawString(this.font, "§e价格", 118, 8, 0x404040, false);
        graphics.drawString(this.font, "§e" + be.getPrice(), 118, 64, 0x404040, false);

        // 销售统计在底部
        graphics.drawString(this.font, "已售: " + be.getTotalSales(), 8, 42, 0x606060, false);
        graphics.drawString(this.font, "收入: " + be.getTotalRevenue(), 8, 54, 0x606060, false);

        if (be.getPendingCoins() > 0) {
            graphics.drawString(this.font, "§7待领: " + be.getPendingCoins(), 8, 66, 0x808080, false);
        }
    }
}
