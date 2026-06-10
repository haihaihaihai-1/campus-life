# New Sim-U-Kraft 1.21.1

Sim-U-Kraft 新版重写项目，目标是把旧版城市、NPC、建筑、经济和生产玩法迁移到 Minecraft 1.21.1 / NeoForge，并用服务层、SQLite、客户端同步和可维护的配置体系重新实现。

## 当前目标环境

```text
Minecraft 1.21.1
NeoForge 21.1.x
Java 21
Gradle
SQLite
LDLib/LDLib2 UI
```

## 当前重点

- 城市系统：创建城市、认领区块、成员权限、资金流水、城市 POI 与客户端城市地图。
- NPC 系统：市民实体、雇佣、职业状态、夜间回家、寻路、吃饭表现和饱食度。
- 建筑系统：建筑盒、建筑资源、预览、建造任务、规划师任务、住宅床位和拆除链路。
- 生产系统：农田真实耕种、商业交易、工业 JSON 工作流、真实机器步骤和物流基础表。
- 客户端兼容：LDLib 文本框中文输入兼容、Xaero World Map 城市区块高亮。

## 最近同步

2026-06-10：

- NPC 投喂按旧版体验走地面物品：玩家丢出的食物可被附近未吃饱 NPC 拾取并消耗；工作中的无主掉落物继续被保护，避免吃掉生产物。
- NPC 饱食度从 `CitizenData` / SQLite 迁出，唯一持久化来源是 `CitizenEntity` 的 `Hunger` NBT；旧数据库 `hunger` 列会在 schema 初始化时迁移删除。
- LDLib 文本框通过反射软兼容 IMBlocker，避免输入框无法输入中文，同时不对未安装 IMBlocker 的环境产生硬依赖。
- 参考旧版接入 Xaero World Map，在大地图/提示中显示城市认领区块、城市边界和城市名称。

## 文档索引

- [新版重写当前进度与后续计划](docs/新版重写当前进度与后续计划.md)
- [接口用法索引](docs/接口用法.md)
- [LDLib2 1.21 使用总结](docs/LDLib2-1.21-summary.md)
- [商业商店 JSON 自定义教程](docs/commercial_customization.md)
- [工业系统自定义说明](docs/industrial_customization.md)
- [工业真实机器操作说明](docs/真实操作方块.md)

## 常用命令

```powershell
.\gradlew compileJava
.\gradlew testClasses
.\gradlew test
```

开发原则：服务端是权威端，GUI 只发起请求和显示结果；长期业务数据优先通过 Service/Manager/SQLite 链路保存，实体运行时状态按对应系统独立持久化。
