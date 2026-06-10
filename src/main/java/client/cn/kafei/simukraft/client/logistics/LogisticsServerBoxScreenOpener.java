package client.cn.kafei.simukraft.client.logistics;

import client.cn.kafei.simukraft.client.hire.NpcHireScreen;
import common.cn.kafei.simukraft.logistics.LogisticsConstants;
import common.cn.kafei.simukraft.logistics.LogisticsControlBoxService;
import common.cn.kafei.simukraft.logistics.LogisticsDirection;
import common.cn.kafei.simukraft.network.logistics.LogisticsBoxActionPacket;
import common.cn.kafei.simukraft.network.logistics.LogisticsServerBoxOpenRequestPacket;
import common.cn.kafei.simukraft.network.logistics.LogisticsServerBoxOpenResponsePacket;
import common.cn.kafei.simukraft.network.logistics.LogisticsWarehouseGridOpenRequestPacket;
import common.cn.kafei.simukraft.network.npc.hire.NpcHireFirePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@SuppressWarnings("null")
@OnlyIn(Dist.CLIENT)
public final class LogisticsServerBoxScreenOpener {
    private static ActiveTab activeTab = ActiveTab.OVERVIEW;

    private LogisticsServerBoxScreenOpener() {
    }

    /** pushWarehouseItems: 把服务端仓库快照推给当前旧版仓库页。 */
    public static void pushWarehouseItems(BlockPos pos, List<net.minecraft.world.item.ItemStack> items, List<Integer> counts) {
        LogisticsWarehouseGridScreen.receiveIfOpen(pos, items, counts);
    }

    /** request: 请求打开旧版服务端主界面。 */
    public static void request(BlockPos pos) {
        activeTab = ActiveTab.OVERVIEW;
        PacketDistributor.sendToServer(new LogisticsServerBoxOpenRequestPacket(pos));
    }

    /** requestMap: 请求打开旧版地图 Tab。 */
    public static void requestMap(BlockPos pos) {
        activeTab = ActiveTab.MAP;
        PacketDistributor.sendToServer(new LogisticsServerBoxOpenRequestPacket(pos));
    }

    /** requestManage: 请求打开旧版仓库总览 Tab。 */
    public static void requestManage(BlockPos pos) {
        activeTab = ActiveTab.OVERVIEW;
        PacketDistributor.sendToServer(new LogisticsServerBoxOpenRequestPacket(pos));
    }

    /** open: 接收服务端快照并打开原生 Screen。 */
    public static void open(LogisticsServerBoxOpenResponsePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> minecraft.setScreen(new LogisticsServerBoxScreen(packet, activeTab)));
        }
    }

    private enum ActiveTab {
        OVERVIEW,
        MAP,
        ROUTES
    }

    private static final class LogisticsServerBoxScreen extends Screen {
        private static final int TAB_WIDTH = 86;
        private static final int TAB_HEIGHT = 24;
        private static final int TAB_X = 5;

        private final LogisticsServerBoxOpenResponsePacket packet;
        private ActiveTab currentTab;

        private LogisticsServerBoxScreen(LogisticsServerBoxOpenResponsePacket packet, ActiveTab currentTab) {
            super(Component.translatable("gui.simukraft.logistics.server.title"));
            this.packet = packet;
            this.currentTab = currentTab;
        }

        /** init: 重建左侧 Tab 和当前内容页按钮。 */
        @Override
        protected void init() {
            rebuildUI();
        }

        /** renderBackground: 绘制旧版深色背景。 */
        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            LogisticsNativeStyle.drawBackdrop(graphics, this.width, this.height);
        }

        /** render: 绘制标题、左侧栏、分隔线和当前 Tab 文本。 */
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.server.title"), TAB_X, 10, LogisticsNativeStyle.TEXT, true);
            LogisticsNativeStyle.drawPanel(graphics, TAB_X - 2, 26, TAB_WIDTH + 4, this.height - 31);
            int lineX = TAB_X + TAB_WIDTH + 6;
            graphics.fill(lineX, 26, lineX + 1, this.height - 5, LogisticsNativeStyle.PANEL_LINE);
            int contentX = lineX + 8;
            int contentY = 30;
            switch (currentTab) {
                case OVERVIEW -> renderOverview(graphics, contentX + 164, contentY);
                case MAP -> renderMapTab(graphics, contentX, contentY);
                case ROUTES -> renderRoutes(graphics, contentX, contentY);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        /** rebuildUI: 清空并重建所有按钮，防止刷新后重复绑定。 */
        private void rebuildUI() {
            clearWidgets();
            int tabY = 30;
            addTab(Component.translatable("gui.simukraft.logistics.server.tab.overview"), ActiveTab.OVERVIEW, tabY);
            addTab(Component.translatable("gui.simukraft.logistics.server.tab.map"), ActiveTab.MAP, tabY + TAB_HEIGHT + 2);
            addTab(Component.translatable("gui.simukraft.logistics.server.tab.routes"), ActiveTab.ROUTES, tabY + (TAB_HEIGHT + 2) * 2);
            int contentX = TAB_X + TAB_WIDTH + 15;
            int contentY = 30;
            switch (currentTab) {
                case OVERVIEW -> buildOverviewButtons(contentX, contentY);
                case MAP -> buildMapButtons(contentX, contentY);
                case ROUTES -> buildRouteButtons(contentX, contentY);
            }
        }

        /** addTab: 创建左侧旧版 Tab 按钮。 */
        private void addTab(Component label, ActiveTab tab, int y) {
            String prefix = currentTab == tab ? "> " : "";
            Button button = addRenderableWidget(LogisticsNativeStyle.button(Component.literal(prefix).append(label),
                    TAB_X, y, TAB_WIDTH, TAB_HEIGHT, () -> {
                        currentTab = tab;
                        activeTab = tab;
                        rebuildUI();
                    }));
            button.active = currentTab != tab;
        }

        /** buildOverviewButtons: 创建仓库总览页按钮。 */
        private void buildOverviewButtons(int x, int y) {
            int buttonWidth = 150;
            int buttonHeight = 20;
            int gap = 24;
            boolean hasWarehouse = hasWarehouse();
            Button hire = addRenderableWidget(LogisticsNativeStyle.button(packet.hasWorker()
                    ? Component.translatable("gui.simukraft.logistics.worker_line", packet.workerName())
                    : Component.translatable("gui.simukraft.logistics.hire_storage"), x, y, buttonWidth, buttonHeight,
                    () -> NpcHireScreen.request(packet.boxPos(), LogisticsConstants.SERVER_SOURCE_TYPE, LogisticsConstants.STORAGE_ROLE)));
            hire.active = packet.hasCity() && !packet.hasWorker();
            Button fire = addRenderableWidget(LogisticsNativeStyle.button(Component.translatable("gui.simukraft.logistics.fire_storage"),
                    x, y + gap, buttonWidth, buttonHeight, this::fireWorker));
            fire.active = packet.hasWorker() && packet.workerId() != null;
            Button create = addRenderableWidget(LogisticsNativeStyle.button(hasWarehouse
                    ? Component.translatable("gui.simukraft.logistics.create_warehouse.count", packet.containers().size())
                    : Component.translatable("gui.simukraft.logistics.create_warehouse"), x, y + gap * 2, buttonWidth, buttonHeight,
                    () -> LogisticsAreaSelectionScreen.openWarehouseBinding(packet.boxPos())));
            create.active = packet.hasCity() && !hasWarehouse;
            Button delete = addRenderableWidget(LogisticsNativeStyle.button(Component.translatable("gui.simukraft.logistics.delete_warehouse"),
                    x, y + gap * 3, buttonWidth, buttonHeight,
                    () -> send(LogisticsBoxActionPacket.Action.DELETE_WAREHOUSE, null, null, BlockPos.ZERO, "", LogisticsDirection.WAREHOUSE_TO_CLIENT, List.of())));
            delete.active = hasWarehouse;
            Button manage = addRenderableWidget(LogisticsNativeStyle.button(Component.translatable("gui.simukraft.logistics.inventory"),
                    x, y + gap * 4, buttonWidth, buttonHeight,
                    () -> PacketDistributor.sendToServer(new LogisticsWarehouseGridOpenRequestPacket(packet.boxPos()))));
            manage.active = hasWarehouse;
        }

        /** buildMapButtons: 创建地图页打开按钮。 */
        private void buildMapButtons(int x, int y) {
            Button openMap = addRenderableWidget(LogisticsNativeStyle.button(Component.translatable("gui.simukraft.logistics.server.tab.map"),
                    x, y + 44, 120, 20, () -> LogisticsNetworkMapScreen.open(packet)));
            openMap.active = hasWarehouse();
        }

        /** buildRouteButtons: 创建路径管理按钮。 */
        private void buildRouteButtons(int x, int y) {
            Button addRoute = addRenderableWidget(LogisticsNativeStyle.button(Component.literal("+ ").append(Component.translatable("gui.simukraft.logistics.channel.create")),
                    x + 120, y - 2, 112, 18, () -> LogisticsChannelCreateScreenOpener.open(packet, null)));
            addRoute.active = hasWarehouse() && !packet.clients().isEmpty();
            int rowY = y + 30;
            for (LogisticsControlBoxService.ChannelEntry channel : packet.channels()) {
                UUID channelId = channel.channelId();
                addRenderableWidget(LogisticsNativeStyle.button(Component.translatable(channel.enabled()
                                ? "gui.simukraft.logistics.channel.disable"
                                : "gui.simukraft.logistics.channel.enable"),
                        x + 240, rowY, 36, 16,
                        () -> send(LogisticsBoxActionPacket.Action.TOGGLE_CHANNEL, null, channelId, BlockPos.ZERO, "", LogisticsDirection.WAREHOUSE_TO_CLIENT, List.of())));
                addRenderableWidget(LogisticsNativeStyle.button(Component.literal("x"),
                        x + 280, rowY, 24, 16,
                        () -> send(LogisticsBoxActionPacket.Action.DELETE_CHANNEL, null, channelId, BlockPos.ZERO, "", LogisticsDirection.WAREHOUSE_TO_CLIENT, List.of())));
                rowY += 40;
                if (rowY > this.height - 20) {
                    break;
                }
            }
        }

        /** renderOverview: 绘制仓库状态和费用说明。 */
        private void renderOverview(GuiGraphics graphics, int x, int y) {
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.server.tab.overview"), x, y, LogisticsNativeStyle.TEXT_WARN);
            y += 14;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.city_line", packet.hasCity() ? packet.cityName() : "-"), x, y, LogisticsNativeStyle.TEXT_DIM);
            y += 11;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.funds_line", String.format(Locale.ROOT, "%.2f", packet.cityBalance())), x, y, LogisticsNativeStyle.TEXT_DIM);
            y += 11;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.worker_line", packet.hasWorker() ? packet.workerName() : "-"), x, y, LogisticsNativeStyle.TEXT_DIM);
            y += 11;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.container_count", packet.containers().size()), x, y, LogisticsNativeStyle.TEXT_DIM);
            y += 11;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.channel_count", packet.channels().size()), x, y, LogisticsNativeStyle.TEXT_DIM);
            y += 22;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.transfer_cost_hint"), x, y, LogisticsNativeStyle.TEXT_WARN);
            y += 14;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.containers"), x, y, LogisticsNativeStyle.TEXT);
            y += 12;
            if (packet.containers().isEmpty()) {
                graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.empty"), x, y, LogisticsNativeStyle.TEXT_MUTED);
            } else {
                for (BlockPos container : packet.containers()) {
                    graphics.drawString(this.font, LogisticsNativeStyle.posText(container), x + 8, y, LogisticsNativeStyle.TEXT_DIM);
                    y += 10;
                    if (y > this.height - 12) {
                        break;
                    }
                }
            }
        }

        /** renderMapTab: 绘制旧版地图页提示。 */
        private void renderMapTab(GuiGraphics graphics, int x, int y) {
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.server.tab.map"), x, y, LogisticsNativeStyle.TEXT_WARN);
            y += 16;
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.map.hint"), x, y, LogisticsNativeStyle.TEXT_DIM);
        }

        /** renderRoutes: 绘制路径列表。 */
        private void renderRoutes(GuiGraphics graphics, int x, int y) {
            graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.server.tab.routes")
                    .append(Component.literal(" (" + packet.channels().size() + ")")), x, y, LogisticsNativeStyle.TEXT_WARN);
            if (packet.channels().isEmpty()) {
                graphics.drawString(this.font, Component.translatable("gui.simukraft.logistics.empty"), x, y + 30, LogisticsNativeStyle.TEXT_MUTED);
                return;
            }
            int rowY = y + 30;
            for (LogisticsControlBoxService.ChannelEntry channel : packet.channels()) {
                String direction = channel.direction() == LogisticsDirection.CLIENT_TO_WAREHOUSE ? "<-" : "->";
                LogisticsNativeStyle.drawStatusBadge(graphics, this.font, channel.enabled(), x, rowY - 1);
                LogisticsNativeStyle.drawFitString(graphics, this.font, direction + " " + LogisticsItemDisplayName.channelName(channel.name(), channel.filters()),
                        x + 31, rowY, 198, LogisticsNativeStyle.TEXT);
                LogisticsNativeStyle.drawFitString(graphics, this.font, clientName(channel.clientId()) + " | " + LogisticsItemDisplayName.filterText(channel.filters()),
                        x + 10, rowY + 11, 225, LogisticsNativeStyle.TEXT_DIM);
                rowY += 40;
                if (rowY > this.height - 20) {
                    break;
                }
            }
        }

        /** send: 发送物流服务端盒动作包。 */
        private void send(LogisticsBoxActionPacket.Action action, UUID clientId, UUID channelId, BlockPos targetPos,
                          String value, LogisticsDirection direction, List<String> filters) {
            PacketDistributor.sendToServer(new LogisticsBoxActionPacket(packet.boxPos(), action, clientId, channelId, targetPos, value, direction,
                    BlockPos.ZERO, BlockPos.ZERO, filters));
        }

        /** fireWorker: 解雇当前仓储管理员。 */
        private void fireWorker() {
            if (packet.hasWorker() && packet.workerId() != null) {
                PacketDistributor.sendToServer(new NpcHireFirePacket(packet.boxPos(), LogisticsConstants.SERVER_SOURCE_TYPE,
                        LogisticsConstants.STORAGE_ROLE, packet.workerId()));
            }
        }

        /** hasWarehouse: 判断当前服务端盒是否已绑定仓库容器。 */
        private boolean hasWarehouse() {
            return !packet.containers().isEmpty();
        }

        /** clientName: 按客户端 ID 获取显示名。 */
        private String clientName(UUID clientId) {
            return packet.clients().stream()
                    .filter(client -> client.clientId().equals(clientId))
                    .map(LogisticsControlBoxService.ClientEntry::name)
                    .findFirst()
                    .orElse("-");
        }

        /** filterText: 格式化频道物品过滤器。 */
    }
}
