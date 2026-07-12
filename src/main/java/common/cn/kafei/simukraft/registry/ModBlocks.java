package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.block.CommercialControlBoxBlock;
import common.cn.kafei.simukraft.block.CityCoreBlock;
import common.cn.kafei.simukraft.block.FarmlandBoxBlock;
import common.cn.kafei.simukraft.block.IndustrialControlBoxBlock;
import common.cn.kafei.simukraft.block.LogisticsClientBoxBlock;
import common.cn.kafei.simukraft.block.LogisticsServerBoxBlock;
import common.cn.kafei.simukraft.block.MilkLiquidBlock;
import common.cn.kafei.simukraft.block.ResidentialControlBoxBlock;
import common.campuslife.block.DisplayRackBlock;
import common.campuslife.block.EmployeeStationBlock;
import common.campuslife.block.ResearchStationBlock;
import common.campuslife.block.SalesStallBlock;
import common.campuslife.block.StartupCoreBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@SuppressWarnings("null")
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimuKraft.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimuKraft.MOD_ID);

    public static final DeferredBlock<Block> BLUE_LIGHT_BLOCK = registerBlock("blue_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> BUILD_BOX = registerBlock("build_box", common.cn.kafei.simukraft.block.BuildBoxBlock::new);
    public static final DeferredBlock<Block> CHEESE_BLOCK = registerBlock("cheese_block", ModBlocks::cheeseBlock);
    public static final DeferredBlock<Block> CITY_CORE = registerBlock("city_core", CityCoreBlock::new);
    public static final DeferredBlock<Block> COMMERCIAL_CONTROL_BOX = registerBlock("commercial_control_box", CommercialControlBoxBlock::new);
    public static final DeferredBlock<Block> GREEN_LIGHT_BLOCK = registerBlock("green_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> INDUSTRIAL_CONTROL_BOX = registerBlock("industrial_control_box", IndustrialControlBoxBlock::new);
    public static final DeferredBlock<Block> LOGISTICS_CLIENT_BOX = registerBlock("logistics_client_box", LogisticsClientBoxBlock::new);
    public static final DeferredBlock<Block> LOGISTICS_SERVER_BOX = registerBlock("logistics_server_box", LogisticsServerBoxBlock::new);
    public static final DeferredBlock<LiquidBlock> MILK_BLOCK = BLOCKS.register("milk_fluid", ModBlocks::milkBlock);
    public static final DeferredBlock<Block> NSUK_FARMLAND_BOX = registerBlock("nsuk_farmland_box", FarmlandBoxBlock::new);
    public static final DeferredBlock<Block> ORANGE_LIGHT_BLOCK = registerBlock("orange_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> OTHER_CONTROL_BOX = registerBlock("other_control_box", ModBlocks::controlBox);
    public static final DeferredBlock<Block> PURPLE_LIGHT_BLOCK = registerBlock("purple_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> RAINBOW_LIGHT_BLOCK = registerBlock("rainbow_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> RED_LIGHT_BLOCK = registerBlock("red_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> RESIDENTIAL_CONTROL_BOX = registerBlock("residential_control_box", ResidentialControlBoxBlock::new);
    public static final DeferredBlock<Block> WHITE_LIGHT_BLOCK = registerBlock("white_light_block", ModBlocks::lightBlock);
    public static final DeferredBlock<Block> YELLOW_LIGHT_BLOCK = registerBlock("yellow_light_block", ModBlocks::lightBlock);

    // Campus Life blocks
    public static final DeferredBlock<Block> STARTUP_CORE = registerBlock("startup_core", StartupCoreBlock::new);
    public static final DeferredBlock<Block> RESEARCH_STATION = registerBlock("research_station", ResearchStationBlock::new);
    public static final DeferredBlock<Block> EMPLOYEE_STATION = registerBlock("employee_station", EmployeeStationBlock::new);
    public static final DeferredBlock<Block> DISPLAY_RACK = registerBlock("display_rack", DisplayRackBlock::new);
    public static final DeferredBlock<Block> SALES_STALL = registerBlock("sales_stall", SalesStallBlock::new);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static DeferredBlock<Block> registerBlock(String name, Supplier<Block> blockSupplier) {
        DeferredBlock<Block> block = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static Block controlBox() {
        return new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.8F).sound(SoundType.METAL));
    }

    private static Block lightBlock() {
        return new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.GLASS).lightLevel(state -> 15));
    }

    @SuppressWarnings("deprecation")
    private static Block cheeseBlock() {
        return new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.SLIME_BLOCK).sound(SoundType.SLIME_BLOCK));
    }

    private static LiquidBlock milkBlock() {
        return new MilkLiquidBlock(ModFluids.SOURCE_MILK.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable().randomTicks());
    }
}
