package common.campuslife.npc;

import common.cn.kafei.simukraft.SimuKraft;
import common.campuslife.core.EconomyVariable;
import common.campuslife.core.WorldStateBoard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tier 2 员工NPC管理器。
 * 
 * 复用simukraft的CitizenEntity，通过外部服务控制其行为。
 * 
 * 员工NPC行为：
 * - 每个员工有岗位类型（实习生/销售/工程师/CTO/CEO）
 * - 根据岗位自动产出（原材料/金币/经验）
 * - 每天消耗工资（从玩家银行账户扣除）
 * - 忠诚度<30时可能被竞争对手挖走
 * - 所有决策使用规则驱动，零LLM调用
 * 
 * 活跃数量：10个
 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class EmployeeManager {

    /** 员工NPC数据缓存 */
    private static final Map<UUID, EmployeeData> EMPLOYEES = new ConcurrentHashMap<>();
    
    /** 最大员工数量 */
    private static final int MAX_EMPLOYEES = 10;
    
    private EmployeeManager() {}
    
    /**
     * 注册管理器。
     */
    public static void register() {
        NeoForge.EVENT_BUS.register(EmployeeManager.class);
        SimuKraft.LOGGER.info("EmployeeManager registered (Tier 2 employee NPCs: max={})", MAX_EMPLOYEES);
    }
    
    /**
     * 玩家尝试招聘员工。
     * 从玩家位置附近生成一个新的CitizenEntity，标记为员工。
     */
    public static boolean hireEmployee(ServerLevel level, UUID playerId, JobType jobType) {
        if (EMPLOYEES.size() >= MAX_EMPLOYEES) {
            return false;
        }
        
        // 生成实体
        var player = level.getPlayerByUUID(playerId);
        if (player == null) return false;
        
        var spawnPos = player.blockPosition().offset(
            level.random.nextInt(10) - 5, 0, level.random.nextInt(10) - 5
        );
        
        var citizenOpt = common.cn.kafei.simukraft.citizen.CitizenService.spawnCitizen(
            level,
            new Vec3(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ()),
            null,
            true
        );
        
        if (citizenOpt.isEmpty()) return false;
        
        UUID npcId = citizenOpt.get().getUUID();
        
        EmployeeData data = new EmployeeData(
            npcId,
            generateRandomName(),
            jobType,
            playerId,
            jobType.getBaseSalary()
        );
        
        EMPLOYEES.put(npcId, data);
        
        SimuKraft.LOGGER.info("Employee '{}' hired: {} (total: {})", data.name, jobType, EMPLOYEES.size());
        return true;
    }
    
    /**
     * 解雇员工。
     */
    public static boolean fireEmployee(UUID npcId) {
        EmployeeData data = EMPLOYEES.remove(npcId);
        if (data != null) {
            SimuKraft.LOGGER.info("Employee '{}' fired: {}", data.name, data.jobType);
            return true;
        }
        return false;
    }
    
    /**
     * Server tick事件 - 驱动所有员工行为。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        
        long tick = level.getGameTime();
        
        // 每5分钟计算一次产出
        if (tick % 6000 == 0) {
            calculateOutput(level);
        }
        
        // 每天结算一次工资
        if (tick % 24000 == 0) {
            paySalaries(level);
        }
    }
    
    /**
     * 计算员工产出。
     * 每个员工根据岗位产生对应的产出。
     */
    private static void calculateOutput(ServerLevel level) {
        float totalOutput = 0;
        
        for (EmployeeData employee : EMPLOYEES.values()) {
            // 忠诚度修正 (忠诚度越低，产出越低)
            float loyaltyMod = employee.loyalty / 100f;
            
            // 岗位基础产出
            float baseOutput = employee.jobType.getBaseOutput();
            
            // 实际产出 = 基础产出 * 忠诚度修正 * 随机因子
            float actualOutput = baseOutput * loyaltyMod * (0.8f + level.random.nextFloat() * 0.4f);
            
            totalOutput += actualOutput;
            
            // 更新员工等级产出累计
            employee.totalOutput += actualOutput;
            employee.outputCycles++;
        }
        
        // 更新世界状态板中的员工产出变量
        float currentEmployeeIncome = (float) WorldStateBoard.get(EconomyVariable.EMPLOYEE_INCOME);
        WorldStateBoard.set(EconomyVariable.EMPLOYEE_INCOME, currentEmployeeIncome + totalOutput, "employee_output");
        
        SimuKraft.LOGGER.debug("Employee output this cycle: {}", String.format("%.2f", totalOutput));
    }
    
    /**
     * 支付员工工资（每天）。
     * 从玩家账户扣除对应金额。
     */
    private static void paySalaries(ServerLevel level) {
        float totalSalary = 0;
        
        for (EmployeeData employee : EMPLOYEES.values()) {
            totalSalary += employee.salary;
            
            // 忠诚度变化：如果薪资低于市场平均，忠诚度下降
            float marketAvg = (float) WorldStateBoard.get(EconomyVariable.EMPLOYEE_MARKET_SALARY);
            if (employee.salary < marketAvg * 0.9f) {
                employee.loyalty = Math.max(0, employee.loyalty - 5);
            } else if (employee.salary > marketAvg * 1.1f) {
                employee.loyalty = Math.min(100, employee.loyalty + 2);
            }
        }
        
        // TODO: 从玩家银行账户扣除工资总额
        // 如果余额不足，进入破产预警
        
        // 更新世界状态板
        WorldStateBoard.set(EconomyVariable.EMPLOYEE_SALARY_COST, totalSalary, "salary_payment");
        
        SimuKraft.LOGGER.info("Paid daily salaries: {} (employees: {})", String.format("%.1f", totalSalary), EMPLOYEES.size());
    }
    
    /**
     * 检查员工是否被挖走。
     * 竞争对手NPC可能尝试挖走低忠诚度的员工。
     */
    public static void checkPoaching(ServerLevel level, UUID competitorId, float poachingStrength) {
        for (EmployeeData employee : EMPLOYEES.values()) {
            if (employee.loyalty < 30 && level.random.nextFloat() < poachingStrength * 0.1f) {
                // 员工被挖走
                SimuKraft.LOGGER.warn("Employee '{}' was poached! (loyalty={})", employee.name, employee.loyalty);
                EMPLOYEES.remove(employee.id);
            }
        }
    }
    
    /**
     * 获取所有员工UUID列表。
     */
    public static Collection<UUID> getEmployeeUUIDs() {
        return Collections.unmodifiableCollection(EMPLOYEES.keySet());
    }
    
    /**
     * 根据UUID获取员工数据。
     */
    public static EmployeeData getEmployee(UUID uuid) {
        return EMPLOYEES.get(uuid);
    }
    
    /**
     * 获取员工数量。
     */
    public static int getEmployeeCount() {
        return EMPLOYEES.size();
    }
    
    /**
     * 随机生成名字。
     */
    private static final String[] FAMILY_NAMES = {"张", "王", "李", "赵", "陈", "刘", "杨", "黄", "周", "吴"};
    private static final String[] GIVEN_NAMES = {"伟", "芳", "娜", "敏", "静", "丽", "强", "磊", "军", "洋"};
    
    private static String generateRandomName() {
        return FAMILY_NAMES[(int) (Math.random() * FAMILY_NAMES.length)] + 
               GIVEN_NAMES[(int) (Math.random() * GIVEN_NAMES.length)];
    }
    
    /**
     * 岗位类型枚举。
     */
    public enum JobType {
        INTERN("实习生", 100, 20, 10),
        SALESMAN("销售员", 300, 60, 30),
        ENGINEER("工程师", 800, 150, 50),
        MANAGER("经理", 2000, 400, 100),
        CTO("CTO", 5000, 1200, 300),
        CEO("CEO", 15000, 5000, 500);
        
        private final String displayName;
        private final float baseCost;      // 招聘成本
        private final float baseOutput;    // 基础产出/5min
        private final float baseSalary;    // 日薪
        
        JobType(String displayName, float baseCost, float baseOutput, float baseSalary) {
            this.displayName = displayName;
            this.baseCost = baseCost;
            this.baseOutput = baseOutput;
            this.baseSalary = baseSalary;
        }
        
        public String getDisplayName() { return displayName; }
        public float getBaseCost() { return baseCost; }
        public float getBaseOutput() { return baseOutput; }
        public float getBaseSalary() { return baseSalary; }
    }
    
    /**
     * 员工NPC运行时数据。
     */
    public static final class EmployeeData {
        public final UUID id;
        public final String name;
        public final JobType jobType;
        public final UUID employerId; // 雇主玩家ID
        
        public float salary;
        public float loyalty = 50f;
        public float totalOutput = 0;
        public long outputCycles = 0;
        public long hireDay;
        
        public EmployeeData(UUID id, String name, JobType jobType, UUID employerId, float salary) {
            this.id = id;
            this.name = name;
            this.jobType = jobType;
            this.employerId = employerId;
            this.salary = salary;
            this.hireDay = System.currentTimeMillis();
        }
        
        /**
         * 平均每周期产出。
         */
        public float getAverageOutput() {
            return outputCycles > 0 ? totalOutput / outputCycles : 0;
        }
    }
}
