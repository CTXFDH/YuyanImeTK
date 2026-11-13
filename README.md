# 语燕输入法 K版 (YuyanImeK) 说明文档

**语燕输入法 K版 (YuyanImeK)** 是基于官方开源项目 **语燕输入法（YuyanIme-master，包含最新提交 `f97026c`）** 源代码进行深度重构、功能扩展与 UI 交互优化的 Android 输入法项目。在保持原版极简、无广告、高隐私特性的同时，K版独有地引入了离线神经网络语音识别、九键/乱序17/全键全模式中英混输预测、按键直接捕获的个性化短语定制、双列并列主题显示、功能键英文显示、全局圆角卡片 UI 与统一弹窗交互体系。

---

# 🚀 第一部分：K版相比原版 (YuyanIme-master) 的独有新功能与重构优化

### 1. 🎙️ 离线神经网络语音识别引擎 (K版全新开发)
- **零网络依赖**：内置 ONNXRuntime 与 Sherpa-onnx Zipformer 离线语音识别框架，完全在手机本地进行语音流式解算，不上传任何数据（原版官方源码无任何实际语音识别引擎）。
- **动态波形键盘 UI (`VoiceKeyboardContainer`)**：支持实时的音频波形动画可视化、按住说话、松开上屏、长按滑动快速控制光标、连续删除与上滑清空手势。
- **离线模型导入与常驻**：支持一键解压导入 Sherpa-onnx 模型包（中英文混合识别 + 自动标点），提供模型常驻内存选项以消除首次加载延迟。

### 2. 🔤 9键/乱序17/全键全模式【中英混输】预测 (K版扩展)
- **应用层 Key-Pattern 极速预测引擎 (`T9EnglishPredictor`)**：
  - 在 **中文 9 键 (T9)** 下输入 `4663` 即可直接在候选词中提示 `good`。
  - 在 **乱序 17 (LX17)** 下输入 `G`-`oXv`-`oXv`-`D` (`GXXD`) 即可自动匹配提示 `good`。
  - 完整映射了 26 个英文字母在 LX17 全布局上的主/备用按键变体，查表耗时 $<1\text{ms}$，零卡顿，且彻底修正了英文词插入导致的 Rime 候选词索引偏移与错位选词 Bug。
- **独立设置开关**：在【输入设置】中增加【中英混输】（`cn_en_input_enable`）独立开关，可随时切换 Rime `echo` 混输状态。

### 3. ✍️ 按键直接捕获型【个性化短语】 (K版独有)
- **设置管理入口**：在主设置【方案与数据】分类中新增【个性化短语】独立管理卡片。
- **软键盘按键直捕 (`KeyCaptureEditText`)**：
  - 上方【输入码】框获得焦点时，**自动拦截并隔绝对话框内的拼音打字与拼写串**，直接捕获软键盘点击的原始按键键值（9 键直接填入 `34944`、乱序 17 直接填入 `GXXD`、全键填入 `dizhi`）。
- **模式绑定与精准出词**：短语与添加时的键盘模式绑定（列表中清晰标注 `[中文9键]`、`[乱序17]` 或 `[拼音全键]`），打字时通过按键序列精准置顶弹出自定义长短语。

### 4. 🎨 界面卡片化、双列主题与全局圆角弹窗 (K版重构)
- **`CardPreferenceStyler` 设置 UI**：主设置全面重构为精致的白色圆角矩形卡片（18dp 卡片间距、统一矢量图标与水波纹触控）。
- **`TwinThemeSelectPreference` 双列主题展示**：在主题设置中支持亮色/暗色主题左右双列并排对照预览与一键切换。
- **`DialogStyleUtils` 全局弹窗重构**：
  - 所有对话框应用 20dp 柔和圆角卡片，弹窗底色与文字**彻底与键盘选中的主题解耦**，严格跟随设置页面 UI 的日/夜间模式自适应变色。
  - 全局隐藏无用的【取消】按键，极简干净；长按主题弹出卡片旁小巧的 12dp 圆角矩形下拉菜单（仅含 `导出主题 (.yus)`）。
  - 主题编辑器顶部的键盘切换胶囊按钮，点击可向下平滑展开 16dp 圆角矩形选择框。

### 5. 🔤 功能键英文显示与按键音效扩展 (K版扩展)
- **功能键英文模式**：支持按键功能键英文显示模式，开启后回车键/功能键动态切换为 `Send`、`Go`、`Search`、`Next`、`Done`、`Prev`、`Enter` 等英文标签。
- **丰富机械轴与微信按键音效**：内置青轴、红轴、茶轴、黑莓、微信音效等 12+ 种点击与删除音效（`assets/sound/`）。
- **精致预设主题 (.yus)**：内置复刻微信、微信暗、赛博朋克霓虹、莫兰迪极简粉、海盐芝士、深海幽蓝、日落晚霞等 15+ 种预设皮肤包，支持 `.yus` 压缩包一键导入导出。

---

# 📂 第二部分：原版源代码 (YuyanIme-master) 与 K版 (YuyanImeK-master) 完整文件差异对比表

> [!IMPORTANT]
> 说明：下表精确对比了原版官方最新源代码目录（`C:\Users\kuang\Downloads\YuyanIme-master`）与当前 K版源代码（`YuyanImeK-master`）。若某文件/模块为 K版独有新增，原版列标注为 **【无此文件】**；若原版包含但 K版未包含，K版列标注为 **【无此文件】**。未在下表中列出的代码文件在两版之间完全一致，可直接互相替换。

---

### 1. 工程架构与构建配置文件对比 (Build & Project Config)

| 文件名 / 相对路径 | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `build.gradle` (Root) | 使用 Gradle Version Catalogs (`alias(libs.plugins...)`) 统一定义插件版本 | 采用传统 Groovy 插件直接依赖，内置 `buildscript` 声明，保障 IDE 各版本的构建兼容性 |
| `gradle/libs.versions.toml` | **包含 Version Catalogs 依赖配置文件** | **【无此文件】** (直接在 Gradle 中显式声明依赖版本) |
| `app/build.gradle` | 采用 Version Catalogs；`versionName = versionNameDate()` 动态生成纯日期版本号（如 `20261107.12`）；配置 `offline` 变体与 release 签名脚本；输出 `yuyanIme_${versionCodeDate()}_${variant.buildType}.apk` | 采用 K版统一递增版本号（`versionName "K62"`、`getKVersionCode() = 62`）；解耦硬件包强签名依赖；输出自定义命名 `YuyanImeK-K62_${versionCodeDate()}_release.apk` 并优化 AAPT 资源压缩 |
| `yuyansdk/build.gradle` | 使用 Version Catalogs (`libs.androidx.appcompat`) 引用依赖；使用 KSP 编译器 | 显式声明 Jetpack、Room、Navigation、Compose、ARouter 及 ONNX 依赖；开启 `buildFeatures.aidl = true` 与 `compose = true`；使用 kapt 编译器保证稳定性 |
| `app/src/main/AndroidManifest.xml` | 声明基础 IME 权限与应用组件 | 增加了 `RECORD_AUDIO` 录音权限，注册了 `AsrInferenceService` 离线语音识别跨进程 AIDL 后台服务 |

---

### 2. K版独有新增模块与代码文件对比 (K-Version Unique Files - 原版【无此文件】)

| 文件名 / 相对路径 | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../service/T9EnglishPredictor.kt` | **【无此文件】** | **[新增]** 九键 (T9) 与乱序 17 (LX17) 中英混输预测引擎，支持 26 字母多按键变体映射 |
| `yuyansdk/.../service/AsrInferenceClient.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别 AIDL 服务客户端通信类 |
| `yuyansdk/.../service/AsrInferenceService.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别后台推理服务 |
| `yuyansdk/.../service/VoiceRecognitionHandler.kt` | **【无此文件】** | **[新增]** 语音识别长按录入与文本回调句柄 |
| `yuyansdk/.../speech/` (全组 11 个代码文件) | **【无此文件】** | **[新增]** Sherpa-onnx 离线语音推理引擎、模型管理、频谱分析器与生命周期管理 |
| `yuyansdk/.../aidl/` (全组 2 个 AIDL 接口文件) | **【无此文件】** | **[新增]** ASR 语音推理服务与文本回调跨进程通信接口 |
| `yuyansdk/.../ui/fragment/PhrasesSettingsFragment.kt` | **【无此文件】** | **[新增]** 个性化短语管理页面，支持自动检测当前键盘模式（9键/LX17/全键）与卡片列表管理 |
| `yuyansdk/.../view/widget/KeyCaptureEditText.kt` | **【无此文件】** | **[新增]** 带 `privateImeOptions="com.yuyan.KEY_CAPTURE"` 标识的输入框，直接捕获软键盘物理键值 |
| `yuyansdk/.../utils/DialogStyleUtils.kt` | **【无此文件】** | **[新增]** 全局弹窗 20dp 圆角卡片样式工具类，根据设置 UI 日/夜模式自适应调色，全局隐藏【取消】按键 |
| `yuyansdk/.../utils/CardPreferenceStyler.kt` | **【无此文件】** | **[新增]** 设置页面圆角卡片化统一样式渲染器 |
| `yuyansdk/.../view/preference/TwinThemeSelectPreference.kt` | **【无此文件】** | **[新增]** 亮色模式主题与暗色模式主题双列并排展示与选中控件 |
| `yuyansdk/.../data/theme/CustomFontManager.kt` | **【无此文件】** | **[新增]** 自定义字体与功能键英文字体标签管理器 |
| `yuyansdk/.../data/theme/ThemeArchiveUtils.kt` | **【无此文件】** | **[新增]** `.yus` 主题皮肤压缩包一键导入与导出工具类 |
| `yuyansdk/.../database/dao/UserWordDao.kt` & `UserWordManager.kt` | **【无此文件】** | **[新增]** 自定义词库 DAO 与用户词添加管理类 |
| `yuyansdk/.../database/dao/QuickPhraseDao.kt` & `QuickPhrase.kt` | **【无此文件】** | **[新增]** 快捷短语 DAO 与数据库映射实体类 |
| `yuyansdk/.../prefs/behavior/ComposingDisplayMode.kt` | **【无此文件】** | **[新增]** 编码串显示模式枚举 |
| `yuyansdk/.../prefs/behavior/Lx17PrefixMode.kt` | **【无此文件】** | **[新增]** 乱序 17 前缀选择模式枚举 |
| `yuyansdk/.../keyboard/container/VoiceKeyboardContainer.kt` | **【无此文件】** | **[新增]** 离线语音输入键盘容器，带实时音频波形可视化与连续删除/上滑清空手势 |
| `yuyansdk/.../ui/keyboard/VoiceKeyboardLayout.kt` | **【无此文件】** | **[新增]** 语音识别波形图控件 |
| `yuyansdk/src/main/assets/pinyindb/english_words.txt` | **【无此文件】** | **[新增]** Top 6,000 高频英文词库表（用于 9键/LX17 中英混输匹配） |
| `yuyansdk/src/main/assets/sound/` (12+ 种 `.ogg`/`.wav` 音效) | **【无此文件】** | **[新增]** 青轴、红轴、茶轴、黑莓、微信等 12+ 种点击与删除机械按键音效包 |
| `theme/` (`*.yus` 15+ 个皮肤文件) | **【无此文件】** | **[新增]** 包含复刻微信、微信暗、海盐芝士、深海幽蓝、日落晚霞、暗夜极光等预设皮肤包 |
| `app/src/main/jni/` (全组 C++/JNI 原生代码) | **【无此文件】** | **[新增]** Sherpa-onnx 语音推理 JNI 代码、Kaldi Native Fbank 及 KissFFT 依赖 |
| `app/src/main/jniLibs/arm64-v8a/` (语音 `.so` 库) | **【无此文件】** | **[新增]** `libsherpa-onnx-jni.so` 与 `libonnxruntime.so` 离线语音 ASR 原生推理 C++ 动态库 |

---

### 3. 重构与优化的代码文件对比 (Refactored & Optimized Files)

| 文件名 / 相对路径 | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../ui/fragment/VoiceSettingsFragment.kt` | **仅有 1 行代码的空壳占位类**（原版无任何实际语音功能） | **[全新开发]** 完整的 Sherpa-onnx 离线语音识别模型管理页面（包含模型状态显示、`.tar.bz2`/`.zip` 解压导入、下载链接与常驻内存开关） |
| `yuyansdk/.../service/ImeService.kt` | 基础 IME 服务逻辑，物理键盘坐标无 `isNaN()` 校验 | 增加 `isNaN()` 物理键盘坐标防崩溃校验，集成 ASR 语音识别与卡片弹窗事件 |
| `yuyansdk/.../service/DictDecoder.kt` | 基础候选词解码 | 整合 T9/LX17 中英混输匹配 (`T9EnglishPredictor`)，算清 `englishOffset` 消除错位；支持 `comment == "🔤"` 英文词及个性化短语选择上屏 |
| `yuyansdk/.../service/InputDispatcher.kt` | 基础 Rime 参数更新 | 新增 `RimeEngine.setImeOption("echo", cnEnInput)` 中英混输参数同步 |
| `yuyansdk/.../inputmethod/CustomEngine.kt` | 仅支持基础日期时间短语查询，硬编码追加 `，` 和 `。` | 支持从 Room `PhraseDao` 精确查询 T9 数字 (`34944`)、全键拼音 (`dizhi`) 及 LX17 (`GXXD`) 个性化短语；**消除了打字/空格后硬编码弹逗号句号的问题** |
| `yuyansdk/.../inputmethod/RimeEngine.kt` | 基础 Rime 联想词预测 | **敲击空格结尾时自动清空联想栏**，保持候选词栏清爽 |
| `yuyansdk/.../keyboard/InputView.kt` | 基础按键处理 | 增加 `isKeyCaptureInput` 逻辑，拦截 Pinyin 拼音输入并实现键值直接捕获；支持语音键盘波形容器 |
| `yuyansdk/.../ui/fragment/ImeSettingsFragment.kt` | 传统列表式设置 UI | **[重构]** 圆角卡片化 UI，新增【方案与数据】分类与【个性化短语】入口，使用 `ic_menu_voice` 话筒图标 |
| `yuyansdk/.../ui/fragment/theme/CustomThemeActivity.kt` | 系统 PopupMenu 切换模式 | **[重构]** 顶部胶囊按钮点击平滑向下展开 16dp 圆角矩形选择框，弹窗接入 `DialogStyleUtils` |
| `yuyansdk/.../ui/fragment/theme/ThemeListFragment.kt` | 基础主题列表 | **[重构]** 新增主题弹窗带 Emoji 图标，复制主题弹窗修复 `minimumHeight` 溢出并支持 `clipToOutline` |
| `yuyansdk/.../ui/fragment/theme/ThemeThumbnailUi.kt` | 系统 PopupMenu 弹出导出 | **[重构]** 长按主题弹出卡片旁 12dp 圆角矩形 `ListPopupWindow`（仅含导出 .yus），配色解耦键盘主题，根据设置 UI 日夜模式变色 |
| `yuyansdk/.../database/DataBaseKT.kt` | 初始化 5 条默认常用语 | **[优化]** 移除 5 条默认预设常用语并在初始化时自动清理旧有预设，保持自定义短语列表纯净 |
| `yuyansdk/.../database/dao/SkbFunDao.kt` | 基础 DAO | 补充 `deleteBarMenu(name: String)` 支持动态增删菜单图标 |
| `yuyansdk/.../database/dao/PhraseDao.kt` | 基础查询 | 支持对 `t9`、`qwerty`、`lx17` 键码字段的排序与精准检索 |
| `yuyansdk/.../prefs/AppPrefs.kt` | 基础配置 | 新增 `cnEnInput` (关联 `cn_en_input_enable` 混输开关) |
| `yuyansdk/.../view/widget/seekbar/SignSeekBar.kt` | **定义为 Java 类 (`SignSeekBar.java`)** | **改写为 Kotlin 类 (`SignSeekBar.kt`)**，优化数值滑动事件响应 |
| `yuyansdk/.../view/widget/seekbar/SignConfigBuilder.kt` | **定义为 Java 类 (`SignConfigBuilder.java`)** | **改写为 Kotlin 类 (`SignConfigBuilder.kt`)**，提供更优的 Kotlin 构建链 |

---

### 4. 原版独有 / K版清理替代的文件说明 (Official-Only Files & Replaced Classes)

| 文件名 / 相对路径 | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../handwriting/` (全组手写 Kotlin 类) | **包含官方最新手写识别算法类** | **【无此文件】** (已被移除清理) |
| `yuyansdk/.../assets/hw/` (`ocrv5.txt` / `rec.nb` 等) | **包含官方手写识别模型与字典** | **【无此文件】** (已被移除清理) |
| `yuyansdk/.../keyboard/target/TextTarget.kt` | **包含旧版编辑框目标处理逻辑** | **【无此文件】** (已被统一解耦的输入调度器替代) |
| `yuyansdk/.../keyboard/target/PhraseEditTarget.kt` | **包含旧版短语编辑目标逻辑** | **【无此文件】** (已被全新的 `PhrasesSettingsFragment` 与 `KeyCaptureEditText` 替代) |
| `yuyansdk/.../keyboard/target/ImeConnectionTarget.kt` | **包含旧版链接目标类** | **【无此文件】** (已被重构清理) |
| `yuyansdk/.../service/EditorTextCache.kt` | **包含旧版文本缓存类** | **【无此文件】** (已被重构清理) |
| `yuyansdk/.../view/PhraseEditorPanel.kt` | **包含旧版短语编辑面板** | **【无此文件】** (已被全新的卡片化设置页面替代) |
| `yuyansdk/images/*.jpg` & `/images/*.jpg` | **包含官方 README 说明截图图片** | **【无此文件】** (减少源码体积) |
| `yuyansdk/.../drawable/selece_input_mode_*.xml` | **包含注音与仓颉5图标** | **【无此文件】** (极简瘦身) |
