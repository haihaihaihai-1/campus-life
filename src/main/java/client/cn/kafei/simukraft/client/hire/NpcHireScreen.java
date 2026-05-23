package client.cn.kafei.simukraft.client.hire;

import client.cn.kafei.simukraft.client.buildbox.BuildBoxScreenOpener;
import client.cn.kafei.simukraft.client.citizen.CitizenAvatarFactory;
import client.cn.kafei.simukraft.client.ui.SimuKraftUiTheme;
import common.cn.kafei.simukraft.SimuKraft;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import common.cn.kafei.simukraft.network.npc.hire.NpcHireAssignPacket;
import common.cn.kafei.simukraft.network.npc.hire.NpcHireListRequestPacket;
import common.cn.kafei.simukraft.network.npc.hire.NpcHireListResponsePacket;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
public final class NpcHireScreen {
    private static final int CARD_TEXT_COLOR = SimuKraftUiTheme.CARD_TEXT_COLOR;
    private static final int NPC_PER_PAGE = 6;
    private static final int COLUMNS = 3;
    private static final int CARD_GAP = 10;
    private static final int TOP_MARGIN = 60;
    private static final int BOTTOM_MARGIN = 50;
    private static final int HEAD_SIZE = 34;
    private static final int PREFERRED_CARD_WIDTH = 180;
    private static final int MIN_CARD_WIDTH = 160;
    private static final int PREFERRED_CARD_HEIGHT = 80;
    private static final int MIN_CARD_HEIGHT = 76;
    private static UUID selectedNpcId;
    private static int currentPage;
    private static NpcHireListResponsePacket latestPacket;

    private NpcHireScreen() {
    }

    public static void request(BlockPos sourcePos, String sourceType, String role) {
        SimuKraft.LOGGER.info("Simukraft: Requesting hire list sourceType={} role={} pos={}", sourceType, role, sourcePos);
        PacketDistributor.sendToServer(new NpcHireListRequestPacket(sourcePos, sourceType, role));
    }

    public static void open(NpcHireListResponsePacket packet) {
        latestPacket = packet;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> {
            try {
                minecraft.setScreen(new com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen(createUi(packet), Component.empty()));
            } catch (Exception exception) {
                SimuKraft.LOGGER.error("Simukraft: Failed to set hire screen for sourceType={} role={}", packet.sourceType(), packet.role(), exception);
            }
        });
    }

    private static void reopen() {
        if (latestPacket != null) {
            SimuKraft.LOGGER.info("Simukraft: Reopening hire screen page={} selectedNpcId={}", currentPage, selectedNpcId);
            open(latestPacket);
        }
    }

    private static ModularUI createUi(NpcHireListResponsePacket packet) {
        try {
            int pageCount = totalPages(packet.candidates());
            currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));
            int screenWidth = Math.max(320, Minecraft.getInstance().getWindow().getGuiScaledWidth());
            int screenHeight = Math.max(240, Minecraft.getInstance().getWindow().getGuiScaledHeight());

            UIElement root = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
            });
            root.addChild(SimuKraftUiTheme.createShellPanel(screenWidth, screenHeight));

            Button backButton = new Button();
            backButton.setText(Component.translatable("gui.button.back"));
            backButton.setOnClick(event -> returnToSource(packet.sourceType(), packet.sourcePos()));
            backButton.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(5);
                layout.top(5);
                layout.width(45);
                layout.height(20);
            });
            root.addChild(backButton);

            root.addChild(textElement(Component.translatable(titleKey(packet.role())), screenWidth, SimuKraftUiTheme.TEXT_PRIMARY_COLOR, TextTexture.TextType.NORMAL).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(0);
                layout.top(25);
                layout.width(screenWidth);
                layout.height(14);
            }));

            Component status = packet.candidates().isEmpty()
                    ? Component.translatable("message.simukraft.no_idle_npcs")
                    : Component.translatable("gui.select_npc.title", packet.candidates().size(), currentPage + 1, pageCount);
            int statusColor = packet.candidates().isEmpty() ? SimuKraftUiTheme.TEXT_ERROR_COLOR : SimuKraftUiTheme.TEXT_SUCCESS_COLOR;
            root.addChild(textElement(status, screenWidth, statusColor, TextTexture.TextType.NORMAL).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(0);
                layout.top(45);
                layout.width(screenWidth);
                layout.height(14);
            }));

            List<NpcHireListResponsePacket.HireCandidate> pageCandidates = pageCandidates(packet.candidates(), currentPage);
            int availableWidth = screenWidth - 40;
            int availableHeight = screenHeight - TOP_MARGIN - BOTTOM_MARGIN - 50;
            int maxCardWidth = (availableWidth - (COLUMNS - 1) * CARD_GAP) / COLUMNS;
            int rows = (int) Math.ceil((double) Math.max(1, pageCandidates.size()) / COLUMNS);
            int actualCardWidth = Math.max(MIN_CARD_WIDTH, Math.min(PREFERRED_CARD_WIDTH, maxCardWidth));
            int maxCardHeight = rows > 0 ? (availableHeight - (rows - 1) * CARD_GAP) / rows : PREFERRED_CARD_HEIGHT;
            int actualCardHeight = Math.max(MIN_CARD_HEIGHT, Math.min(PREFERRED_CARD_HEIGHT, maxCardHeight));
            int totalWidth = COLUMNS * actualCardWidth + (COLUMNS - 1) * CARD_GAP;
            int totalHeight = rows * actualCardHeight + (rows - 1) * CARD_GAP;
            int startX = (screenWidth - totalWidth) / 2;
            int startY = Math.max(TOP_MARGIN, TOP_MARGIN + (availableHeight - totalHeight) / 2);
            for (int i = 0; i < pageCandidates.size(); i++) {
                int row = i / COLUMNS;
                int col = i % COLUMNS;
                int x = startX + col * (actualCardWidth + CARD_GAP);
                int y = startY + row * (actualCardHeight + CARD_GAP);
                root.addChild(candidateCard(packet, pageCandidates.get(i), x, y, actualCardWidth, actualCardHeight));
            }

            if (pageCount > 1) {
                root.addChild(textElement(Component.translatable("gui.pagination.info", currentPage + 1, pageCount), screenWidth, SimuKraftUiTheme.TEXT_SECONDARY_COLOR, TextTexture.TextType.NORMAL).layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(0);
                    layout.top(screenHeight - 40);
                    layout.width(screenWidth);
                    layout.height(12);
                }));
            }

            if (selectedNpcId != null) {
                String selectedName = selectedNpcName(packet.candidates(), selectedNpcId);
                root.addChild(textElement(Component.translatable("gui.select_npc.selected", selectedName), screenWidth, SimuKraftUiTheme.TEXT_WARNING_COLOR, TextTexture.TextType.NORMAL).layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(0);
                    layout.top(screenHeight - 60);
                    layout.width(screenWidth);
                    layout.height(12);
                }));
            }

            Button prevButton = new Button();
            prevButton.setText(Component.translatable("gui.pagination.previous"));
            prevButton.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(screenWidth / 2 - 100);
                layout.top(screenHeight - 30);
                layout.width(80);
                layout.height(20);
            });
            if (currentPage > 0) {
                prevButton.setOnClick(event -> {
                    currentPage--;
                    reopen();
                });
            } else {
                prevButton.setActive(false);
            }
            root.addChild(prevButton);

            Button nextButton = new Button();
            nextButton.setText(Component.translatable("gui.pagination.next"));
            nextButton.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(screenWidth / 2 + 20);
                layout.top(screenHeight - 30);
                layout.width(80);
                layout.height(20);
            });
            if (currentPage < pageCount - 1) {
                nextButton.setOnClick(event -> {
                    currentPage++;
                    reopen();
                });
            } else {
                nextButton.setActive(false);
            }
            root.addChild(nextButton);

            Button confirmButton = new Button();
            confirmButton.setText(Component.translatable("gui.button.hire"));
            confirmButton.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.right(10);
                layout.top(screenHeight - 30);
                layout.width(80);
                layout.height(20);
            });
            if (selectedNpcId != null) {
                confirmButton.setOnClick(event -> {
                    PacketDistributor.sendToServer(new NpcHireAssignPacket(packet.sourcePos(), packet.sourceType(), packet.role(), selectedNpcId));
                    returnToSource(packet.sourceType(), packet.sourcePos());
                });
            } else {
                confirmButton.setActive(false);
            }
            root.addChild(confirmButton);

            return new ModularUI(SimuKraftUiTheme.createUi(root)).shouldCloseOnEsc(true).shouldCloseOnKeyInventory(false);
        } catch (Exception exception) {
            SimuKraft.LOGGER.error("Simukraft: Failed to build hire UI sourceType={} role={} page={} selectedNpcId={}",
                    packet.sourceType(), packet.role(), currentPage, selectedNpcId, exception);
            throw exception;
        }
    }

    private static UIElement candidateCard(NpcHireListResponsePacket packet, NpcHireListResponsePacket.HireCandidate candidate, int x, int y, int width, int height) {
        try {
            boolean selected = candidate.citizenId().equals(selectedNpcId);
            int buttonInset = 2;
            int buttonWidth = Math.max(1, width - buttonInset * 2);
            int buttonHeight = Math.max(1, height - buttonInset * 2);
            UIElement wrapper = new UIElement().layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(x);
                layout.top(y);
                layout.width(width);
                layout.height(height);
            });
            wrapper.addChild(SimuKraftUiTheme.createDecorationLayer(3, 4, width - 4, height - 4, "simukraft_card_shadow"));

            Button card = new Button().noText();
            card.addClass("simukraft_large_button");
            card.layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(buttonInset);
                layout.top(buttonInset);
                layout.width(buttonWidth);
                layout.height(buttonHeight);
                layout.paddingAll(6);
            });
            card.addClass("simukraft_card_button");
            card.setOnClick(event -> {
                selectedNpcId = candidate.citizenId();
                reopen();
            });
            card.addChild(SimuKraftUiTheme.createDecorationLayer(6, 7, buttonWidth - 12, buttonHeight - 14, "simukraft_card_content_panel"));
            card.addChild(SimuKraftUiTheme.createDecorationLayer(6, 8, HEAD_SIZE + 4, HEAD_SIZE + 4, "simukraft_card_slot"));

            card.addChild(CitizenAvatarFactory.createHead(candidate.skinPath(), 0xFFFFFFFF).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(8);
                layout.top(10);
                layout.width(HEAD_SIZE);
                layout.height(HEAD_SIZE);
            }));

            int textLeft = HEAD_SIZE + 18;
            int textWidth = buttonWidth - textLeft - 10;

            card.addChild(infoLine(Component.literal(candidate.name()), textWidth, CARD_TEXT_COLOR).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(textLeft);
                layout.top(10);
                layout.width(textWidth);
                layout.height(14);
            }));
            card.addChild(infoLine(Component.translatable("work_status.idle"), textWidth, CARD_TEXT_COLOR).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(textLeft);
                layout.top(28);
                layout.width(textWidth);
                layout.height(14);
            }));
            card.addChild(levelBadge(candidate.skillLevel()).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(textLeft);
                layout.top(42);
                layout.width(34);
                layout.height(12);
            }));

            card.addChild(expBar(candidate, buttonWidth, buttonHeight).layout(layout -> {
                layout.positionType(TaffyPosition.ABSOLUTE);
                layout.left(8);
                layout.top(buttonHeight - 18);
                layout.width(buttonWidth - 16);
                layout.height(10);
            }));
            wrapper.addChild(card);
            if (selected) {
                wrapper.addChild(SimuKraftUiTheme.createSelectionBorder(width, height));
            }

            return wrapper;
        } catch (Exception exception) {
            SimuKraft.LOGGER.error("Simukraft: Failed to build hire candidate card id={} name={} skinPath={} level={}",
                    candidate.citizenId(), candidate.name(), candidate.skinPath(), candidate.skillLevel(), exception);
            throw exception;
        }
    }

    private static UIElement expBar(NpcHireListResponsePacket.HireCandidate candidate, int width, int height) {
        int level = Math.max(1, candidate.skillLevel());
        int currentXp = getXpForCurrentLevel(level);
        int nextXp = getXpForNextLevel(level);
        boolean isMaxLevel = nextXp < 0;
        float progress = isMaxLevel ? 1.0F : 0.35F;
        int barWidth = width - 16;
        String expText = isMaxLevel ? "MAX" : currentXp + "/" + nextXp;

        ProgressBar bar = new ProgressBar();
        bar.setRange(0.0F, 1.0F);
        bar.setProgress(progress);
        bar.label.setText(expText);
        return bar.layout(layout -> {
            layout.width(barWidth);
            layout.height(10);
        });
    }


    private static UIElement levelBadge(int level) {
        UIElement badge = new UIElement();
        badge.addClass("simukraft_badge");
        badge.style(style -> style.backgroundTexture(new TextTexture("Lv " + level).setWidth(34).setType(TextTexture.TextType.NORMAL).setColor(CARD_TEXT_COLOR).setDropShadow(false)));
        return badge;
    }

    private static UIElement infoLine(Component text, int width, int color) {
        UIElement element = new UIElement();
        element.style(style -> style.backgroundTexture(new TextTexture(text.getString())
                .setWidth(width)
                .setType(TextTexture.TextType.LEFT)
                .setColor(color)
                .setDropShadow(false)));
        return element;
    }

    private static UIElement textElement(Component text, int width, int color, TextTexture.TextType type) {
        UIElement element = new UIElement();
        element.style(style -> style.backgroundTexture(new TextTexture(text.getString())
                .setWidth(width)
                .setType(type)
                .setColor(color)
                .setDropShadow(true)));
        return element;
    }

    private static List<NpcHireListResponsePacket.HireCandidate> pageCandidates(List<NpcHireListResponsePacket.HireCandidate> candidates, int page) {
        int start = Math.max(0, page) * NPC_PER_PAGE;
        int end = Math.min(start + NPC_PER_PAGE, candidates.size());
        if (start >= end) {
            return new ArrayList<>();
        }
        return new ArrayList<>(candidates.subList(start, end));
    }

    private static String selectedNpcName(List<NpcHireListResponsePacket.HireCandidate> candidates, UUID selectedId) {
        for (NpcHireListResponsePacket.HireCandidate candidate : candidates) {
            if (candidate.citizenId().equals(selectedId)) {
                return candidate.name();
            }
        }
        return "";
    }

    private static int getXpForCurrentLevel(int level) {
        return switch (level) {
            case 1 -> 0;
            case 2 -> 50;
            case 3 -> 150;
            case 4 -> 350;
            case 5 -> 650;
            case 6 -> 1150;
            case 7 -> 1850;
            default -> 2850;
        };
    }

    private static int getXpForNextLevel(int level) {
        return switch (level) {
            case 1 -> 50;
            case 2 -> 150;
            case 3 -> 350;
            case 4 -> 650;
            case 5 -> 1150;
            case 6 -> 1850;
            case 7 -> 2850;
            default -> -1;
        };
    }

    private static void returnToSource(String sourceType, BlockPos sourcePos) {
        if ("build_box".equalsIgnoreCase(sourceType)) {
            BuildBoxScreenOpener.open(sourcePos);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private static String titleKey(String role) {
        return "planner".equalsIgnoreCase(role) ? "gui.hire_planner.title" : "gui.hire_builder.title";
    }

    private static int totalPages(List<NpcHireListResponsePacket.HireCandidate> candidates) {
        return Math.max(1, (int) Math.ceil(Math.max(1, candidates.size()) / 6.0D));
    }
}
