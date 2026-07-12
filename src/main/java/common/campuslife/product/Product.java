package common.campuslife.product;

import net.minecraft.world.item.ItemStack;

/**
 * 产品定义。
 * 
 * 每个产品有多个属性，影响NPC购买决策：
 * - tier: 品质等级1-5
 * - cost: 生产成本
 * - craftTime: 制作时间(秒)
 * - ingredients: 所需原料
 * - shelfLife: 保质期(tick)
 * - satisfaction: 基础满意度(0-100)
 * - innovation: 创新度(研发提升)
 * - brandBonus: 品牌加成
 */
public final class Product {

    private final String id;
    private final String name;
    private final Category category;
    private final int tier;
    private final float cost;
    private final int craftTime;
    private final ItemStack[] ingredients;
    private final int shelfLife;
    private final float satisfaction;
    private final float innovation;
    private final float brandBonus;

    public Product(String id, String name, Category category, int tier, float cost,
                   int craftTime, ItemStack[] ingredients, int shelfLife, 
                   float satisfaction, float innovation, float brandBonus) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.tier = tier;
        this.cost = cost;
        this.craftTime = craftTime;
        this.ingredients = ingredients;
        this.shelfLife = shelfLife;
        this.satisfaction = satisfaction;
        this.innovation = innovation;
        this.brandBonus = brandBonus;
    }

    // === Getters ===

    public String getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public int getTier() { return tier; }
    public float getCost() { return cost; }
    public int getCraftTime() { return craftTime; }
    public ItemStack[] getIngredients() { return ingredients; }
    public int getShelfLife() { return shelfLife; }
    public float getSatisfaction() { return satisfaction; }
    public float getInnovation() { return innovation; }
    public float getBrandBonus() { return brandBonus; }

    /**
     * 产品分类。
     */
    public enum Category {
        FOOD("食品"),
        TOOL("工具"),
        SERVICE("服务"),
        TECH("科技"),
        BRANDED("品牌产品");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    @Override
    public String toString() {
        return "Product{" + id + ", tier=" + tier + ", sat=" + satisfaction + "}";
    }
}
