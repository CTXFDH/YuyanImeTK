# 语燕输入法 K版 (YuyanImeK) 说明文档

**语燕输入法 K版 (YuyanImeK)** 是基于官方最新开源项目 **语燕输入法（YuyanIme-master）** 源代码进行深度重构、功能扩展与 UI 交互优化的 Android 输入法版本。在保持原版极简、无广告、高隐私特性的同时，K版独有地引入了离线神经网络语音识别、九键/乱序17/全键全模式中英混输预测、按键直接捕获的个性化短语定制、全局圆角卡片 UI 与统一弹窗交互体系。

---

# 🚀 第一部分：K版相比原版 (YuyanIme-master) 的独有新功能与优化

### 1. 🎙️ 离线神经网络语音识别引擎 (K版全新开发)
- **零网络依赖**：内置 ONNXRuntime 与 Sherpa-onnx Zipformer 离线语音识别框架，完全在手机本地进行语音流式解算，不上传任何数据（原版官方源码仅有一个 1 行代码的空壳类，无任何语音识别功能）。
- **动态波形键盘 UI (`VoiceKeyboardContainer`)**：支持实时的音频波形动画可视化、按住说话、松开上屏、长按滑动快速控制光标、连续删除与上滑清空手势。
- **离线模型导入与常驻**：支持一键解压导入 Sherpa-onnx 模型包（中英文混合识别 + 自动标点），提供模型常驻内存选项以消除首次加载延迟。

### 2. 🔤 9键/乱序17/全键全模式【中英混输】预测 (K版扩展)
- **应用层 Key-Pattern 极速预测引擎 (`T9EnglishPredictor`)**：
  - 在 **中文 9 键 (T9)** 下输入 `4663` 即可直接在候选词中提示 `good`。
  - 在 **乱序 17 (LX17)** 下输入 `G`-`oXv`-`oXv`-`D` (`GXXD`) 即可自动匹配提示 `good`。
  - 完整映射了 26 个英文字母在 LX17 全布局上的主/备用按键变体，查表耗时 $<1\text{ms}$，零卡顿。
- **独立设置开关**：在【输入设置】中增加【中英混输】（`cn_en_input_enable`）独立开关，可随时切换 Rime `echo` 混输状态。

### 3. ✍️ 按键直接捕获型【个性化短语】 (K版独有)
- **设置管理入口**：在主设置【方案与数据】分类中新增【个性化短语】独立管理卡片。
- **软键盘按键直捕 (`KeyCaptureEditText`)**：
  - 上方【输入码】框获得焦点时，**自动拦截并隔绝对话框内的拼音打字与拼写串**，直接捕获软键盘点击的原始按键键值（9 键直接填入 `34944`、乱序 17 直接填入 `GXXD`、全键填入 `dizhi`）。
- **模式绑定与精准出词**：短语与添加时的键盘模式绑定（列表中清晰标注 `[中文9键]`、`[乱序17]` 或 `[拼音全键]`），打字时通过按键序列精准置顶弹出自定义长短语。

### 4. 🎨 圆角卡片化设置 UI 与全局弹窗样式重构 (K版重构)
- **`CardPreferenceStyler` 设置 UI**：主设置全面重构为精致的白色圆角矩形卡片（18dp 卡片间距、统一矢量图标与水波纹触控）。
- **`DialogStyleUtils` 全局弹窗重构**：
  - 所有对话框应用 20dp 柔和圆角卡片，弹窗底色与文字**彻底与键盘选中的主题解耦**，严格跟随设置页面 UI 的日/夜间模式自适应变色。
  - 全局隐藏无用的【取消】按键，极简干净；长按主题弹出卡片旁小巧的 12dp 圆角矩形下拉菜单（仅含 `导出主题 (.yus)`）。
  - 主题编辑器顶部的键盘切换胶囊按钮，点击可向下平滑展开 16dp 圆角矩形选择框。

### 5. 🎵 按键机械音效、精美主题与预测扩展 (K版扩展)
- **丰富机械轴与微信按键音效**：内置青轴、红轴、茶轴、黑莓、微信音效等 12+ 种点击与删除音效（`assets/sound/`）。
- **精致预设主题 (.yus)**：内置复刻微信、微信暗、赛博朋克霓虹、莫兰迪极简粉、炫彩彩虹拼盘、小米大回车等预设皮肤，支持 `.yus` 压缩包一键导入导出。
- **日期时间预测**：扩展了乱序 17、双拼、九键和全键下的日期与时间预测功能（输入 `rq` / `riqi` / `sj` / `shijian` 实时提示当前日期时间）。

### 6. ✍️ 神经网络手写识别支持 (同步自原版并优化)
- **兼容与稳定优化**：同步并优化了原版作者引入的 Paddle/ONNX 手写识别算法 (`HandwritingRecognizer.kt`)，补全了 `arm64-v8a`、`armeabi-v7a` (32位)、`x86`、`x86_64` 全架构原生动态库，消除了 32 位低端机和模拟器崩溃问题。

---

# 📂 第二部分：原版源代码 (YuyanIme-master) 与 K版 (YuyanImeK-master) 详细文件对比表

> [!NOTE]
> 说明：下表对比了官方最新源代码（`C:\Users\kuang\Downloads\YuyanIme-master`）与当前 K版源代码（`YuyanImeK-master`）。若某文件/模块为 K版独有新增，原版列标注为 **【无此文件】**。未在下表中列出的文件在两版之间完全一致，可直接互相替换。

---

### 1. 核心构建与工程配置文件对比 (Build & Project Config)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `build.gradle` (Root) | 使用 Gradle Version Catalogs (`alias(libs.plugins...)`) 统一定义插件版本 | 采用传统 Groovy 插件直接依赖，内置 `buildscript` 声明，保障 IDE 各版本的兼容性 |
| `app/build.gradle` | 采用 Version Catalogs；`versionName = versionNameDate()` 动态生成纯日期版本号（如 `20261107.12`）；配置 `offline` 变体与 release 签名脚本；输出 `yuyanIme_${versionCodeDate()}_${variant.buildType}.apk` | 采用 K版统一递增版本号（`versionName "K53"`、`getKVersionCode() = 53`）；解耦硬件包强签名依赖；输出自定义命名 `YuyanImeK-K53_${versionCodeDate()}.apk` 并优化 AAPT 资源压缩 |
| `yuyansdk/build.gradle` | 使用 Version Catalogs (`libs.androidx.appcompat`) 引用依赖；使用 KSP 编译器 | 显示声明 Jetpack、Room、Navigation、Compose、ARouter 及 ONNX 依赖；开启 `buildFeatures.aidl = true` 与 `compose = true`；使用 kapt 编译器保证稳定性 |
| `app/src/main/AndroidManifest.xml` | 声明基础 IME 权限与应用组件 | 增加了 `RECORD_AUDIO` 录音权限，注册了 `AsrInferenceService` 离线语音识别跨进程 AIDL 后台服务 |

---

### 2. K版独有新增模块文件对比 (K-Version Unique Files - 原版【无此文件】)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../service/T9EnglishPredictor.kt` | **【无此文件】** | **[新增]** 九键 (T9) 与乱序 17 (LX17) 中英混输预测引擎，支持 26 字母多按键变体映射 |
| `yuyansdk/.../service/AsrInferenceClient.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别 AIDL 服务客户端通信类 |
| `yuyansdk/.../service/AsrInferenceService.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别后台推理服务 |
| `yuyansdk/.../service/VoiceRecognitionHandler.kt` | **【无此文件】** | **[新增]** 语音识别长按录入与文本回调句柄 |
| `yuyansdk/.../speech/` (全组 11 个文件) | **【无此文件】** | **[新增]** Sherpa-onnx 离线语音推理引擎、模型管理、频谱分析器与生命周期管理 |
| `yuyansdk/.../aidl/` (全组 2 个 AIDL 文件) | **【无此文件】** | **[新增]** ASR 语音推理服务与文本回调跨进程通信接口 |
| `yuyansdk/.../ui/fragment/PhrasesSettingsFragment.kt` | **【无此文件】** | **[新增]** 个性化短语管理页面，支持自动检测当前键盘模式（9键/LX17/全键）与卡片列表管理 |
| `yuyansdk/.../view/widget/KeyCaptureEditText.kt` | **【无此文件】** | **[新增]** 带 `privateImeOptions="com.yuyan.KEY_CAPTURE"` 标识的输入框，直接捕获软键盘物理键值 |
| `yuyansdk/.../utils/DialogStyleUtils.kt` | **【无此文件】** | **[新增]** 全局弹窗 20dp 圆角卡片样式工具类，根据设置 UI 日/夜模式自适应调色，全局隐藏【取消】按键 |
| `yuyansdk/.../utils/CardPreferenceStyler.kt` | **【无此文件】** | **[新增]** 设置页面圆角卡片化统一样式渲染器 |
| `yuyansdk/.../data/theme/ThemeArchiveUtils.kt` | **【无此文件】** | **[新增]** `.yus` 主题皮肤压缩包一键导入与导出工具类 |
| `yuyansdk/.../keyboard/container/VoiceKeyboardContainer.kt` | **【无此文件】** | **[新增]** 离线语音输入键盘容器，带实时音频波形可视化与连续删除/上滑清空手势 |
| `yuyansdk/.../ui/keyboard/VoiceKeyboardLayout.kt` | **【无此文件】** | **[新增]** 语音识别波形图控件 |
| `yuyansdk/src/main/assets/pinyindb/english_words.txt` | **【无此文件】** | **[新增]** Top 6,000 高频英文词库表（用于 9键/LX17 中英混输匹配） |
| `yuyansdk/src/main/assets/sound/` (12+ 种 `.ogg`/`.wav` 音效) | **【无此文件】** | **[新增]** 青轴、红轴、茶轴、黑莓、微信等 12+ 种点击与删除机械按键音效包 |
| `theme/` (`*.yus` 7 个皮肤文件) | **【无此文件】** | **[新增]** 包含复刻微信、微信暗、赛博朋克霓虹、莫兰迪极简粉、炫彩彩虹拼盘、小米大回车等预设皮肤包 |
| `app/src/main/jni/` (全组 C++/JNI 代码) | **【无此文件】** | **[新增]** Sherpa-onnx 语音推理 JNI 代码、Kaldi Native Fbank 及 KissFFT 依赖 |
| `app/src/main/jniLibs/arm64-v8a/` (语音 `.so` 库) | **【无此文件】** | **[新增]** `libsherpa-onnx-jni.so` 与 `libonnxruntime.so` 离线语音 ASR 原生推理 C++ 动态库 |

---

### 3. 重构与优化的代码文件对比 (Refactored & Optimized Files)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../ui/fragment/VoiceSettingsFragment.kt` | **仅有 1 行代码的空壳占位类**（原版完全无语音识别功能） | **[全新开发]** 完整的 Sherpa-onnx 离线语音识别模型管理页面（包含模型状态显示、解压导入、下载链接引导与常驻内存开关） |
| `yuyansdk/.../service/ImeService.kt` | 基础 IME 服务逻辑，物理键盘坐标无 `isNaN()` 校验 | 增加 `isNaN()` 物理键盘坐标防崩溃校验，集成 ASR 语音识别与卡片弹窗事件 |
| `yuyansdk/.../service/DictDecoder.kt` | 基础候选词解码 | 整合 T9/LX17 中英混输匹配 (`T9EnglishPredictor`)，支持 `comment == "🔤"` 英文词及个性化短语选择上屏 |
| `yuyansdk/.../service/InputDispatcher.kt` | 基础 Rime 参数更新 | 新增 `RimeEngine.setImeOption("echo", cnEnInput)` 中英混输参数同步 |
| `yuyansdk/.../inputmethod/CustomEngine.kt` | 仅支持基础日期时间短语查询 | 支持从 Room `PhraseDao` 精确查询 T9 数字 (`34944`)、全键拼音 (`dizhi`) 及 LX17 (`GXXD`) 个性化短语 |
| `yuyansdk/.../keyboard/InputView.kt` | 基础按键处理 | 增加 `isKeyCaptureInput` 逻辑，拦截 Pinyin 拼音输入并实现键值直接捕获；支持语音键盘波形容器 |
| `yuyansdk/.../ui/fragment/ImeSettingsFragment.kt` | 传统列表式设置 UI | **[重构]** 圆角卡片化 UI，新增【方案与数据】分类与【个性化短语】入口，使用 `ic_menu_voice` 话筒图标 |
| `yuyansdk/.../ui/fragment/theme/CustomThemeActivity.kt` | 系统 PopupMenu 切换模式 | **[重构]** 顶部胶囊按钮点击平滑向下展开 16dp 圆角矩形选择框，弹窗接入 `DialogStyleUtils` |
| `yuyansdk/.../ui/fragment/theme/ThemeListFragment.kt` | 基础主题列表 | **[重构]** 新增主题弹窗带 Emoji 图标，复制主题弹窗修复 `minimumHeight` 溢出并支持 `clipToOutline` |
| `yuyansdk/.../ui/fragment/theme/ThemeThumbnailUi.kt` | 系统 PopupMenu 弹出导出 | **[重构]** 长按主题弹出卡片旁 12dp 圆角矩形 `ListPopupWindow`（仅含导出 .yus），配色解耦键盘主题，根据设置 UI 日夜模式变色 |
| `yuyansdk/.../database/DataBaseKT.kt` | 初始化 5 条默认常用语 | **[优化]** 移除 5 条默认预设常用语并在初始化时自动清理旧有预设，保持自定义短语列表纯净 |
| `yuyansdk/.../database/dao/SkbFunDao.kt` | 基础 DAO | 补充 `deleteBarMenu(name: String)` 支持动态增删菜单图标 |
| `yuyansdk/.../database/dao/PhraseDao.kt` | 基础查询 | 支持对 `t9`、`qwerty`、`lx17` 键码字段的排序与精准检索 |
| `yuyansdk/.../prefs/AppPrefs.kt` | 基础配置 | 新增 `cnEnInput` (关联 `cn_en_input_enable` 混输开关) |
| `yuyansdk/.../handwriting/HandwritingRecognizer.kt` | 官方 Paddle/ONNX 手写识别算法 | 同步官方算法，增加 Kotlin 语法兼容重构、`@Synchronized` 线程锁初始化与 32位/x86 多架构 `.so` 补全 |
