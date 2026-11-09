# 语燕输入法 K版 (YuyanImeK) 说明文档

**语燕输入法 K版 (YuyanImeK)** 是基于官方开源版本 **语燕输入法（YuyanIme）** 源代码进行深度重构、性能优化与功能扩展的 Android 输入法项目。本版本在继承原版极简、无广告、高隐私特性的基础上，全面扩展了离线神经网络语音识别、全模式中英混输预测、个性化短语精准映射、全局圆角卡片 UI 与弹窗交互体系。

---

# 🚀 第一部分：K版与原版 (YuyanIme) 相比的新功能与优化

### 1. 🎙️ 离线神经网络语音识别引擎 (Offline ASR)
- **零网络依赖**：集成 ONNXRuntime 与 Sherpa-onnx Zipformer 离线语音识别框架，语音解析完全在手机本地计算，不上传任何音频数据，保障极致隐私。
- **实时波形可视化界面 (`VoiceKeyboardContainer`)**：全新设计了支持动态 FFT 音频频谱波形动画的语音键盘，支持按住说话、松开上屏、长按滑动快速控制光标、连续删除与上滑清空手势。
- **离线模型导入与常驻**：支持一键选择并解压导入 Sherpa-onnx 模型包（中英文混合识别 + 自动标点），提供模型常驻内存选项以消除首次加载延迟。

### 2. 🔤 九键/乱序17/全键全布局【中英混输】预测
- **应用层 Key-Pattern 极速预测引擎 (`T9EnglishPredictor`)**：
  - **中文 9 键 (T9)**：输入数字 `4663` 即可在候选词栏中自动匹配并提示英文单词 `good`。
  - **乱序 17 (LX17)**：输入按键组合 `G`-`oXv`-`oXv`-`D` (`GXXD`) 即可自动换算并精确提示英文单词 `good`。
  - **26 字母全映射**：完整实现了 26 个英文字母在 LX17 键位上的主/备用按键变体映射，倒排索引查表耗时 $<1\text{ms}$，零卡顿。
- **独立开关控制**：在【输入设置】中新增【中英混输】（`cn_en_input_enable`）独立开关，可随时控制 Rime `echo` 混输状态。

### 3. ✍️ 按键直接捕获型【个性化短语】 (Personalized Phrases)
- **专属管理界面 (`PhrasesSettingsFragment`)**：在主设置【方案与数据】分类中新增【个性化短语】独立管理卡片。
- **软键盘按键直捕 (`KeyCaptureEditText`)**：
  - 上方【输入码】框获得焦点时，**自动隔绝并阻止普通拼音打字**，直接捕获软键盘/物理键盘点击的原始按键键值（9 键直接填入 `34944`、乱序 17 直接填入 `GXXD`、全键填入 `dizhi`）。
- **模式绑定与精确出词**：短语与添加时的键盘模式绑定（列表中清晰标注 `[中文9键]`、`[乱序17]` 或 `[拼音全键]`），打字时通过按键序列精准置顶弹出自定义长短语。

### 4. ⌨️ 物理键盘悬浮候选词栏体验升级
- **数字序号前缀**：悬浮候选词栏增加 `1.` `2.` `3.` 序号前缀显示 (`FloatCandidatesBarAdapter`)，贴近 PC 拼音输入习惯。
- **方向键与翻页历史栈**：支持方向键 `DPAD_LEFT/RIGHT` 高亮选词及 `DPAD_UP/DOWN` 快速翻页（含 `pageStarts` 页面历史轨迹栈）。
- **`isNaN()` 异常防护**：在 `ImeService.onUpdateCursorAnchorInfo` 中增加了坐标数值容错，消除了部分应用窗口 Transform 矩阵异常导致的悬浮窗崩溃。

### 5. 🎨 统一圆角卡片 UI 与全局弹窗体系重构
- **主设置卡片化 (`CardPreferenceStyler`)**：全量重构为精致的白色圆角卡片（18dp 分栏间距、统一矢量图标与水波纹触控）。
- **全局圆角弹窗重构 (`DialogStyleUtils`)**：
  - 所有对话框统一升级为 20dp 柔和圆角卡片，弹窗底色与文字**彻底与键盘选中的主题解耦**，严格跟随设置页面 UI 的日/夜间模式自适应变色。
  - 全局隐藏无用的【取消】按键，极简干净；长按主题弹出卡片旁 12dp 圆角矩形下拉菜单（仅含 `导出主题 (.yus)`）。
  - 主题编辑器顶部的键盘切换胶囊按钮，点击可向下平滑展开 16dp 圆角矩形选择框。

### 6. ✍️ 神经网络手写识别引擎升级与全架构补全
- **Paddle/ONNX 神经网络手写**：升级了手写识别框架 (`HandwritingRecognizer`)，基于 Paddle OCR v5 识别模型与 `ocrv5.txt` 字典。
- **补全 32位 / x86 架构**：补全了 `arm64-v8a`、`armeabi-v7a` (32位)、`x86`、`x86_64` 原生 JNI 库 (`libyuyanwriting.so`)，消除了 32 位低端机和模拟器手写崩溃问题。

### 7. 🎵 音效、主题皮肤与日期时间预测扩展
- **丰富机械轴与微信按键音效**：内置青轴、红轴、茶轴、黑莓、微信音效等多种高清点击与删除音效（`assets/sound/`）。
- **精致预设主题 (.yus)**：内置复刻微信、微信暗、赛博朋克霓虹、莫兰迪极简粉、炫彩彩虹拼盘、小米大回车等预设皮肤，支持 `.yus` 压缩包一键导入导出。
- **日期时间预测**：扩展了乱序 17、双拼、九键和全键下的日期与时间预测功能（输入 `rq` / `riqi` / `sj` / `shijian` 实时提示当前日期时间）。

---

# 📂 第二部分：原版源代码 (YuyanIme-master) 与 K版 (YuyanImeK-master) 详细文件对比表

> [!NOTE]
> 说明：以下表格对比了官方最新源代码（`C:\Users\kuang\Downloads\YuyanIme-master`）与当前 K版源代码（`YuyanImeK-master`）。若某文件仅存在于其中一方，对应列标注为 **【无此文件】**。未在下表中列出的文件在两版之间完全一致，可直接互相替换。

---

### 1. 核心业务、输入法服务与引擎逻辑文件对比 (Core Logic & Services)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../service/ImeService.kt` | 基础 IME 服务逻辑，物理键盘坐标无 `isNaN()` 校验 | 增加 `isNaN()` 物理键盘坐标防崩溃校验，集成 ASR 语音识别与卡片弹窗事件 |
| `yuyansdk/.../service/DictDecoder.kt` | 基础候选词解码 | 整合 T9/LX17 中英混输匹配 (`T9EnglishPredictor`)，支持 `comment == "🔤"` 英文词及个性化短语选择上屏 |
| `yuyansdk/.../service/InputDispatcher.kt` | 仅支持基础 Rime 参数更新 | 新增 `RimeEngine.setImeOption("echo", cnEnInput)` 中英混输参数同步 |
| `yuyansdk/.../service/T9EnglishPredictor.kt` | **【无此文件】** | **[新增]** 九键 (T9) 与乱序 17 (LX17) 中英混输预测引擎，支持 26 字母多按键变体映射 |
| `yuyansdk/.../inputmethod/CustomEngine.kt` | 仅支持基础日期时间短语查询 | 支持从 Room `PhraseDao` 精确查询 T9 数字 (`34944`)、全键拼音 (`dizhi`) 及 LX17 (`GXXD`) 个性化短语 |
| `yuyansdk/.../inputmethod/RimeEngine.kt` | 基础 Rime 引擎桥接 | 增强多编码组合推导，支持个性化短语和混输词置顶 |
| `yuyansdk/.../inputmethod/HWEngine.kt` | 调用旧版 JNI `HandWriting.init` | 调用全新的 `HandwritingRecognizer` 神经网络手写识别 API |
| `yuyansdk/.../inputmethod/core/HandWriting.kt` | 旧版 JNI 手写 C++ 接口类 | 包装新版 `yuyanwriting` 原生推理句柄 (`nativeCreate`/`nativeRun`/`nativeRelease`) |

---

### 2. 离线语音识别 (ASR) 模块文件对比 (Speech Recognition ASR Module)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../service/AsrInferenceClient.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别 AIDL 服务客户端通信类 |
| `yuyansdk/.../service/AsrInferenceService.kt` | **【无此文件】** | **[新增]** ASR 离线语音识别后台推理服务 |
| `yuyansdk/.../service/VoiceRecognitionHandler.kt` | **【无此文件】** | **[新增]** 语音识别长按录入与文本回调句柄 |
| `yuyansdk/.../speech/AsrSupport.kt` | **【无此文件】** | **[新增]** 离线语音识别模型 Warmup 热身与生命周期管理 |
| `yuyansdk/.../speech/AsrModelManager.kt` | **【无此文件】** | **[新增]** 离线语音模型 Archive 压缩包解压与导入管理 |
| `yuyansdk/.../speech/AsrBackendFactory.kt` | **【无此文件】** | **[新增]** Sherpa-onnx 语音推理 Backend 工厂 |
| `yuyansdk/.../speech/SpeechRecognitionManager.kt` | **【无此文件】** | **[新增]** 语音识别流式音频采集与 JNI 数据推流 |
| `yuyansdk/.../speech/SpectrumAnalyzer.kt` | **【无此文件】** | **[新增]** 实时音频 FFT 频谱分析器（用于波形 UI） |
| `yuyansdk/.../speech/AsrBackend.kt` | **【无此文件】** | **[新增]** 语音识别推理后端抽象接口 |
| `yuyansdk/.../speech/AsrNative.kt` | **【无此文件】** | **[新增]** Sherpa-onnx JNI 原生 C++ 接口 |
| `yuyansdk/.../speech/OfflineAsrBackend.kt` | **【无此文件】** | **[新增]** 离线 Zipformer 模型推理后端实现类 |
| `yuyansdk/.../speech/SpeechTypes.kt` | **【无此文件】** | **[新增]** 语音识别状态与配置枚举模型 |
| `yuyansdk/.../speech/models/*Profile.kt` | **【无此文件】** | **[新增]** X-ASR 中英文+标点模型及 Zipformer 模型的配置文件 |
| `yuyansdk/.../aidl/.../IInferenceAsrService.aidl` | **【无此文件】** | **[新增]** ASR 推理服务 AIDL 跨进程接口 |
| `yuyansdk/.../aidl/.../IInferenceAsrCallback.aidl` | **【无此文件】** | **[新增]** ASR 文本识别回调 AIDL 接口 |

---

### 3. 手写识别模块文件对比 (Handwriting Engine Module)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../handwriting/HandwritingRecognizer.kt` | 旧版基础封装 | **[重构]** 线程池异步加载 `Predictor` 与 `RecEngine` 模型 |
| `yuyansdk/.../handwriting/internal/Predictor.kt` | 旧版实现 | **[重构]** 负责 Paddle/ONNX 手写模型 `rec.nb` 与字典 `ocrv5.txt` 的解算 |
| `yuyansdk/.../handwriting/internal/RecEngine.kt` | 旧版实现 | **[重构]** 行切分与逐字 `CtcDecoder` 束搜索合并手写候选词 |
| `yuyansdk/.../handwriting/internal/CtcDecoder.kt` | 旧版解码 | **[重构]** 基于字典束搜索（Beam Search）的 CTC 概率解码器 |
| `yuyansdk/.../handwriting/internal/ImagePreprocessor.kt` | 基础预处理 | **[重构]** 笔画 SmoothPath 渲染与 $48 \times 320$ 归一化图像预处理 |
| `yuyansdk/.../handwriting/internal/StrokeSegmenter.kt` | 基础切分 | **[重构]** 基于空隙与宽高比的多字手写笔画分割器 |
| `yuyansdk/.../handwriting/Models.kt` | 基础数据结构 | 数据类 `Stroke` 与 `Candidate` 封装 |
| `yuyansdk/.../handwriting/ShortStrokeParser.kt` | 基础解析 | 手写 Short 点阵序列转换解析器 |
| `yuyansdk/.../entity/handwriting/Bezier.kt` | **【无此文件】** | **[新增]** 贝塞尔曲线平滑手写笔迹算法类 |
| `yuyansdk/.../entity/handwriting/ControlTimedPoints.kt` | **【无此文件】** | **[新增]** 带有时间戳的手写控制点数据模型 |
| `yuyansdk/.../entity/handwriting/TimedPoint.kt` | **【无此文件】** | **[新增]** 包含 X, Y, 压力的时间点阵模型 |

---

### 4. UI、适配器、设置与弹窗控件文件对比 (UI, Adapters & Dialogs)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../ui/fragment/ImeSettingsFragment.kt` | 传统列表式设置 UI | **[重构]** 圆角卡片化 UI，新增【方案与数据】分类与【个性化短语】入口，使用 `ic_menu_voice` 话筒图标 |
| `yuyansdk/.../ui/fragment/PhrasesSettingsFragment.kt` | **【无此文件】** | **[新增]** 个性化短语管理页面，支持自动检测当前键盘模式（9键/LX17/全键）与卡片列表管理 |
| `yuyansdk/.../ui/fragment/VoiceSettingsFragment.kt` | 基础语音设置 | **[重构]** 支持 Sherpa-onnx 模型导入、在线下载链接与常驻内存开关 |
| `yuyansdk/.../ui/fragment/theme/CustomThemeActivity.kt` | 系统 PopupMenu 切换模式 | **[重构]** 顶部胶囊按钮点击平滑向下展开 16dp 圆角矩形选择框，弹窗接入 `DialogStyleUtils` |
| `yuyansdk/.../ui/fragment/theme/ThemeListFragment.kt` | 基础主题列表 | **[重构]** 新增主题弹窗带 Emoji 图标，复制主题弹窗修复 `minimumHeight` 溢出并支持 `clipToOutline` |
| `yuyansdk/.../ui/fragment/theme/ThemeThumbnailUi.kt` | 系统 PopupMenu 弹出导出 | **[重构]** 长按主题弹出卡片旁 12dp 圆角矩形 `ListPopupWindow`（仅含导出 .yus），配色解耦键盘主题，根据设置 UI 日夜模式变色 |
| `yuyansdk/.../view/widget/KeyCaptureEditText.kt` | **【无此文件】** | **[新增]** 带 `privateImeOptions="com.yuyan.KEY_CAPTURE"` 标识的输入框，直接捕获软键盘物理键值 |
| `yuyansdk/.../view/widget/ImeEditText.kt` | `ImeEditText` 类 | `open class ImeEditText` 允许继承 |
| `yuyansdk/.../utils/DialogStyleUtils.kt` | **【无此文件】** | **[新增]** 全局弹窗 20dp 圆角卡片样式工具类，根据设置 UI 日/夜模式自适应调色，全局隐藏【取消】按键 |
| `yuyansdk/.../utils/CardPreferenceStyler.kt` | **【无此文件】** | **[新增]** 设置页面圆角卡片化统一样式渲染器 |
| `yuyansdk/.../adapter/FloatCandidatesBarAdapter.kt` | **【无此文件】** | **[新增]** 物理键盘专属候选词适配器，支持数字序号前缀（`1.` `2.` `3.`）与高亮激活绑定 |
| `yuyansdk/.../candidate/FloatCandidateBar.kt` | 基础悬浮候选栏 | 支持 DPAD 方向键高亮选词与 DPAD_UP/DOWN 翻页及 `pageStarts` 历史轨迹栈 |
| `yuyansdk/.../adapter/MenuAdapter.kt` | 主线程实时读写 Room DB | 接入 `SkbMenuOrderManager` 内存缓存，消除 UI 绑定卡顿 |
| `yuyansdk/.../manager/SkbMenuOrderManager.kt` | **【无此文件】** | **[新增]** 菜单顺序单例内存缓存 + 后台 ThreadPoolUtils 异步数据库落盘 |
| `yuyansdk/.../keyboard/InputView.kt` | 基础按键处理 | 增加 `isKeyCaptureInput` 逻辑，拦截 Pinyin 拼音输入并实现键值直接捕获；支持语音键盘波形容器 |
| `yuyansdk/.../keyboard/container/VoiceKeyboardContainer.kt` | **【无此文件】** | **[新增]** 离线语音输入键盘容器，带实时音频波形可视化与连续删除/上滑清空手势 |
| `yuyansdk/.../ui/keyboard/VoiceKeyboardLayout.kt` | **【无此文件】** | **[新增]** 语音识别波形图控件 |

---

### 5. 偏好设置、数据模型与数据库文件对比 (Database & Preferences)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../database/DataBaseKT.kt` | 初始化 5 条默认常用语 | **[优化]** 移除 5 条默认预设常用语并在初始化时自动清理旧有预设，保持自定义短语列表纯净 |
| `yuyansdk/.../database/dao/SkbFunDao.kt` | 基础 DAO | 补充 `deleteBarMenu(name: String)` 支持动态增删菜单图标 |
| `yuyansdk/.../database/dao/PhraseDao.kt` | 基础查询 | 支持对 `t9`、`qwerty`、`lx17` 键码字段的排序与精准检索 |
| `yuyansdk/.../database/entry/Phrase.kt` | 包含 `t9/qwerty/lx17` 字段 | 存储自定义短语及其对应的 9键/26键/LX17 键码 |
| `yuyansdk/.../prefs/AppPrefs.kt` | 基础配置 | 新增 `cnEnInput` (关联 `cn_en_input_enable` 混输开关) |
| `yuyansdk/.../prefs/behavior/ChineseAssociationMode.kt` | **【无此文件】** | **[新增]** 中文联想模式枚举（关闭联想/单次联想/连续联想） |
| `yuyansdk/.../data/theme/ThemeArchiveUtils.kt` | **【无此文件】** | **[新增]** `.yus` 主题皮肤压缩包一键导入与导出工具类 |

---

### 6. 音效、主题皮肤、布局与资源文件对比 (Assets, Sounds, Themes & Resources)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/src/main/assets/pinyindb/english_words.txt` | **【无此文件】** | **[新增]** Top 6,000 高频英文词库表（用于 9键/LX17 中英混输匹配） |
| `yuyansdk/src/main/assets/hw/ocrv5.txt` | 旧版/无 | **[新增]** Paddle/ONNX 手写识别字典库 |
| `yuyansdk/src/main/assets/hw/rec.nb` | 旧版/无 | **[新增]** Paddle/ONNX 手写识别神经网络模型文件 |
| `yuyansdk/src/main/assets/sound/` (`bd_axis_*.ogg`, `wx_*.wav`, etc.) | 基础键盘音效 | **[新增]** 青轴、红轴、茶轴、黑莓、微信等 12+ 种高清机械音效包 |
| `theme/` (`*.yus`) | **【无此文件】** | **[新增]** 包含复刻微信、微信暗、赛博朋克霓虹、莫兰迪极简粉、炫彩彩虹拼盘、小米大回车等预设皮肤包 |
| `yuyansdk/libs/` (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) | 缺少部分架构 `.so` | **[补全]** 包含 `libyuyanime.so` 与 `libyuyanwriting.so` 完整多架构动态库 |
| `yuyansdk/src/main/res/drawable/ic_menu_voice.xml` | **【无此文件】** | **[新增]** 重新绘制的话筒语音识别矢量图标 |
| `yuyansdk/src/main/res/drawable/` (`icon_symbol_lock_pretty`, `ic_arrow_*_pretty`, `sdk_bg_settings_card_*`) | 基础图标 | **[新增]** 全套卡片化圆角背景图与精致矢量图标资源 |
| `yuyansdk/src/main/res/layout/` (`sdk_item_category_card`, `sdk_item_preference_card_*`) | 传统布局 | **[新增]** 7+ 款卡片化圆角 Setting Preference 布局模版文件 |
| `yuyansdk/src/main/res/layout/sdk_item_recyclerview_float_candidates_bar.xml` | **【无此文件】** | **[新增]** 物理键盘悬浮候选词带数字序号项的布局 XML |
| `yuyansdk/src/main/res/navigation/settings_nav.xml` | 基础设置路由 | 增加了 `phrasesSettingsFragment` 个性化短语导航路由 |
| `yuyansdk/src/main/res/values/strings.xml` | 基础字符串 | 新增 `<string name="cn_en_input">中英混输</string>` 等语言资源 |

---

### 7. C++/JNI 原生库与构建配置对比 (C++/JNI Core & Build Config)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `app/src/main/jni/` (`CMakeLists.txt`, `asr/`, `onnxruntime/`, `third_party/knf/`) | **【无此文件】** | **[新增]** C++ 原生构建流，包含 Sherpa-onnx 语音推理 JNI 代码、Kaldi Native Fbank 及 KissFFT 依赖 |
| `app/src/main/jniLibs/arm64-v8a/` (`libsherpa-onnx-jni.so`, `libonnxruntime.so`, etc.) | **【无此文件】** | **[新增]** 离线语音 ASR 原生推理 C++ 动态链接库 |
| `app/build.gradle` | `versionName "K52"` / `versionCode 52` | `versionName "K53"` / `versionCode 53`，优化 APK 命名与 AAPT 资源压缩规则 |
| `app/src/main/AndroidManifest.xml` | 基础 Manifest | 注册 `AsrInferenceService` 离线语音服务并声明 `RECORD_AUDIO` 录音权限 |
