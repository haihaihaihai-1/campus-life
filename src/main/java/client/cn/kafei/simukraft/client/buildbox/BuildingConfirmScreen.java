package client.cn.kafei.simukraft.client.buildbox;

import client.cn.kafei.simukraft.client.ui.SimuKraftUiTheme;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import common.cn.kafei.simukraft.building.BuildingBlockData;
import common.cn.kafei.simukraft.building.BuildingStructure;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@SuppressWarnings("null")
public final class BuildingConfirmScreen extends ModularUIScreen {
    private static final int CONTENT_MARGIN = 18;
    private static final float SCENE_WIDTH_RATIO = 0.71F;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 26;
    private static final int BUTTON_BOTTOM_MARGIN = 24;
    private static final float SCENE_CAMERA_YAW = -135.0F;
    private static final float SCENE_CAMERA_PITCH = 25.0F;
    private static final float SCENE_ZOOM_PADDING = 1.18F;

    public BuildingConfirmScreen(Screen parent, BuildingCacheService.BuildingMeta building, BlockPos buildBoxPos, BuildingStructure structure) {
        super(createUi(parent, building, buildBoxPos, structure), Component.translatable("gui.building_preview.title"));
    }

    private static ModularUI createUi(Screen parent, BuildingCacheService.BuildingMeta building, BlockPos buildBoxPos, BuildingStructure structure) {
        int screenWidth = Math.max(320, Minecraft.getInstance().getWindow().getGuiScaledWidth());
        int screenHeight = Math.max(240, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        UIElement root = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        int buttonTop = screenHeight - BUTTON_BOTTOM_MARGIN - BUTTON_HEIGHT;
        int sceneLeft = CONTENT_MARGIN;
        int sceneTop = CONTENT_MARGIN;
        int sceneWidth = Math.max(120, Math.round(screenWidth * SCENE_WIDTH_RATIO) - CONTENT_MARGIN);
        int sceneHeight = Math.max(80, buttonTop - CONTENT_MARGIN * 2);
        root.addChild(createBuildingScene(structure, sceneLeft, sceneTop, sceneWidth, sceneHeight));

        int infoLeft = sceneLeft + sceneWidth + CONTENT_MARGIN;
        int infoWidth = Math.max(120, screenWidth - infoLeft - CONTENT_MARGIN);
        int infoTop = Math.max(CONTENT_MARGIN, screenHeight / 2 - 82);
        root.addChild(text(Component.translatable("gui.building_confirm.title", building.name()), infoWidth, SimuKraftUiTheme.TEXT_PRIMARY_COLOR, infoLeft, infoTop, 16, TextTexture.TextType.NORMAL));
        root.addChild(text(Component.translatable("gui.building_confirm.size", building.size()), infoWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, infoLeft, infoTop + 26, 14, TextTexture.TextType.NORMAL));
        root.addChild(text(Component.translatable("gui.building_confirm.price", building.amount()), infoWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, infoLeft, infoTop + 44, 14, TextTexture.TextType.NORMAL));
        root.addChild(text(Component.translatable("gui.building_confirm.author", building.author()), infoWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, infoLeft, infoTop + 62, 14, TextTexture.TextType.NORMAL));
        root.addChild(text(Component.translatable("gui.building_confirm.desc", building.structureFileName()), infoWidth, SimuKraftUiTheme.TEXT_INFO_COLOR, infoLeft, infoTop + 92, 14, TextTexture.TextType.NORMAL));
        root.addChild(text(Component.translatable("gui.building_confirm.hint"), infoWidth, SimuKraftUiTheme.TEXT_WARNING_COLOR, infoLeft, infoTop + 122, 28, TextTexture.TextType.NORMAL));

        root.addChild(createButton(Component.translatable("gui.building_confirm.preview"), screenWidth / 2 - BUTTON_WIDTH - 24, buttonTop, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(new BuildingPreviewScreen(Minecraft.getInstance().screen, building, buildBoxPos, structure));
            }
        }));
        root.addChild(createButton(Component.translatable("gui.button.back"), screenWidth / 2 + 24, buttonTop, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }));

        return new ModularUI(SimuKraftUiTheme.createUi(root))
                .shouldCloseOnEsc(true)
                .shouldCloseOnKeyInventory(false);
    }

    private static Scene createBuildingScene(BuildingStructure structure, int left, int top, int width, int height) {
        TrackedDummyWorld dummyWorld = new TrackedDummyWorld();
        List<BlockPos> positions = structure.blocks().stream()
                .filter(block -> !block.state().isAir() && block.state().getBlock() != Blocks.STRUCTURE_VOID)
                .map(BuildingBlockData::relativePos)
                .toList();
        List<BlockPos> renderedPositions = positions.isEmpty() ? List.of(BlockPos.ZERO) : positions;
        for (BuildingBlockData block : structure.blocks()) {
            if (!block.state().isAir() && block.state().getBlock() != Blocks.STRUCTURE_VOID) {
                dummyWorld.setBlockAndUpdate(block.relativePos(), block.state());
            }
        }

        Scene scene = new Scene()
                .createScene(dummyWorld)
                .useOrtho()
                .setTickWorld(false)
                .setRenderFacing(false)
                .setRenderSelect(false)
                .setShowHoverBlockTips(false)
                .useCacheBuffer()
                .setRenderedCore(renderedPositions, null, false)
                .setCameraYawAndPitch(SCENE_CAMERA_YAW, SCENE_CAMERA_PITCH)
                .setZoom(calculateFitZoom(renderedPositions, width, height));
        scene.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(left);
            layout.top(top);
            layout.width(width);
            layout.height(height);
        });
        return scene;
    }

    private static float calculateFitZoom(List<BlockPos> positions, int viewportWidth, int viewportHeight) {
        if (positions.isEmpty()) {
            return 4.0F;
        }
        Bounds bounds = Bounds.from(positions);
        double yaw = Math.toRadians(SCENE_CAMERA_YAW);
        double pitch = Math.toRadians(SCENE_CAMERA_PITCH);
        double cameraX = Math.cos(yaw);
        double cameraY = Math.tan(pitch);
        double cameraZ = Math.sin(yaw);
        double cameraLength = Math.sqrt(cameraX * cameraX + cameraY * cameraY + cameraZ * cameraZ);
        double forwardX = -cameraX / cameraLength;
        double forwardY = -cameraY / cameraLength;
        double forwardZ = -cameraZ / cameraLength;
        double rightX = -forwardZ;
        double rightY = 0.0D;
        double rightZ = forwardX;
        double rightLength = Math.sqrt(rightX * rightX + rightZ * rightZ);
        rightX /= rightLength;
        rightZ /= rightLength;
        double upX = rightY * forwardZ - rightZ * forwardY;
        double upY = rightZ * forwardX - rightX * forwardZ;
        double upZ = rightX * forwardY - rightY * forwardX;

        double minRight = Double.MAX_VALUE;
        double maxRight = -Double.MAX_VALUE;
        double minUp = Double.MAX_VALUE;
        double maxUp = -Double.MAX_VALUE;
        for (double x : new double[]{bounds.minX(), bounds.maxX()}) {
            for (double y : new double[]{bounds.minY(), bounds.maxY()}) {
                for (double z : new double[]{bounds.minZ(), bounds.maxZ()}) {
                    double projectedRight = x * rightX + y * rightY + z * rightZ;
                    double projectedUp = x * upX + y * upY + z * upZ;
                    minRight = Math.min(minRight, projectedRight);
                    maxRight = Math.max(maxRight, projectedRight);
                    minUp = Math.min(minUp, projectedUp);
                    maxUp = Math.max(maxUp, projectedUp);
                }
            }
        }

        float aspectRatio = viewportWidth / (float) Math.max(1, viewportHeight);
        float horizontalZoom = (float) ((maxRight - minRight) * 0.5D);
        float verticalZoom = (float) ((maxUp - minUp) * aspectRatio * 0.5D);
        return Math.max(4.0F, Math.max(horizontalZoom, verticalZoom) * SCENE_ZOOM_PADDING);
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private static Bounds from(List<BlockPos> positions) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos pos : positions) {
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX() + 1);
                maxY = Math.max(maxY, pos.getY() + 1);
                maxZ = Math.max(maxZ, pos.getZ() + 1);
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private static UIElement text(Component text, int width, int color, int left, int top, int height, TextTexture.TextType type) {
        UIElement element = new UIElement();
        element.style(style -> style.backgroundTexture(new TextTexture(text.getString())
                .setWidth(width)
                .setType(type)
                .setColor(color)
                .setDropShadow(true)));
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(left);
            layout.top(top);
            layout.width(width);
            layout.height(height);
        });
        return element;
    }

    private static Button createButton(Component text, int left, int top, Runnable action) {
        Button button = new Button();
        button.setText(text);
        button.setOnClick(event -> action.run());
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(left);
            layout.top(top);
            layout.width(BUTTON_WIDTH);
            layout.height(BUTTON_HEIGHT);
        });
        return button;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
