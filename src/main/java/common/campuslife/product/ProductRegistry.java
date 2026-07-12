package common.campuslife.product;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * 产品注册表。
 * 
 * 管理所有可生产的产品配方。
 * 玩家通过GUI从中选择要生产的产品。
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
            1200, 40.0f, 0.0f, 0.0f
        ));

        register(new Product(
            "bread_basic", "基础面包", Product.Category.FOOD, 1,
            3.0f, 20, new ItemStack[]{
                new ItemStack(Items.WHEAT, 3)
            },
            2400, 50.0f, 0.0f, 0.0f
        ));

        register(new Product(
            "奶茶", "珍珠奶茶", Product.Category.FOOD, 2,
            8.0f, 45, new ItemStack[]{
                new ItemStack(Items.WHEAT, 2),
                new ItemStack(Items.MILK_BUCKET, 1),
                new ItemStack(Items.SUGAR, 1)
            },
            1200, 60.0f, 0.1f, 0.0f
        ));

        register(new Product(
            "学习笔记", "学习笔记", Product.Category.TOOL, 1,
            2.0f, 15, new ItemStack[]{
                new ItemStack(Items.PAPER, 3),
                new ItemStack(Items.INK_SAC, 1)
            },
            4800, 35.0f, 0.0f, 0.0f
        ));

        register(new Product(
            "计算器", "简易计算器", Product.Category.TOOL, 2,
            15.0f, 60, new ItemStack[]{
                new ItemStack(Items.REDSTONE, 3),
                new ItemStack(Items.STONE_BUTTON, 2),
                new ItemStack(Items.PAPER, 1)
            },
            4800, 55.0f, 0.2f, 0.0f
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
