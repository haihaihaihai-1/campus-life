package common.campuslife.network;

import common.campuslife.block.entity.StartupCoreBlockEntity;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端->服务端：请求开始生产产品。
 * 玩家在GUI点击产品按钮时发送。
 */
public record CraftRequestPayload(BlockPos pos, String productId) implements CustomPacketPayload {

    public static final Type<CraftRequestPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SimuKraft.MOD_ID, "craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPayload> STREAM_CODEC =
        StreamCodec.of(CraftRequestPayload::encode, CraftRequestPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buf, CraftRequestPayload packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeUtf(packet.productId());
    }

    public static CraftRequestPayload decode(RegistryFriendlyByteBuf buf) {
        return new CraftRequestPayload(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(CraftRequestPayload packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            var level = player.serverLevel();
            var be = level.getBlockEntity(packet.pos());
            if (be instanceof StartupCoreBlockEntity coreEntity) {
                coreEntity.startCraft(packet.productId(), player);
            }
        }
    }
}
