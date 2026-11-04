# IT 资产管理系统

基于 JavaFX 的桌面资产管理工具，使用 MyBatis + SQLite 存储数据，支持资产增删改查、分类/部门管理、维修记录、操作日志、Excel 导出和数据库备份恢复。

资产列表默认按“部门 → 使用人 → 分类 → 资产 ID”降序排列，无部门的资产排在最后；顶部会实时显示当前筛选后的资产数量。资产处于“维修”状态时必须填写维修费用，状态变化会写入维修记录。

## 技术栈

- Java 21
- JavaFX 21
- MyBatis 3.5 + SQLite
- Apache POI（Excel 导出）
- Maven Shade + jpackage（打包）

## 分层结构

```
controller  →  service  →  mapper  →  SQLite
```

| 层 | 职责 | 位置 |
|---|---|---|
| Controller | FXML 绑定、表格/对话框、文件选择、提示框；不写 SQL，不管理事务 | `controller/` |
| Service | 参数校验、业务规则、事务边界、操作日志、报表/备份编排 | `service/` |
| Mapper | MyBatis 接口与 SQL 映射 | `mapper/` + `resources/mapper/` |
| Model | 资产、分类、部门、日志、状态枚举 | `model/` |
| Infrastructure | 数据库连接、建库、备份文件轮转 | `infrastructure/` |

事务统一由 `MyBatisUtil.inTransaction(...)` 管理：资产修改和日志写入要么一起提交，要么一起回滚。只读查询使用 `MyBatisUtil.query(...)`。

## 目录

```
src/main/java/com/company/assetmgmt/itassetmanagement/
├── Launcher.java                 程序入口
├── MainApp.java                  JavaFX 启动
├── controller/MainController.java
├── service/
│   ├── AssetService.java
│   ├── CategoryService.java
│   ├── DepartmentService.java
│   ├── LogService.java
│   ├── ReportService.java
│   ├── BackupService.java
│   ├── ServiceException.java
│   └── ServiceSupport.java
├── mapper/                       MyBatis Mapper 接口
├── model/                        Asset / Category / Department / OperationLog / AssetStatus
└── infrastructure/
    ├── db/                       DatabaseManager / MyBatisUtil
    └── backup/BackupManager.java
```

## 运行与构建

```bash
mvn javafx:run          # 开发运行
mvn clean package       # 打包（target/app-1.0-SNAPSHOT.jar）
```

Windows 下也可以用 `一键生成exe.bat` 调用 jpackage 生成安装包。

## 资产编号

资产编号不再手工填写，统一自动生成：

```text
{category.value}-{asset.id}
```

例如 `lpc-12`、`xsq-153`。新增或修改资产时资产编号只读显示，保存后自动生成。

分类值：

| 分类 | 分类值 |
|---|---|
| 笔记本电脑 | `lpc` |
| 台式电脑 | `dpc` |
| 显示器 | `xsq` |
| 办公外设 | `office` |
| 服务器设备 | `server` |
| 移动设备 | `laptop` |
| 网络设备 | `net` |

## 数据与备份

- 数据库：`data/asset.db`
- 退出时自动备份到 `backup/asset_时间戳.db`，只保留最近 10 份
- 手动备份和恢复通过左侧导航栏操作；恢复前会校验 SQLite 文件头，并先把当前库备份为 `backup/pre_restore_时间戳.db`

## 资产状态

| 编码 | 显示 |
|---|---|
| `IN_USE` | 使用中 |
| `REPAIR` | 维修 |
| `LENT` | 出借（必须填写使用人） |
| `IDLE` | 闲置（部门自动清空） |
| `TO_BE_SCRAPPED` | 待报废 |
| `SCRAPPED` | 报废 |

维修记录视图只展示操作日志中“资产送修，维修费用”的记录，可单独导出为 Excel；维修结束等其它日志不会进入该视图。
