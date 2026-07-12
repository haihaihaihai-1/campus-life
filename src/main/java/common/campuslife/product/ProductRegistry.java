package common.campuslife.product;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * 产品注册表。
 *
 * 管理所有可生产的产品配方。
 * 每个产品有多个属性，影响NPC购买决策：
 * - tier: 品质等级1-5
 * - cost: 生产成本
 * - craftTime: 制作时间(秒)
 * - ingredients: 所需原料
 * - shelfLife: 保质期(tick)
 * - satisfaction: 基础满意度(0-100)
 * - innovation: 创新度(研发提升)
 * - brandBonus: 品牌加成
 * - resultItem: 产出物品
 * - resultCount: 产出数量
 */
public final class ProductRegistry {

    private static final Map<String, Product> PRODUCTS = new LinkedHashMap<>();

    private ProductRegistry() {}

    /**
     * 注册初始产品。
     */
    static {
        register(new Product(
            "coffee_basic", "基础咖啡", Product.Category.FOOD, 1,
            5.0f, 30, new ItemStack[]{
                new ItemStack(Items.WHEAT, 2),
                new ItemStack(Items.COCOA_BEANS, 1)
            },
            1200, 40.0f, 0.0f, 0.0f,
            common.cn.kafei.simukraft.registry.ModItems.COFFEE_BASIC.get(), 1
        ));

        register(new Product(
            "bread_basic", "基础面包", Product.Category.FOOD, 1,
            3.0f, 20, new ItemStack[]{
                new ItemStack(Items.WHEAT, 3)
            },
            2400, 50.0f, 0.0f, 0.0f,
            common.cn.kafei.simukraft.registry.ModItems.BREAD_BASIC.get(), 1
        ));

        register(new Product(
            "milk_tea", "珍珠奶茶", Product.Category.FOOD, 2,
            8.0f, 45, new ItemStack[]{
                new ItemStack(Items.WHEAT, 2),
                new ItemStack(Items.MILK_BUCKET, 1),
                new ItemStack(Items.SUGAR, 1)
            },
            1200, 60.0f, 0.1f, 0.0f,
            common.cn.kafei.simukraft.registry.ModItems.MILK_TEA.get(), 1
        ));

        register(new Product(
            "study_notes", "学习笔记", Product.Category.TOOL, 1,
            2.0f, 15, new ItemStack[]{
                new ItemStack(Items.PAPER, 3),
                new ItemStack(Items.INK_SAC, 1)
            },
            4800, 35.0f, 0.0f, 0.0f,
            common.cn.kafei.simukraft.registry.ModItems.STUDY_NOTES.get(), 1
        ));

        register(new Product(
            "calculator", "简易计算器", Product.Category.TOOL, 2,
            15.0f, 60, new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.STONE_BUTTON, 2),
                new ItemStack(Items.PAPER, 1)
            },
            4800, 55.0f, 0.2f, 0.0f,
            common.cn.kafei.simukraft.registry.ModItems.CALCULATOR.get(), 1
        ));
    }

    /**
     * 注册产品。
     */
    public static void register(Product product) {
        PRODUCTS.put(product.getId(), product);
    }

    /**
     * 获取产品。
     */
    public static Product get(String id) {
        return PRODUCTS.get(id);
    }

    /**
     * 获取所有产品。
     */
    public static Collection<Product> getAll() {
        return Collections.unmodifiableCollection(PRODUCTS.values());
    }

    /**
     * 获取指定分类的产品。
     */
    public static List<Product> getByCategory(Product.Category category) {
        List<Product> result = new ArrayList<>();
        for (Product product : PRODUCTS.values()) {
            if (product.getCategory() == category) {
                result.add(product);
            }
        }
        return result;
    }

    /**
     * 获取产品总数。
     */
    public static int size() {
        return PRODUCTS.size();
    }
}
