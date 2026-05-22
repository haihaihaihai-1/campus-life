package client.cn.kafei.simukraft.client.buildbox;

import client.cn.kafei.simukraft.client.ui.SimuKraftUiTheme;
import common.cn.kafei.simukraft.building.BuildingStructure;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
public final class BuildingConfirmScreen extends ModularUIScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 155;

    public BuildingConfirmScreen(Screen parent, BuildingCacheService.BuildingMeta building, BlockPos buildBoxPos, BuildingStructure structure) {
        super(createUi(parent, building, buildBoxPos, structure), Component.translatable("gui.building_preview.title"));
    }

    private static ModularUI createUi(Screen parent, BuildingCacheService.BuildingMeta building, BlockPos buildBoxPos, BuildingStructure structure) {
        int screenWidth = Math.max(320, Minecraft.getInstance().getWindow().getGuiScaledWidth());
        int screenHeight = Math.max(240, Minecraft.getInstance().getWindow().getGuiScaledHeight());
        UIElement root = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        root.addChild(SimuKraftUiTheme.createShellPanel(screenWidth, screenHeight));

        UIElement panel = new UIElement().layout(layout -> {
            layout.width(PANEL_WIDTH);
            layout.height(PANEL_HEIGHT);
            layout.marginTop(-30);
        }).addClass("simukraft_panel");
        root.addChild(panel);

        int contentWidth = PANEL_WIDTH - 24;
        int textY = 18;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.title", building.name()), contentWidth, SimuKraftUiTheme.TEXT_PRIMARY_COLOR, textY, 14));
        textY += 18;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.size", building.size()), contentWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, textY, 12));
        textY += 12;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.price", building.amount()), contentWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, textY, 12));
        textY += 12;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.author", building.author()), contentWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, textY, 12));
        textY += 16;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.desc", building.structureFileName()), contentWidth, SimuKraftUiTheme.TEXT_INFO_COLOR, textY, 14));
        textY += 18;
        panel.addChild(centerText(Component.translatable("gui.building_confirm.hint"), contentWidth, SimuKraftUiTheme.TEXT_WARNING_COLOR, textY, 14));

        panel.addChild(createButton(Component.translatable("gui.building_confirm.preview"), 90, PANEL_HEIGHT - 36, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(new BuildingPreviewScreen(Minecraft.getInstance().screen, building, buildBoxPos, structure));
            }
        }));
        panel.addChild(createButton(Component.translatable("gui.button.back"), 190, PANEL_HEIGHT - 36, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }));

        return new ModularUI(SimuKraftUiTheme.createUi(root))
                .shouldCloseOnEsc(true)
                .shouldCloseOnKeyInventory(false);
    }

    private static UIElement centerText(Component text, int width, int color, int top, int height) {
        UIElement element = new UIElement();
        element.style(style -> style.backgroundTexture(new TextTexture(text.getString())
                .setWidth(width)
                .setType(TextTexture.TextType.NORMAL)
                .setColor(color)
                .setDropShadow(true)));
        element.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(12);
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
            layout.width(80);
            layout.height(20);
        });
        return button;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
