package client.campuslife.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 产品选择按钮。
 * 点击后发送CraftRequestPayload到服务端。
 */
public class ProductButton extends Button {

    private final String productId;

    public ProductButton(int x, int y, int width, int height, Component label, String productId, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
