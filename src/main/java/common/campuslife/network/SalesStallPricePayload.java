package common.campuslife.network;

import common.campuslife.block.entity.SalesStallBlockEntity;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端->服务端：设置销售摊位价格。
 * 玩家点击GUI里的+/-按钮时发送。
 */
public record SalesStallPricePayload(BlockPos pos, int newPrice) implements CustomPacketPayload {

    public static final Type<SalesStallPricePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SimuKraft.MOD_ID, "sales_stall_price"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SalesStallPricePayload> STREAM_CODEC =
        StreamCodec.of(SalesStallPricePayload::encode, SalesStallPricePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SalesStallPricePayload packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeInt(packet.newPrice());
    }

    public static SalesStallPricePayload decode(RegistryFriendlyByteBuf buf) {
        return new SalesStallPricePayload(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(SalesStallPricePayload packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            var level = player.serverLevel();
            var be = level.getBlockEntity(packet.pos());
            if (be instanceof SalesStallBlockEntity stall) {
                stall.setPrice(packet.newPrice());
            }
        }
    }
}
