package client.cn.kafei.simukraft.client.citizen;

import common.cn.kafei.simukraft.network.citizen.manage.CityCitizenManageActionPacket;
import common.cn.kafei.simukraft.network.citizen.manage.CityCitizenManageResponsePacket;
import common.cn.kafei.simukraft.network.citizen.manage.CityCitizenManageResponsePacket.CitizenEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * CityCitizenManageScreen: 城市核心“市民管理”原生界面。
 * 展示全城在册市民，可滚动浏览；有管理权限时右键单个市民弹出菜单解雇/流放。
 */
@SuppressWarnings("null")
@OnlyIn(Dist.CLIENT)
public final class CityCitizenManageScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int PANEL_W = 360;
    private static final int MENU_W = 72;
    private static final int MENU_H = 36;

    private static final int COLOR_PANEL = 0xE0101418;
    private static final int COLOR_ROW_EVEN = 0xFF161B21;
    private static final int COLOR_ROW_ODD = 0xFF12161B;
    private static final int COLOR_ROW_HOVER = 0xFF20262E;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFFAAB2BD;
    private static final int COLOR_MUTED = 0xFF8A929C;
    private static final int COLOR_MENU_BG = 0xF0202830;
    private static final int COLOR_MENU_HOVER = 0xFF30404F;
    private static final int COLOR_EXILE = 0xFFFF8080;

    private final CityCitizenManageResponsePacket packet;
    private final List<CitizenEntry> citizens;
    private int scrollRow;
    private int menuIndex = -1;
    private int menuX;
    private int menuY;

    private CityCitizenManageScreen(CityCitizenManageResponsePacket packet) {
        super(Component.translatable("screen.simukraft.city_core.citizen_manage.title", packet.cityName(), packet.citizens().size()));
        this.packet = packet;
        this.citizens = packet.citizens();
    }

    /** open: 接收服务端市民快照并打开界面。 */
    public static void open(CityCitizenManageResponsePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> minecraft.setScreen(new CityCitizenManageScreen(packet)));
        }
    }

    @Override
    protected void init() {
        clampScroll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelLeft() {
        return (this.width - PANEL_W) / 2;
    }

    private int listTop() {
        return 52;
    }

    private int listBottom() {
        return this.height - 24;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, citizens.size() - visibleRows());
    }

    private void clampScroll() {
        scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = panelLeft();
        graphics.fill(left - 6, 10, left + PANEL_W + 6, this.height - 10, COLOR_PANEL);
        graphics.drawString(this.font, this.title, left, 18, COLOR_TEXT, true);
        if (packet.canManage()) {
            graphics.drawString(this.font, Component.translatable("screen.simukraft.city_core.citizen_manage.hint"), left, 32, COLOR_DIM);
        }
        clampScroll();
        if (citizens.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("screen.simukraft.city_core.citizen_manage.empty"), left, listTop() + 8, COLOR_MUTED);
        } else {
            int visible = visibleRows();
            int last = Math.min(citizens.size(), scrollRow + visible);
            for (int i = scrollRow; i < last; i++) {
                CitizenEntry citizen = citizens.get(i);
                int rowY = listTop() + (i - scrollRow) * ROW_H;
                boolean hovered = menuIndex < 0 && mouseX >= left && mouseX <= left + PANEL_W && mouseY >= rowY && mouseY < rowY + ROW_H;
                graphics.fill(left, rowY, left + PANEL_W, rowY + ROW_H - 2, hovered ? COLOR_ROW_HOVER : ((i & 1) == 0 ? COLOR_ROW_EVEN : COLOR_ROW_ODD));
                String name = citizen.name() == null || citizen.name().isBlank() ? "-" : citizen.name();
                graphics.drawString(this.font, name, left + 6, rowY + 3, COLOR_TEXT);
                Component info = Component.translatable("screen.simukraft.city_core.citizen_manage.row_info",
                        Component.translatable(citizen.jobKey()),
                        Component.translatable(citizen.workStatusKey()),
                        String.valueOf(citizen.age()),
                        Component.translatable("screen.simukraft.city_core.citizen_manage.gender_" + citizen.gender()));
                graphics.drawString(this.font, info, left + 6, rowY + 13, COLOR_DIM);
            }
            if (maxScroll() > 0) {
                graphics.drawString(this.font, Component.translatable("screen.simukraft.city_core.citizen_manage.scroll_hint",
                                (scrollRow + 1) + "-" + last, citizens.size()),
                        left + PANEL_W - 78, 32, COLOR_DIM);
            }
        }
        if (menuIndex >= 0) {
            renderContextMenu(graphics, mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = Math.min(menuX, this.width - MENU_W);
        int y = Math.min(menuY, this.height - MENU_H);
        graphics.fill(x, y, x + MENU_W, y + MENU_H, COLOR_MENU_BG);
        boolean hoverDismiss = mouseX >= x && mouseX <= x + MENU_W && mouseY >= y && mouseY < y + MENU_H / 2;
        boolean hoverExile = mouseX >= x && mouseX <= x + MENU_W && mouseY >= y + MENU_H / 2 && mouseY < y + MENU_H;
        if (hoverDismiss) {
            graphics.fill(x, y, x + MENU_W, y + MENU_H / 2, COLOR_MENU_HOVER);
        }
        if (hoverExile) {
            graphics.fill(x, y + MENU_H / 2, x + MENU_W, y + MENU_H, COLOR_MENU_HOVER);
        }
        graphics.drawString(this.font, Component.translatable("screen.simukraft.city_core.citizen_manage.dismiss"), x + 6, y + 5, COLOR_TEXT);
        graphics.drawString(this.font, Component.translatable("screen.simukraft.city_core.citizen_manage.exile"), x + 6, y + MENU_H / 2 + 5, COLOR_EXILE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (menuIndex < 0 && verticalAmount != 0 && maxScroll() > 0) {
            scrollRow -= (int) Math.signum(verticalAmount);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menuIndex >= 0) {
            int x = Math.min(menuX, this.width - MENU_W);
            int y = Math.min(menuY, this.height - MENU_H);
            if (menuIndex < citizens.size() && mouseX >= x && mouseX <= x + MENU_W && mouseY >= y && mouseY < y + MENU_H) {
                CitizenEntry target = citizens.get(menuIndex);
                sendAction(mouseY < y + MENU_H / 2 ? CityCitizenManageActionPacket.Action.DISMISS : CityCitizenManageActionPacket.Action.EXILE, target);
            }
            menuIndex = -1;
            return true;
        }
        if (button == 1 && packet.canManage()) {
            int idx = rowAt(mouseX, mouseY);
            if (idx >= 0) {
                menuIndex = idx;
                menuX = (int) mouseX;
                menuY = (int) mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int rowAt(double mouseX, double mouseY) {
        int left = panelLeft();
        if (mouseX < left || mouseX > left + PANEL_W || mouseY < listTop()) {
            return -1;
        }
        int last = Math.min(citizens.size(), scrollRow + visibleRows());
        for (int i = scrollRow; i < last; i++) {
            int rowY = listTop() + (i - scrollRow) * ROW_H;
            if (mouseY >= rowY && mouseY < rowY + ROW_H) {
                return i;
            }
        }
        return -1;
    }

    private void sendAction(CityCitizenManageActionPacket.Action action, CitizenEntry target) {
        PacketDistributor.sendToServer(new CityCitizenManageActionPacket(packet.pos(), action, target.citizenId()));
    }
}
