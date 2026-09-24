# Multiavatar (Pure Java)

Multiavatar —— 多文化头像生成器（Multicultural Avatar Maker）的**纯 Java 实现**。

移植自 [`multiavatar.js`](https://github.com/multiavatar/Multiavatar)（Gie Katon，2020-2021）。
**零第三方依赖，JDK 8+ 即可编译运行**，是一个标准 Maven 项目。

共可生成 **16^6 = 12,230,590,464** 个唯一头像，任何输入字符串都能确定性地映射为一个唯一头像（可作 identicon 使用）。

---

## 算法原理

1. 对输入字符串做 **SHA-256**（JDK 自带 `MessageDigest`，与 JS 内置 CryptoJS 输出一致）。
2. 取十六进制结果中前 **12 位数字**，每 2 位数字经 `round(47/100 * 两位)` 映射为 0-47 的部件编号。
3. 编号换算成 16 个初始角色（`00`–`15`）与 3 个颜色主题（`A`/`B`/`C`）。
4. 每个角色部件的 SVG 模板中的 `#xxx;` 颜色占位符，被该主题的颜色**按序替换**。
5. 按 `env → head → clo → top → eyes → mouth` 顺序拼装成完整 SVG。

角色与颜色数据（`MultiavatarData.java`）由脚本从 `multiavatar.js` 自动提取生成，可追溯、可复现。

---

## 使用方式

### 作为库调用

```java
import com.cary.multiavatar.Multiavatar;

String svg = Multiavatar.multiavatar("Binx Bond");                    // 完整头像（含背景圆）
String svg2 = Multiavatar.multiavatar("test", true);                  // 去掉背景圆（sansEnv）
String svg3 = Multiavatar.multiavatar("test", false, "00", "A");      // 强制角色 00 + 主题 A（对应 JS 的 ver）
```

三个重载分别对应 JS 的 `multiavatar(string)`、`multiavatar(string, sansEnv)`、
`multiavatar(string, sansEnv, ver)`。输入为空字符串时返回空串（与 JS 一致）。

### 命令行演示

```bash
# 生成若干示例头像到 ./demo/ 目录
java -cp target/classes com.cary.multiavatar.Multiavatar

# 把参数作为输入，打印 SVG 到标准输出
java -cp target/classes com.cary.multiavatar.Multiavatar "Binx Bond"
```

---

## 构建

```bash
mvn clean package
```

- 产物：`target/multiavatar-java.jar`
- **无任何第三方依赖**，`pom.xml` 的 `<dependencies>` 为空
- 编译目标：Java 1.8（`maven-compiler-plugin` 设 `source/target=1.8`）

---

## 与 JS 实现的一致性验证

`verify/` 目录提供可复现的对照验证流程，确认 Java 输出与官方 `multiavatar.js` 逐字符一致：

1. 用 Node 运行官方 `multiavatar.js` 生成基准：`node verify/gen_benchmark.cjs`
   （生成 `verify/benchmark.json`，覆盖 basic / sansEnv / ver 三种模式，含空串、中文、长串等用例）
2. 编译 Java 后生成 Java 侧输出：`java -cp target/classes;target/test-classes com.cary.multiavatar.CompareWithJs target/compare_java.json`
3. 逐项比对：`python verify/compare_output.py`

> 当前验证结果：**19/19 项全部一致**（basic 12 项、sansEnv 4 项、ver 2 项）。

`MultiavatarData.java` 由 `verify/gen_data.py` 从 `multiavatar.js` 自动提取生成。若上游 JS 数据更新，
重跑 `python verify/gen_data.py` 即可重新生成数据文件（脚本会同时校验「每个部件的颜色数」与
「SVG 占位符数」的对应关系）。

注：`CompareWithJs.java` 位于 `src/test/java`，仅用于验证，不是库代码。

---

## 目录结构

```
java-multiavatar/
├── pom.xml                       # Maven 配置（JDK 1.8，零第三方依赖）
├── README.md
├── src/
│   ├── main/java/multiavatar/
│   │   ├── Multiavatar.java      # 核心算法与演示入口（SHA-256、编号映射、颜色替换、拼装）
│   │   └── MultiavatarData.java  # 自动生成的数据：颜色主题 + SVG 部件模板
│   └── test/java/multiavatar/
│       └── CompareWithJs.java    # 与 JS 基准对照的工具
└── verify/
    ├── benchmark.json            # JS 官方基准输出（对照用）
    ├── gen_benchmark.cjs         # 生成基准的 Node 脚本
    ├── gen_data.py               # 从 multiavatar.js 重新生成 MultiavatarData.java 的脚本
    └── compare_output.py         # 比对脚本
```

---

## 许可

本项目移植自 Multiavatar，原始作品版权归 Gie Katon（2020-2021），遵循根目录 [`LICENSE`](../LICENSE) 的许可条款。
