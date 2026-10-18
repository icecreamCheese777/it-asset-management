-- ==========================================
-- 1. 资产类别表 (Category)
-- ==========================================
CREATE TABLE IF NOT EXISTS category (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键ID：系统自动递增的唯一标识
                                        name TEXT NOT NULL UNIQUE              -- 类别名称：如"笔记本电脑"、"服务器"（不可重复）
);

-- ==========================================
-- 2. 部门表 (Department)
-- ==========================================
CREATE TABLE IF NOT EXISTS department (
                                          id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键ID：系统自动递增的唯一标识
                                          name TEXT NOT NULL UNIQUE              -- 部门名称：如"研发部"、"财务部"（不可重复）
);

-- ==========================================
-- 3. 资产主表 (Asset) - 已去除入库日期
-- ==========================================
CREATE TABLE IF NOT EXISTS asset (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键ID：系统自动递增的唯一标识
                                     asset_no TEXT NOT NULL UNIQUE,         -- 资产编号：人为赋予的业务编号，如"NB-2024-001"（不可重复）
                                     name TEXT NOT NULL,                    -- 资产名称：设备的名称，如"ThinkPad X1"
                                     category_id INTEGER NOT NULL,          -- 类别ID：关联资产类别表(category)，表明它是什么类型的资产
                                     status TEXT NOT NULL,                  -- 状态：如"IN_USE"(使用中)、"IDLE"(闲置)、"SCRAPPED"(已报废)
                                     owner_dept_id INTEGER,                 -- 部门ID：关联部门表(department)，记录资产归属哪个部门
                                     device_params TEXT,                    -- 设备参数：记录硬件规格，如"i7/16G内存/512G固态"
                                     created_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- 创建时间(即入库录入时间)：该资产被录入系统的精确时间
                                     updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- 更新时间：该条记录最后一次被修改的时间
                                     FOREIGN KEY (category_id) REFERENCES category(id),
                                     FOREIGN KEY (owner_dept_id) REFERENCES department(id)
);

-- ==========================================
-- 4. 操作日志表 (Operation_Log)
-- ==========================================
CREATE TABLE IF NOT EXISTS operation_log (
                                             id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 主键ID：日志记录的唯一标识
                                             action TEXT NOT NULL,                  -- 动作类型：如"CREATE"(新增)、"UPDATE"(修改)、"DELETE"(删除)
                                             entity TEXT NOT NULL,                  -- 操作对象：即操作了哪张表，如"ASSET"(资产)
                                             entity_id INTEGER,                     -- 对象ID：被操作的业务数据的主键ID
                                             details TEXT,                          -- 详情描述：记录具体的变更内容
                                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP -- 发生时间：该操作发生的精确时间
);

-- ==========================================
-- 5. 插入基础测试数据
-- ==========================================
INSERT OR IGNORE INTO category (name) VALUES
                                          ('笔记本电脑'), ('服务器'), ('台式电脑'), ('显示器');

INSERT OR IGNORE INTO department (name) VALUES
                                            ('研发部'), ('IT部'), ('财务部');