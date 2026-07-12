package client.campuslife.client.screen;

import common.campuslife.menu.WorkstationMenu;
import common.campuslife.network.CraftRequestPayload;
import common.campuslife.product.Product;
import common.campuslife.product.ProductRegistry;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台客户端Screen。
 *
 * 布局：
 * - 左侧：27格方块容器 + 36格玩家背包（和箱子一样）
 * - 右侧：产品列表（5个按钮，点击开始生产）
 *
 * 玩家把原料放进容器，点击产品按钮，等待制作完成，产品出现在容器中。
 */
public class WorkstationScreen extends AbstractContainerScreen<WorkstationMenu> {

    private static final ResourceLocation CONTAINER_BACKGROUND =
        ResourceLocation.parse("textures/gui/container/generic_54.png");

    private final List<ProductButton> productButtons = new ArrayList<>();

    public WorkstationScreen(WorkstationMenu menu, Inventory playerInventory, Component title) {
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

        // 在容器右侧创建产品按钮
        int buttonX = this.leftPos + this.imageWidth + 4;
        int buttonY = this.topPos;
        int index = 0;

        for (Product product : ProductRegistry.getAll()) {
            int y = buttonY + index * 22;
            ProductButton btn = new ProductButton(
                buttonX, y, 100, 20,
                Component.literal(product.getName()),
                product.getId(),
                (b) -> onProductClicked(product.getId())
            );
            this.addRenderableWidget(btn);
            productButtons.add(btn);
            index++;
        }
    }

    /**
     * 玩家点击产品按钮时，发送网络包到服务端。
     */
    private void onProductClicked(String productId) {
        var pos = this.getMenu().getBlockEntity().getBlockPos();
        PacketDistributor.sendToServer(new CraftRequestPayload(pos, productId));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 渲染顶部容器区域（3行=72px）
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // 玩家背包标题
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 显示生产进度
        var be = this.getMenu().getBlockEntity();
        if (be.getCraftProgress() > 0) {
            String progress = String.format("生产中: %d/%d", be.getCraftTotalTime() - be.getCraftProgress(), be.getCraftTotalTime());
            graphics.drawString(this.font, progress, this.titleLabelX, this.titleLabelY + 12, 0x404040, false);
        } else {
            graphics.drawString(this.font, "就绪 - 点击右侧生产", this.titleLabelX, this.titleLabelY + 12, 0x808080, false);
        }

        // 总生产数
        if (be.getTotalCrafted() > 0) {
            String crafted = "已生产: " + be.getTotalCrafted();
            graphics.drawString(this.font, crafted, this.titleLabelX, this.titleLabelY + 24, 0x606060, false);
        }
    }
}
