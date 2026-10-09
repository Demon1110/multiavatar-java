# Multiavatar (Pure Java)

Multiavatar —— 多文化头像生成器（Multicultural Avatar Maker）的**纯 Java 实现**。

移植自 [`multiavatar.js`](https://github.com/multiavatar/Multiavatar)（Gie Katon，2020-2021）。
**零第三方依赖，JDK 8+ 即可编译运行**，标准 Maven 项目，开箱即用。

共可生成 **16^6 = 12,230,590,464** 个唯一头像，任何输入字符串都能确定性地映射为一个唯一头像（可作 identicon 使用）。

---

## 能力一览

| 能力                | 说明                                                                                                                                    |
|-------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| **SVG 输出**        | 完整头像 / 去背景（sansEnv）/ 强制角色+主题（ver）三种模式，与官方 JS **逐字符一致**                                                                                |
| **PNG 输出**        | 基于 JDK 自带 Java2D 光栅化，无需任何第三方 jar；任意尺寸（默认 256×256），透明背景                                                                                |
| **JPEG 输出**       | 白底不透明 JPEG（博客/OA 场景），`AvatarFormat.JPG`                                                                                               |
| **GIF 动画**        | 部件逐帧淡入（env→head→clo→top→eyes→mouth 依次长出），无限循环，`AvatarFormat.GIF`                                                                      |
| **BufferedImage** | `Avatar.toImage()` / `Multiavatar.toImage()` 直接返回图像供继续加工（缩放/合成/加水印）                                                                   |
| **favicon 打包**    | 一次生成 16/32/48/64/128/256 多尺寸 PNG 并打成 zip（零依赖，`java.util.zip`）                                                                         |
| **PNG data URI**  | `toDataUri()` / `Avatar.dataUri()` 返回 `data:image/png;base64,...`，`<img src>` 直接内嵌，免上传                                                |
| **LRU 缓存**        | 全局 LRU（默认容量 128）：同参数输入重复生成复用已组装 `Avatar`，满则淘汰最久未使用（`LinkedHashMap` 实现，零依赖）                                                            |
| **批量预览图**         | 演示入口自动拼接 N 输入横排预览图 `demo/preview_grid.png`（兼作批量渲染视觉回归图）                                                                               |
| **SVG 体积优化**      | 默认开启：path 坐标按精度取整（描边 2 位 / 填充 1 位）、transform 与 polygon 坐标取整、删除 style 尾分号，体积再减 16%~31%（`AvatarOptions.optimizeSvg(false)` 可关闭，还原逐字符原文） |
| **零依赖**           | `pom.xml` 的 `<dependencies>` 为空，只用 JDK 标准库（java.awt / javax.imageio / MessageDigest / DOM）                                            |

---

## 算法原理

1. 输入字符串做 **SHA-256**（JDK 自带 `MessageDigest`，与 JS 内置 CryptoJS 输出一致）。
2. 取十六进制结果前 **12 位数字**，每 2 位经 `round(47/100 * 两位)` 映射为 0–47 的部件编号。
3. 编号换算成 16 个初始角色（`00`–`15`）与 3 个颜色主题（`A`/`B`/`C`）。
4. 角色部件 SVG 模板中的 `#xxx;` 颜色占位符被该主题的颜色**按序替换**（等价复刻 JS 的 `replaceFirst` 语义）。
5. 按 `env → head → clo → top → eyes → mouth` 顺序拼装成完整 SVG（viewBox `0 0 231 231`，与官方 JS 一致，
   环境圆几何边界恰为 0~231，头像正好充满画布；根元素默认
   `width="256" height="256"`，可用 `AvatarOptions.svgSize` 调整）。

角色与颜色数据（`MultiavatarData.java`，约 86KB）由脚本从 `multiavatar.js` 自动提取生成，可追溯、可复现。

---

## 使用方式

### 作为库调用（SVG）

```java
import com.cary.multiavatar.Multiavatar;
import com.cary.multiavatar.AvatarOptions;

String svg1 = Multiavatar.multiavatar("Binx Bond");              // 完整头像（含背景圆）
String svg2 = Multiavatar.multiavatar("test", true);             // 去掉背景圆（sansEnv）
String svg3 = Multiavatar.multiavatar("test", false, "00", "A"); // 强制角色 00 + 主题 A（对应 JS 的 ver）
```

三个重载分别对应 JS 的 `multiavatar(string)`、`multiavatar(string, sansEnv)`、
`multiavatar(string, sansEnv, ver)`。输入为空字符串时返回空串（与 JS 一致）。
SVG 根元素默认带 `width="256" height="256"`（可用 `.svgSize(w, h)` 自定义，见下）。

### 作为库调用（图片格式）

```java
// PNG：默认 256×256 透明底
byte[] png = Multiavatar.toPng("Binx Bond");
byte[] png2 = Multiavatar.toPng("Binx Bond", 512);

// JPEG：白底不透明（博客/OA 场景）
byte[] jpg = Multiavatar.toJpg("Binx Bond");
byte[] jpg2 = Multiavatar.toJpg("Binx Bond", 512);

// GIF：部件逐帧淡入动画，无限循环
byte[] gif = Multiavatar.toGif("Binx Bond");

// BufferedImage：直接返回图像，供继续加工（缩放/合成/加水印）
BufferedImage img = Multiavatar.toImage("Binx Bond", 256);

// favicon 打包：多尺寸 PNG 的 zip 字节（默认 16/32/48/64/128/256）
byte[] zip = Multiavatar.toFaviconZip("Binx Bond");
byte[] zip2 = Multiavatar.toFaviconZip("Binx Bond", 32, 64);

// PNG data URI：<img src="..."> / CSS background 直接内嵌，免上传
String uri = Multiavatar.toDataUri("Binx Bond");       // data:image/png;base64,...
String uri2 = Multiavatar.avatar("Binx Bond").dataUri(128);

// 全局 LRU 缓存：同参数输入重复生成直接复用（容量 128，满则淘汰最久未使用）
Avatar a1 = Multiavatar.avatar("Binx Bond");
Avatar a2 = Multiavatar.avatar("Binx Bond");           // a1 == a2（同一实例）
int cached = Multiavatar.cacheSize();                  // 当前缓存条目数
Multiavatar.

clearCache();                              // 清空缓存

// 高级选项：去背景 + 强制角色/主题 + 自定义尺寸 + 自定义 SVG 宽高 + 指定格式
AvatarOptions opts = AvatarOptions.builder()
        .sansEnv(true)
        .part("07").theme("B")
        .size(128)                 // PNG/JPEG/GIF 输出边长（像素）
        .svgSize(256, 256)         // SVG 根元素宽高（像素，默认 256×256）
        .format(AvatarFormat.PNG)
        .build();
byte[] out = Multiavatar.render("Binx Bond", AvatarFormat.GIF, opts);

// 直接写入文件
Multiavatar.

writePng("Binx Bond",opts, new File("avatar.png"));
        Multiavatar.

writeFaviconZip("Binx Bond",new File("favicon.zip"));
```

### 面向对象的高级用法

```java
// 不可变产物：SVG 文本 + 按需图像
Avatar avatar = Multiavatar.avatar("Binx Bond");
String svg = avatar.svg();
BufferedImage img = avatar.toImage(256);  // 直接拿图
byte[] png = avatar.png();                // PNG 字节（默认尺寸）
byte[] pngLarge = avatar.png(512);
byte[] jpg = avatar.jpg();                // JPEG 字节（白底）

// 直接获取渲染器（策略模式），可自行扩展新格式
AvatarRenderer renderer = Renderers.create(AvatarFormat.GIF);
byte[] out = renderer.render(avatar.svg(), 256);

// SVG 体积优化默认开启（约 -20%~-30%）；需要逐字符原文时显式关闭
Avatar avatarRaw = Multiavatar.avatar("Binx Bond", AvatarOptions.builder().optimizeSvg(false).build());
```

### 命令行演示

```bash
# 生成 5 组示例头像（SVG + PNG）到 ./demo/ 目录
java -cp target/classes com.cary.multiavatar.Multiavatar

# 把参数作为输入，打印 SVG 到标准输出
java -cp target/classes com.cary.multiavatar.Multiavatar "Binx Bond"
```

---

## 架构设计

分层 + 设计模式，职责单一、可扩展、可测试。

```
src/main/java/com/cary/multiavatar/
├── Multiavatar.java          # 门面（Facade）：兼容旧静态 API + 新 API + 演示 main
├── Avatar.java               # 不可变产物对象：SVG 缓存 + toImage/png/jpg
├── AvatarOptions.java        # 构建者（Builder）：sansEnv / part / theme / size / svgSize / format
├── core/                     # 核心装配流水线
│   ├── AvatarIdHasher        #   策略接口：字符串 → 哈希
│   ├── Sha256AvatarIdHasher  #   策略实现：SHA-256 十六进制
│   ├── PartNumberMapper      #   哈希 → 0-47 部件编号
│   ├── PartKeyResolver       #   编号 → 角色 / 主题 / 部件键
│   ├── AvatarSpec            #   不可变规格（含强制 part/theme，对应 JS ver）
│   ├── SvgFragmentPainter    #   占位符颜色替换（replaceFirst 语义）
│   └── SvgComposer           #   模板方法（Template Method）：固定拼装顺序，部件可注入
├── svg/                      # 领域层：SVG 对象模型 + 解析 + 光栅化前处理
│   ├── SvgDocument / SvgShape（抽象，paint 模板方法：先填充后描边，帧级 alpha 乘数）
│   ├── SvgPath / SvgPolygon / SvgLine / SvgRect / SvgColor / SvgStyle
│   ├── PathDataParser        #   path d 全指令解析（含 arc→三次贝塞尔，W3C F.6）
│   ├── TransformParser       #   transform="matrix(...)" 解析
│   └── SvgParser             #   JDK DOM 解析（XXE 防护）
├── render/                   # 渲染策略层（Strategy）
│   ├── AvatarFormat          #   枚举：SVG / PNG / JPG / GIF
│   ├── AvatarRenderer        #   渲染器接口
│   ├── SvgAvatarRenderer     #   SVG 渲染器（字节输出）
│   ├── PngAvatarRenderer     #   PNG 渲染器（Java2D 光栅化 + ImageIO 编码）
│   ├── JpgAvatarRenderer     #   JPEG 渲染器（白底）
│   ├── GifAvatarRenderer     #   GIF 渲染器（部件逐帧淡入动画）
│   ├── SvgRasterizer         #   viewBox 等比缩放 + 居中 + 抗锯齿（支持背景色/部分形状）
│   ├── PngWriter / JpgWriter / GifWriter  #   各格式编码器
│   ├── Favicons              #   多尺寸 PNG → zip 打包
│   └── Renderers             #   简单工厂（Simple Factory）
├── data/
│   └── DataTables            # 单例（Singleton）：MultiavatarData 只读访问
└── util/
    ├── Hashes                # SHA-256 十六进制
    └── Strings               # 工具方法
```

### 用到的设计模式

| 模式                      | 位置                                           | 说明                                           |
|-------------------------|----------------------------------------------|----------------------------------------------|
| **Facade**              | `Multiavatar`                                | 统一入口，屏蔽底层分层细节                                |
| **Strategy**            | `AvatarIdHasher` / `AvatarRenderer`          | 哈希算法、输出格式可替换扩展                               |
| **Template Method**     | `SvgComposer`（装配顺序）、`SvgShape.paint`（先填充后描边） | 固定骨架，细节由子类/注入决定                              |
| **Builder**             | `AvatarOptions`                              | 可选参数（sansEnv/part/theme/size/svgSize/format） |
| **Simple Factory**      | `Renderers`                                  | 按格式创建渲染器                                     |
| **Singleton**           | `DataTables`                                 | 数据表唯一实例                                      |
| **Immutable Object**    | `AvatarSpec` / `Avatar`                      | 不可变规格与产物                                     |
| **Lazy Initialization** | `Avatar.png()`                               | 首次调用才渲染并缓存                                   |

---

## 构建

```bash
mvn clean package
```

- 产物：`target/multiavatar-java.jar`
- **无任何第三方依赖**，`pom.xml` 的 `<dependencies>` 为空
- 编译目标：Java 1.8（`maven-compiler-plugin` 设 `source/target=1.8`）

---

## 验证

### 1. SVG 与官方 JS 逐字符一致（回归）

`src/test/java/com/cary/multiavatar/CompareWithJs.java` 生成 Java 侧输出，与 Node 运行官方
`multiavatar.js` 的基准逐字符对比（viewBox 均 `0 0 231 231`，无坐标换算差异）：

```
node gen_benchmark2.cjs                          # 生成 target/js_benchmark.json（JS 官方基准）
mvn clean compile test-compile
java -cp target/classes;target/test-classes com.cary.multiavatar.CompareWithJs target/compare_after_refactor.json
python regression_svg2.py                       # 逐字符比对（viewBox 一致，脚本归一化仅为兼容）
```

> 当前结果：**19/19 项全部一致**（basic 12 项、sansEnv 4 项、ver 2 项）。

### 2. PNG 渲染与浏览器像素级对照

`src/test/java/com/cary/multiavatar/PngCheckTool.java` 批量生成 SVG + PNG（含强制角色/主题用例，
覆盖数据中全部 4 个带 `opacity` 半透明颜色的 eyes 部件），用无头 Chrome 渲染同 SVG 后逐像素对比：

- 形状差异（二值化后不匹配像素占比）：34 个用例**全部 < 0.1%**（差异为两个渲染引擎的边缘抗锯齿差异，非几何错误）
- 核心区域平均 RGB 差：**< 2.1 / 255**（几何与颜色完全一致）

`PartCheckTool.java` 可对 6 个部件逐一渲染对照，用于定位具体部件问题。

### 3. 输出层冒烟验证（toImage / JPG / GIF / favicon / data URI）

`OutputSmokeCheck.java` 验证新增输出能力：`toImage` 尺寸与空输入、JPG 白底可解码、
GIF 帧数 = 形状数×2（18 帧）且无限循环（magick 识别 Iterations: 0）、favicon zip 条目数、
data URI 前缀与 Base64 可解码回 PNG 且与 `Avatar.dataUri()` 一致。

### 3b. LRU 缓存行为验证（CacheCheck）

`CacheCheck.java` 验证：同参数命中（同一实例）、`size` 不入键而 `part/theme/sansEnv/svgSize` 入键、
容量 2 时最久未使用项被淘汰、`clearCache()` 后重新组装。

### 4. JPG 输出专项验证（JpgFormatCheck）

`src/test/java/com/cary/multiavatar/JpgFormatCheck.java`（14 项断言，零依赖）：

- 基本渲染：字节非空、ImageIO 可解码、默认 256×256、无 alpha 通道、四角白底、文件头魔数 `FFD8FF`
- 多尺寸（16/128/512）与多输入（含中文）渲染正确；空输入返回空数组
- 入口等价：`Avatar.jpg()`、`render(..., AvatarFormat.JPG, ...)` 与 `toJpg(...)` 字节一致
- 与 PNG 形状一致性（JPEG 有损，双指标）：**recall ≥ 0.9999**（JPG 几何完整覆盖 PNG，无缺失/无画错）、
  **extra 6.1%~7.7% ≤ 10%**（差异仅为压缩振铃导致的边缘扩散）

> 注：上述验证工具位于 `src/test/java`，仅用于验证，不是库代码。

### 5. SVG 体积优化专项验证（SvgOptimizeCheck）

`src/test/java/com/cary/multiavatar/SvgOptimizeCheck.java`（零依赖）：

- **体积减量**：10 个样例（含中文/邮箱/纯数字/特殊字符）实测减量 **16.6%~31.1%**（`github` 5900B→4075B）
- **等价性双指标**（Java 光栅化 256 对比优化前后）：**recall ≥ 0.9977**、**extra ≤ 0.16%**（阈值 0.5%）——
  全部通过；`optimizeSvg(false)` 逐字符还原原文；优化开关已入 LRU 缓存键
- **数据事实**：`MultiavatarData` 全部 221 个形状中 48 个 `fill:none` **全部带描边**（可见线条），
  不存在可删除的隐形形状，故删除项不做、描边形状保留
- **关键修复（负零粘连）**：紧凑负号分隔（如 `2.707-0.0428`）中负小数取整归零时若输出 `0`，
  会与前一数字粘连成 `2.70`（坐标参数丢失 → 路径扭曲延伸）。修复为负数归零输出 `-0`
  （`-0` 是合法 SVG 数字且负号永远保持 token 分隔），回归断言 `-0.0428 -> "-0"`。
  该缺陷正是 `github` 样例优化后形状扩散 4.4%（Chrome/Java 双端复现）的根因，修复后降至 0.03%
- **Chrome 对照**（34 用例，优化版 Java 光栅化 vs Chrome 渲染）：修复前最差 idx=16=2.83%，
  修复后**全部 ≤ 0.078%**（阈值 3%）

---

## 许可

本项目移植自 Multiavatar，原始作品版权归 Gie Katon（2020-2021），遵循其开源许可条款。
