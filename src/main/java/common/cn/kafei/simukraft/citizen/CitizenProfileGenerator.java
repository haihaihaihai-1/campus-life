package common.cn.kafei.simukraft.citizen;

import net.minecraft.util.RandomSource;

import java.util.UUID;

public final class CitizenProfileGenerator {
    private static final String[] FAMILY_NAMES = {
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "朱", "马", "胡", "郭", "林", "何", "高", "郑",
            "罗", "梁", "谢", "宋", "唐", "许", "韩", "冯", "邓", "曹",
            "彭", "曾", "萧", "田", "董", "袁", "潘", "于", "蒋", "蔡",
            "魏", "薛", "叶", "阎", "余", "杜", "夏", "钟", "汪", "任",
            "姜", "范", "方", "石", "姚", "谭", "廖", "邹", "熊", "金",
            "陆", "郝", "孔", "白", "崔", "康", "毛", "邱", "秦", "江",
            "史", "顾", "侯", "邵", "孟", "龙", "万", "段", "雷", "钱",
            "汤", "尹", "黎", "易", "常", "武", "乔", "贺", "赖", "龚",
            "文", "庞", "樊", "兰", "殷", "施", "陶", "洪", "翟", "安",
            "颜", "倪", "严", "牛", "温", "季", "俞", "章", "鲁", "葛",
            "伍", "韦", "申", "尤", "毕", "聂", "焦", "向", "柳", "邢",
            "岳", "齐", "梅", "莫", "庄", "辛", "管", "祝", "左", "涂",
            "谷", "祁", "时", "舒", "耿", "卜", "詹", "关", "苗", "凌",
            "费", "纪", "靳", "盛", "童", "欧", "甄", "项", "曲", "成",
            "游", "阳", "裴", "席", "卫", "屈", "鲍", "覃", "霍", "翁",
            "甘", "景", "柯", "阮", "桂", "闵", "欧阳", "诸葛", "上官", "司马",
            "东方", "皇甫", "慕容", "司徒", "端木", "公孙", "轩辕", "令狐", "钟离", "宇文",
            "长孙", "鲜于", "澹台", "淳于", "太叔", "申屠", "仲孙", "颛孙", "巫马", "公西"
    };
    private static final String[] GIVEN_NAMES = {
            "叙白", "砚丞", "翊珩", "昭野", "淮序", "既明", "晏清", "昀朗", "叙深", "砚舟",
            "允墨", "景曜", "叙川", "淮之", "昭临", "砚知", "翊川", "既望", "晏桥", "昀野",
            "淮安", "砚书", "翊声", "景深", "允和", "既白", "晏声", "昭棠", "叙柔", "砚卿",
            "淮月", "既夏", "晏宁", "昀舒", "允笙", "翊乔", "景芊", "叙蘅", "淮蘅", "景蘅",
            "叙棠", "砚棠", "既棠", "晏棠", "昀棠", "叙月", "砚月", "既月", "晏月", "昀月",
            "叙珩", "淮珩", "昭珩", "砚珩", "允珩", "既珩", "晏珩", "昀珩", "叙朗", "淮朗",
            "云深", "星河", "墨染", "清欢", "瑾瑜", "修远", "明轩", "浩然", "子衿", "若水",
            "逸尘", "瑾年", "清扬", "子墨", "云帆", "景行", "修竹", "明德", "致远", "清和",
            "子谦", "云逸", "修文", "明志", "志远", "清泉", "子安", "云开", "修齐", "明远",
            "志诚", "清风", "子建", "云舒", "修身", "明理", "志强", "清韵", "子瑜", "云锦",
            "修心", "明义", "志明", "清音", "子骞", "云翔", "修道", "明道", "志高", "清波",
            "子龙", "云涛", "修德", "明法", "清辉", "子期", "云海", "修业", "明礼", "志新",
            "清影", "子敬", "云峰", "修睦", "明仁", "志勇", "清霜", "子昂", "云汉", "明智",
            "志华", "清露", "子真", "云梦", "修静", "明慧", "志坚", "清秋", "子美", "云泽",
            "修雅", "明达", "清寒", "子文", "云溪", "明哲", "志宏", "清晓", "子健", "云霓",
            "修诚", "明诚", "清夜", "子厚", "云衢", "明敬", "清昼", "子方", "云翼", "景泰"
    };
    private static final int MALE_SKIN_COUNT = 60;
    private static final int FEMALE_SKIN_COUNT = 60;

    private CitizenProfileGenerator() {
    }

    public static void fillMissingProfile(CitizenData data, RandomSource random, long gameDay) {
        if (isMissingGender(data.gender())) {
            data.setGender(random.nextDouble() < 0.5D ? "male" : "female");
        }
        if (data.name().isBlank()) {
            data.setName(createName(data.gender(), random));
        }
        if (data.skinPath().isBlank()) {
            data.setSkinPath(createSkinPath(data.gender(), data.uuid()));
        }
        if (data.age() <= 0) {
            data.setAge(18 + random.nextInt(18));
        }
        if (data.lifespan() <= 0) {
            data.setLifespan(70 + random.nextInt(21));
        }
        if (data.bornDay() <= 0L) {
            data.setBornDay(gameDay - data.age() * 365L);
        }
    }

    private static boolean isMissingGender(String gender) {
        return gender == null || gender.isBlank() || "unknown".equalsIgnoreCase(gender);
    }

    private static String createName(String gender, RandomSource random) {
        String familyName = FAMILY_NAMES[random.nextInt(FAMILY_NAMES.length)];
        return familyName + GIVEN_NAMES[random.nextInt(GIVEN_NAMES.length)];
    }

    private static String createSkinPath(String gender, UUID uuid) {
        int skinCount = "female".equals(gender) ? FEMALE_SKIN_COUNT : MALE_SKIN_COUNT;
        int index = Math.floorMod(uuid.hashCode(), skinCount);
        String prefix = "female".equals(gender) ? "custom_female_entity_" : "custom_male_entity_";
        String folder = "female".equals(gender) ? "female" : "male";
        return "simukraft:textures/entity/" + folder + "/" + prefix + index + ".png";
    }
}
