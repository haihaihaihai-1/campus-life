package common.campuslife.block.entity;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * BlockEntity类型注册。
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimuKraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StartupCoreBlockEntity>> STARTUP_CORE =
        BLOCK_ENTITIES.register("startup_core", () ->
            BlockEntityType.Builder.of(StartupCoreBlockEntity::new,
                ModBlocks.STARTUP_CORE.get())
                .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SalesStallBlockEntity>> SALES_STALL =
        BLOCK_ENTITIES.register("sales_stall", () ->
            BlockEntityType.Builder.of(SalesStallBlockEntity::new,
                ModBlocks.SALES_STALL.get())
                .build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
