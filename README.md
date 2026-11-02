# 语燕输入法 K版 (YuyanImeK)

<p align="center">
  <img src="yuyansdk/src/main/res/drawable/ic_sdk_launcher.xml" width="100" height="100" alt="YuyanImeK Logo">
</p>

**语燕输入法 K版 (YuyanImeK)** 是基于原版 [YuyanIme](https://github.com/gurecn/YuyanIme) 进行深度性能优化、主题引擎重构、音效体验升级与界面精致化定制的开源 Android 拼音/双拼输入法。

---

## ⚡ 原版 (YuyanIme) vs K版 (YuyanImeK) 核心对比

| 维度 / 功能 | 原版语燕输入法 (YuyanIme) | K版语燕输入法 (YuyanImeK) | K版改进优势 |
| :--- | :--- | :--- | :--- |
| **版本号机制** | 基础日期版本号 | 统一 `K` 版递增版本号机制 (当前版本：`K21`) | 版本控制更清晰，更易追溯打包产物 |
| **自定义主题编辑** | 编辑过程中实时修改全局 `activeTheme` 单例，若中途退出/取消会污染实际运行中的键盘主题 | **草稿隔离机制**：编辑期间仅在 Activity 内部草稿修改，取消时彻底回滚，只有点击保存时才应用全局 | 彻底解决中途取消编辑导致全局键盘主题错乱的 Bug |
| **壁纸解码与流畅度** | 主线程同步解码原图（无 `inSampleSize` 降采样，无内存 Cache），多张壁纸列表滑动严重卡顿且容易 `OOM` | **按 View 尺寸 `inSampleSize` 降采样解码 + `LruCache` 内存缓存**；列表预览卡片使用 360x240 缩略图 | 彻底消灭主题列表与键盘唤起的卡顿丢帧，避免内存溢出 (`OOM`) |
| **自定义 Logo 压缩优化** | 截取大图原图全分辨率直接保存并每次在主线程全图解码，大图 Logo 导致打字卡顿 | **截取 Logo 自动压缩降采样至 256x256** 保存于主题文件夹，且候选词栏结合 `LruCache` 零延迟渲染 | 解决高清大图 Logo 导致打字严重卡顿丢帧问题 |
| **按键音效库** | 仅依赖 Android 系统原生按键音效果 | **集成微信与百度全套高品质音效**（微信点击/动作/删除，百度红轴/青轴/茶轴/机械/黑莓/轻敲/经典/可爱/卡通/快板） | 音效体验丰富度提升 1000%，声音清脆解压 |
| **按键音效选择界面** | 无独立选择界面，仅有单一条基础音量设置 | **全新卡片 Tab + 胶囊芯片网格选择器**（标准、空格、删除、回车 4 维选择，支持一键试听） | 界面时尚好看，交互直观简单 |
| **音量/震动调节 Seekbar** | 使用粗重轨道的 `SignSeekBar` 自定义 View，带巨大节点圆圈 | **2dp 极细微轨条 + Material 风格精细滑块**，提供 5 档精准吸附调节 | 视觉体验精细美观，与 Material Design 深度适配 |
| **设置偏好依赖联动** | 关闭“启用按键边框”时，下方“按键阴影/间距/圆角”仍保持亮起状态，逻辑混淆 | **设置界面状态联动**：关闭“启用按键边框”时，下方依赖的三个参数自动置灰禁用 | 逻辑清晰严谨，符合原版预期的交互标准 |
| **主题文件序列化** | 手动拼接带 `#` 注释的非标 JSON 字符串，解析容易因转义失败 | **标准 `kotlinx.serialization` 引擎**，统一规范 JSON 编码与解码 | 数据持久化健壮可靠，避免格式解析失败 |
| **偏好监听器安全** | `WeakHashSet` 存储监听器，匿名 lambda 监听器易被 GC 隐式回收导致回调失效 | **`CopyOnWriteArraySet` 线程安全集合**，并发安全且防止回调失效 | 解决监听器隐式失效与并发修改异常问题 |
| **图标与矢量资源** | 基础图标集 | **全面重绘与优化**（重新绘制了 35+ 菜单图标、16+ 符号图标、Emoji 与 Shift/功能按键图标） | 界面精致度大幅提升 |

---

## 🌟 原版 (YuyanIme) 特性

* **Rime 核心驱动**：基于 Rime 中文输入法引擎，支持全拼、双拼（自然码、小鹤、搜狗、MSPY 等）。
* **轻量无广告**：不请求联网权限，隐私安全，本地离线词库。
* **基本自定义主题**：支持预设主题与基础背景图片设置。
* **基础输入功能**：支持剪贴板历史、常用语、手写输入、Emoji 与符号选择。

---

## 📊 K版与原版文件变动详细对照表 (File Comparison)

> [!IMPORTANT]
> **重要声明**：凡未在以下表格中列出的文件，均与原版语燕输入法（`YuyanIme-master`）完全相同，可以无缝互相替换使用。

### 一、 新增文件列表 (Added Files Table)

| 文件类型 | 文件路径 | 原版特性 | K版特性 |
| :--- | :--- | :--- | :--- |
| **微信按键音效** | `yuyansdk/src/main/assets/sound/wx_click.wav`<br>`yuyansdk/src/main/assets/sound/wx_action.wav`<br>`yuyansdk/src/main/assets/sound/wx_delete.wav` | 不存在该音效文件，按键声音仅有系统基础效果 | 新增提取自微信输入法的高品质原生点击音效、动作音效与删除音效 |
| **百度按键音效** | `yuyansdk/src/main/assets/sound/bd_axis_red.ogg`<br>`yuyansdk/src/main/assets/sound/bd_axis_green.ogg`<br>`yuyansdk/src/main/assets/sound/bd_axis_tea.ogg`<br>`yuyansdk/src/main/assets/sound/bd_axis_mechanical.ogg`<br>`yuyansdk/src/main/assets/sound/bd_device_blackberry.ogg`<br>`yuyansdk/src/main/assets/sound/bd_device_tap.ogg`<br>`yuyansdk/src/main/assets/sound/bd_classic_click.ogg`<br>`yuyansdk/src/main/assets/sound/bd_classic_func.ogg`<br>`yuyansdk/src/main/assets/sound/bd_style_cute.ogg`<br>`yuyansdk/src/main/assets/sound/bd_style_cartoon.ogg`<br>`yuyansdk/src/main/assets/sound/bd_style_allegro.ogg` | 不存在该音效文件 | 新增提取自百度输入法全套分类音效（涵盖 Cherry 机械红/青/茶轴、机械键盘、黑莓全键盘、轻敲、经典点击/动作、萌趣、卡通、快板等） |
| **主题包工具** | `yuyansdk/src/main/java/com/yuyan/imemodule/data/theme/ThemeArchiveUtils.kt` | 不支持压缩包格式皮肤解析 | 新增 `.yus` 压缩包格式离线主题皮肤的完整导入与导出解包工具 |
| **联想模式枚举** | `yuyansdk/src/main/java/com/yuyan/imemodule/prefs/behavior/ChineseAssociationMode.kt` | 无中文输入联想状态枚举 | 新增中文输入联想模式状态枚举控制 |
| **高斯模糊算法** | `yuyansdk/src/main/java/com/yuyan/imemodule/utils/BlurUtils.kt` | 无背景模糊算法处理类 | 基于 StackBlur 算法增加 Bitmap 软件高斯模糊处理逻辑 |
| **箭头/导航矢量图** | `yuyansdk/src/main/res/drawable/ic_arrow_down_pretty.xml`<br>`yuyansdk/src/main/res/drawable/ic_arrow_left_pretty.xml`<br>`yuyansdk/src/main/res/drawable/ic_arrow_right_pretty.xml`<br>`yuyansdk/src/main/res/drawable/ic_arrow_up_pretty.xml`<br>`yuyansdk/src/main/res/drawable/ic_jump_arrow.xml` | 原版使用基础系统图标 | 新增美化版上下左右箭头与设置项跳转箭头矢量图 |
| **交互控制矢量图** | `yuyansdk/src/main/res/drawable/ic_close_pretty.xml`<br>`yuyansdk/src/main/res/drawable/ic_emoji_send.xml` | 基础或缺失矢量图标 | 新增精致美化版关闭/取消图标与 Emoji 面板发送图标 |
| **符号锁/状态矢量图** | `yuyansdk/src/main/res/drawable/icon_symbol_arrow_left.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_arrow_right.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_delete_line.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_lock_line.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_lock_pretty.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_unlock.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_unlock_line.xml`<br>`yuyansdk/src/main/res/drawable/icon_symbol_unlock_pretty.xml` | 原版侧边栏矢量图样式粗糙 | 新增精致侧边栏符号解锁、锁定与删除箭头矢量图标集 |
| **环境配置文件** | `local.properties` | 本地配置文件 | 自动化 Android SDK / NDK 路径配置 |

---

### 二、 修改文件列表 (Modified Files Table)

| 文件类型 | 文件路径 | 原版特性 | K版特性 |
| :--- | :--- | :--- | :--- |
| **构建脚本** | `app/build.gradle` | 基础日期版本号算法（如 K9） | 修改为统一递增版本号机制（`getKVersionCode()` 当前升级为 `K11`）并优化产物 APK 命名 |
| **主题背景解码** | `yuyansdk/src/main/java/com/yuyan/imemodule/data/theme/Theme.kt` | `toDrawable()` 主线程同步解码原图，无采样率降采样与 Cache，多大图卡顿易 OOM | 增加按 View 尺寸 `inSampleSize` 降采样计算，引入 `LruCache` 内存缓存，彻底解决丢帧与 OOM |
| **主题草稿隔离** | `yuyansdk/src/main/java/com/yuyan/imemodule/ui/fragment/theme/CustomThemeActivity.kt` | 编辑期间实时修改全局 `activeTheme` 单例，取消退出会污染正在使用的键盘主题 | **草稿隔离机制**：编辑期间仅在本地草稿修改刷新预览，取消时彻底回滚，仅保存时应用到全局 |
| **主题列表卡片渲染** | `yuyansdk/src/main/java/com/yuyan/imemodule/ui/fragment/theme/ThemeThumbnailUi.kt` | RecyclerView 解码原图，滑动丢帧严重 | 卡片改用 360x240 指定尺寸降采样加载，列表滑动极度流畅 |
| **主题管理器** | `yuyansdk/src/main/java/com/yuyan/imemodule/data/theme/ThemeManager.kt` | `WeakHashSet` 存储监听器，匿名 lambda 容易被 GC 隐式回收导致回调失效 | 升级为 `CopyOnWriteArraySet` 线程安全集合，防范回调失效与并发修改异常；安全初始化 `activeTheme` |
| **主题序列化** | `yuyansdk/src/main/java/com/yuyan/imemodule/data/theme/CustomThemeSerializer.kt` | 手动拼接带 `#` 注释的非标 JSON，极易因转义或字符导致解析失败 | 标准 `kotlinx.serialization` 解析与编码，保证主题文件持久化绝对可靠 |
| **按键音效与反馈驱动** | `yuyansdk/src/main/java/com/yuyan/imemodule/prefs/InputFeedbacks.kt` | 仅依赖 Android 系统的 `playSoundEffect` 基础按键音 | **集成 `SoundPool` 音频池**，实时零延迟加载播放微信/百度多套分类高品质 `.wav`/`.ogg` 音频 |
| **按键音偏好设置** | `yuyansdk/src/main/java/com/yuyan/imemodule/prefs/AppPrefs.kt` | 仅有单一基础音量控制配置 | 新增 `soundEffectStandard`、`soundEffectSpaceBar`、`soundEffectDelete`、`soundEffectReturn` 4 种音效偏好项 |
| **偏好基类事件广播** | `yuyansdk/src/main/java/com/yuyan/imemodule/view/preference/ManagedPreference.kt` | `setValue()` 仅写 SharedPreferences，不触发 `fireChange()` | **`setValue()` 增加 `fireChange()` 广播**，驱动设置界面依赖关联组件实时联动更新 |
| **偏好可见度联动评估** | `yuyansdk/src/main/java/com/yuyan/imemodule/prefs/ManagedPreferenceVisibilityEvaluator.kt` | 初始评估不发送 changed 事件，开关切换无界面刷新 | **设置界面联动**：关闭“启用按键边框”时，下方依赖的阴影、间距、圆角自动置灰禁用 |
| **按键效果设置 Fragment** | `yuyansdk/src/main/java/com/yuyan/imemodule/ui/fragment/KeyboardFeedbacksFragment.kt` | 仅有简单的音量/震动控制 | **全新 4 维胶囊 Tab + 网格卡片音效选择器**，支持试听与切换；音量/震动升级为 2dp 极细 5 档吸附 SeekBar |
| **自定义拖动条控件** | `yuyansdk/src/main/java/com/yuyan/imemodule/view/widget/seekbar/SignSeekBar.java` | 忽略 `isShowSectionMark`，粗重轨道上强制绘制巨大节点圆圈 | 修复节点绘制逻辑，支持 `2dp` 极细微轨条与 Material 风格精细滑块 |
| **字符串资源** | `yuyansdk/src/main/res/values/strings.xml`<br>`yuyansdk/src/main/res/values/arrays.xml` | 基础语言资源 | 新增按键音分类名称（默认、微信点击、微信空格、微信删除、微信回车、百度各轴体）及 5 档音量描述 |
| **菜单矢量图标集** | `yuyansdk/src/main/res/drawable/ic_menu_*.xml` (共35项) | 原版基础菜单图标 | 重绘并替换为最新精美矢量图，统一图标尺寸与线条风格 |
| **符号矢量图标集** | `yuyansdk/src/main/res/drawable/icon_symbol_*.xml` (共16项) | 基础符号图标 | 重绘更新侧边栏与符号面板图标 |
| **Emoji矢量图标集** | `yuyansdk/src/main/res/drawable/icon_emojibar_*.xml` (共11项) | 基础 Emoji 分类图标 | 重绘 Emoji 分类栏矢量图标 |
| **颜文字矢量图标集** | `yuyansdk/src/main/res/drawable/icon_emoticon_*.xml` (共5项) | 基础颜文字分类图标 | 重绘颜文字分类栏矢量图标 |
| **按键功能图标集** | `yuyansdk/src/main/res/drawable/sdk_skb_key_*.xml` (共6项) | 基础功能图标 | 重绘更新删除、回车、空格、中英文模式按键图标 |
| **Shift 状态图标集** | `yuyansdk/src/main/res/drawable/shift_*.xml` (共7项) | 基础 Shift 图标 | 重绘 Shift 小写、首字母大写、大写锁定 3 种状态矢量图 |
| **预设主题背景** | `yuyansdk/src/main/res/drawable/skb_background_*.xml` (共6项) | 基础背景梯度 | 重绘更新红、黄、粉、紫、蓝、深红预设主题背景 Drawable 渐变 |
| **微信符号与Emoji顺序** | `yuyansdk/src/main/java/com/yuyan/imemodule/data/emojicon/EmojiconData.kt` | 原版基础符号与Emoji排列 | **完全对齐微信输入法官方分类与排列顺序**（中文/英文标点、数学、括号、序号/标号、特殊图形、表情/人物/自然/食物/活动/符号/旗帜），零体积增加 |
| **微信全屏特效触发** | `yuyansdk/src/main/java/com/yuyan/imemodule/keyboard/container/SymbolContainer.kt` | 点击微信表情仅提交提示文本，且Emoji底部Tab数量不匹配 | **智能映射与底栏对齐**：点击“💥炸弹”、“🎇烟花”、“🎉庆祝”提交官方指令 `[炸弹]`、`[烟花]`、`[庆祝]`，100% 触发微信全屏动画；底部 Emoji 分类 Tab（最近、表情、自然、食物、活动、符号、旗帜、微信、热门）与 `ViewPager2` 1-to-1 精确对齐 |
| **曦码风格设置界面** | `yuyansdk/src/main/java/com/yuyan/imemodule/utils/XimePreferenceStyler.kt`<br>`yuyansdk/src/main/res/layout/sdk_item_preference_xime.xml` | 原版基础列表布局 | **曦码输入法现代卡片架构**：重绘圆角白色分组卡片、淡紫色高颜值图标盒、粗体主标题与清晰描述副标题，100% 重现曦码输入法极简美学 |
| **工程通用工具类** | `yuyansdk/src/main/java/com/yuyan/imemodule/utils/*.kt`<br>`yuyansdk/src/main/java/com/yuyan/imemodule/service/*.kt`<br>`yuyansdk/src/main/java/com/yuyan/imemodule/keyboard/*.kt` | 基础工具与键盘视图类 | 配合主题缓存、离线词库、多窗口尺寸计算及系统服务进行兼容优化 |

---

## 🛠️ 构建与编译 (Build)

环境要求：
* Android Studio Jellyfish / 2026.2.1 或更高版本
* JDK 17
* Android SDK Platform 36

编译调试 APK：
```bash
./gradlew :app:assembleOfflineDebug
```

生成产物：`app/build/outputs/apk/offline/debug/语燕输入法K-K21_*.apk`

---

## 📄 开源协议 (License)

本项目继承原版语燕输入法的开源协议，基于 [GPL-3.0 License](LICENSE) 协议开源。
