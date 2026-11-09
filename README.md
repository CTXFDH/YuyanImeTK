# 语燕输入法 K版 (YuyanImeK)

语燕输入法 K版 (YuyanImeK) 是基于开源“语燕输入法”（YuyanIme）深度重构与功能增强的 Android 输入法版本。在保持极简、高效、无广告特性的同时，引入了全新的离线神经网络语音识别引擎、全模式中英混输扩展、按键直接捕获的个性化短语定制、卡片化设置 UI 与全局圆角弹窗设计。

---

## 🚀 第一部分：K版与原版相比的新功能与优化

### 1. 🎙️ 离线神经网络语音识别 (Offline ASR Voice Input)
- **原生 Zipformer / ONNX 引擎**：内置 ONNXRuntime 离线语音识别引擎，完全不依赖网络，毫秒级响应，隐私安全无泄漏。
- **动态波形键盘 UI (`VoiceKeyboardContainer`)**：支持实时的音频波形动画可视化、按住说话、长按滑动快速光标控制、连续删除与上滑清空。
- **自定义模型导入**：支持一键解压导入 Sherpa-onnx 离线模型包（包含中英文混合 + 自动标点）。

### 2. 🔤 9键/乱序17/全键全模式【中英混输】增强
- **应用层 Key-Pattern 极速预测 (`T9EnglishPredictor`)**：
  - 在 **中文 9 键 (T9)** 下输入 `4663` 即可直接在候选词中提示 `good`。
  - 在 **乱序 17 (LX17)** 下输入 `G`-`oXv`-`oXv`-`D` (`GXXD`) 即可自动匹配 `good`。
  - 支持 26 个英文字母在 LX17 全布局上的多按键变体映射建索引，查表耗时 $<1\text{ms}$，零卡顿。
- **独立设置开关**：在【输入设置】中增加【中英混输】（`cn_en_input_enable`）独立开关，可随时切换 Rime `echo` 混输状态。

### 3. ✍️ 个性化短语 (Personalized Phrases)
- **设置管理入口**：在主设置【方案与数据】分类中新增【个性化短语】。
- **按键直接捕获 (`KeyCaptureEditText`)**：
  - 焦点停在【输入码】框时，**自动拦截并隔绝对话框内的拼音打字**，直接获取软键盘点击的原始按键键值（9 键填入 `34944`、乱序 17 填入 `GXXD`、全键填入 `dizhi`）。
- **精准出词**：打字时在中文 9 键、乱序 17 或全键下输入对应键码，首位优先弹出用户自定义的长短语。

### 4. ⌨️ 物理键盘悬浮候选词栏优化
- **数字序号前缀**：悬浮候选词栏增加 `1.` `2.` `3.` 序号前缀显示 (`FloatCandidatesBarAdapter`)。
- **方向键与翻页历史栈**：支持方向键 `DPAD_LEFT/RIGHT` 高亮选词及 `DPAD_UP/DOWN` 快速翻页（含 `pageStarts` 历史轨迹栈）。
- **`isNaN()` 异常防护**：在 `ImeService.onUpdateCursorAnchorInfo` 中增加了坐标容错，消除了部分应用窗口 Transform 异常导致的崩溃。

### 5. 🎨 圆角卡片化设置 UI 与全局弹窗样式重构
- **`CardPreferenceStyler` 设置 UI**：主设置全面重构为精致的白色圆角矩形卡片（18dp 卡片间距、统一图标与水波纹）。
- **`DialogStyleUtils` 全局弹窗重构**：
  - 所有对话框应用 20dp 柔和圆角卡片，底色与文字颜色**完全与键盘选中的主题解耦**，严格跟随设置页面 UI 的日/夜间深浅模式自适应变色。
  - 全局隐藏无用的【取消】按键，点击外部空白或返回键即关闭对话框。
- **胶囊按钮展开框**：主题编辑器顶部的键盘切换按钮，点击向下平滑展开为 16dp 圆角矩形选择框。
- **长按导出 `.yus` 菜单**：取消长按主题的大弹窗，改为卡片旁小巧的 12dp 圆角矩形下拉菜单（含 `导出主题 (.yus)`）。

### 6. ✍️ 手写识别引擎升级 (Paddle/ONNX Handwriting Engine)
- **神经网络手写 OCR**：彻底替换了旧版 JNI C++ 手写库，引入基于 Paddle/ONNX 的神经网络手写识别框架 (`HandwritingRecognizer`)。
- **全架构支持**：补全了 `arm64-v8a`、`armeabi-v7a` (32位)、`x86`、`x86_64` 库文件，解决了 32 位手机和模拟器手写崩溃问题。

### 7. 🎨 主题与预测扩展
- **默认主题扩展**：内置 Pixel Light 与 Pixel Dark 默认精美主题，支持 `.yus` 主题文件一键导入导出。
- **日期时间预测**：扩展了乱序 17、双拼、九键和全键下的日期与时间预测功能（如输入 `rq` / `riqi` / `sj` / `shijian` 实时提示当前日期时间）。

---

## 📂 第二部分：详细代码与资源文件对比表

> [!NOTE]
> 说明：下表列出了 K版与原版（YuyanIme）之间所有不同的代码文件、资源文件和配置文件。未在下表中列出的文件在两版之间完全一致，可直接互相替换。

### 1. 核心业务服务与引擎逻辑文件对比 (Core Logic & Services)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../service/ImeService.kt` | 基础 IME 服务逻辑，物理键盘坐标无 `isNaN()` 校验 | 增加 `isNaN()` 物理键盘坐标防崩溃校验，集成 ASR 语音识别与卡片弹窗事件 |
| `yuyansdk/.../service/DictDecoder.kt` | 基础候选词解码 | 整合 T9/LX17 中英混输匹配 (`T9EnglishPredictor`)，支持 `comment == "🔤"` 英文词及个性化短语选择上屏 |
| `yuyansdk/.../service/InputDispatcher.kt` | 仅支持基础 Rime 参数更新 | 新增 `RimeEngine.setImeOption("echo", cnEnInput)` 中英混输参数同步 |
| `yuyansdk/.../service/T9EnglishPredictor.kt` | 【无此文件】 | **[新增]** 九键 (T9) 与乱序 17 (LX17) 中英混输预测引擎，支持 26 字母多按键变体映射 |
| `yuyansdk/.../service/AsrInferenceClient.kt` | 【无此文件】 | **[新增]** ASR 离线语音识别 AIDL 服务客户端通信类 |
| `yuyansdk/.../service/AsrInferenceService.kt` | 【无此文件】 | **[新增]** ASR 离线语音识别后台推理服务 |
| `yuyansdk/.../service/VoiceRecognitionHandler.kt` | 【无此文件】 | **[新增]** 语音识别长按录入与文本回调句柄 |
| `yuyansdk/.../inputmethod/CustomEngine.kt` | 仅支持日期时间短语查询 | 支持从 Room `PhraseDao` 精确查询 T9 数字 (`34944`)、全键拼音 (`dizhi`) 及 LX17 (`GXXD`) 个性化短语 |
| `yuyansdk/.../inputmethod/RimeEngine.kt` | 基础 Rime 引擎桥接 | 增强多编码组合推导，支持个性化短语和混输词置顶 |
| `yuyansdk/.../inputmethod/HWEngine.kt` | 调用旧版 JNI `HandWriting.init` | 调用全新的 `HandwritingRecognizer` 神经网络手写识别 API |
| `yuyansdk/.../inputmethod/core/HandWriting.kt` | 旧版 JNI 手写 C++ 接口类 | 包装新版 `yuyanwriting` 原生推理句柄 (`nativeCreate`/`nativeRun`/`nativeRelease`) |

### 2. 手写识别与离线语音识别模块文件对比 (Handwriting & Speech Recognition)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../handwriting/HandwritingRecognizer.kt` | 旧版基础封装 | **[重构]** 线程池异步加载 `Predictor` 与 `RecEngine` 模型 |
| `yuyansdk/.../handwriting/internal/Predictor.kt` | 旧版类 | **[重构]** 负责 Paddle/ONNX 手写模型 `rec.nb` 与字典 `ocrv5.txt` 的解算 |
| `yuyansdk/.../handwriting/internal/RecEngine.kt` | 旧版类 | **[重构]** 行切分与逐字 `CtcDecoder` 束搜索合并手写候选词 |
| `yuyansdk/.../handwriting/internal/CtcDecoder.kt` | 旧版解码 | **[重构]** 基于字典束搜索（Beam Search）的 CTC 概率解码器 |
| `yuyansdk/.../handwriting/internal/ImagePreprocessor.kt` | 基础预处理 | **[重构]** 笔画 SmoothPath 渲染与 $48 \times 320$ 归一化图像预处理 |
| `yuyansdk/.../handwriting/internal/StrokeSegmenter.kt` | 基础切分 | **[重构]** 基于空隙与宽高比的多字手写笔画分割器 |
| `yuyansdk/.../speech/AsrSupport.kt` | 【无此文件】 | **[新增]** 离线语音识别模型 Warmup 热身与生命周期管理 |
| `yuyansdk/.../speech/AsrModelManager.kt` | 【无此文件】 | **[新增]** 离线语音模型 Archive 压缩包解压与导入管理 |
| `yuyansdk/.../speech/AsrBackendFactory.kt` | 【无此文件】 | **[新增]** Sherpa-onnx 语音推理 Backend 工厂 |
| `yuyansdk/.../speech/SpeechRecognitionManager.kt` | 【无此文件】 | **[新增]** 语音识别流式音频采集与 JNI 数据推流 |
| `yuyansdk/.../speech/SpectrumAnalyzer.kt` | 【无此文件】 | **[新增]** 实时音频 FFT 频谱分析器（用于波形 UI） |

### 3. UI、适配器、设置与弹窗控件文件对比 (UI, Adapters & Views)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../ui/fragment/ImeSettingsFragment.kt` | 传统列表式设置 UI | **[重构]** 圆角卡片化 UI，新增【方案与数据】分类与【个性化短语】入口，使用 `ic_menu_voice` 话筒图标 |
| `yuyansdk/.../ui/fragment/PhrasesSettingsFragment.kt` | 【无此文件】 | **[新增]** 个性化短语管理页面，支持自动检测当前键盘模式（9键/LX17/全键）与卡片列表管理 |
| `yuyansdk/.../ui/fragment/VoiceSettingsFragment.kt` | 基础语音设置 | **[重构]** 支持 Sherpa-onnx 模型导入、在线下载链接与常驻内存开关 |
| `yuyansdk/.../ui/fragment/theme/CustomThemeActivity.kt` | 系统 PopupMenu 切换模式 | **[重构]** 顶部胶囊按钮点击平滑向下展开 16dp 圆角矩形选择框，弹窗接入 `DialogStyleUtils` |
| `yuyansdk/.../ui/fragment/theme/ThemeListFragment.kt` | 基础主题列表 | **[重构]** 新增主题弹窗带 Emoji 图标，复制主题弹窗修复 `minimumHeight` 溢出并支持 `clipToOutline` |
| `yuyansdk/.../ui/fragment/theme/ThemeThumbnailUi.kt` | 系统 PopupMenu 弹出导出 | **[重构]** 长按主题弹出卡片旁 12dp 圆角矩形 `ListPopupWindow`（仅含导出 .yus），配色解耦键盘主题 |
| `yuyansdk/.../view/widget/KeyCaptureEditText.kt` | 【无此文件】 | **[新增]** 带 `privateImeOptions="com.yuyan.KEY_CAPTURE"` 标识的输入框，直接捕获软键盘物理键值 |
| `yuyansdk/.../view/widget/ImeEditText.kt` | `ImeEditText` 类 | `open class ImeEditText` 允许继承 |
| `yuyansdk/.../utils/DialogStyleUtils.kt` | 【无此文件】 | **[新增]** 全局弹窗 20dp 圆角卡片样式工具类，根据设置 UI 日/夜模式自适应调色，全局隐藏【取消】按键 |
| `yuyansdk/.../utils/CardPreferenceStyler.kt` | 【无此文件】 | **[新增]** 设置页面圆角卡片化统一样式渲染器 |
| `yuyansdk/.../adapter/FloatCandidatesBarAdapter.kt` | 基础悬浮适配器 | 支持数字序号前缀（`1.` `2.` `3.`）与高亮激活状态绑定 |
| `yuyansdk/.../candidate/FloatCandidateBar.kt` | 基础悬浮候选栏 | 支持 DPAD 方向键高亮选词与 DPAD_UP/DOWN 翻页及 `pageStarts` 历史轨迹栈 |
| `yuyansdk/.../adapter/MenuAdapter.kt` | 主线程实时读写 Room DB | 接入 `SkbMenuOrderManager` 内存缓存，消除 UI 绑卡顿 |
| `yuyansdk/.../manager/SkbMenuOrderManager.kt` | 每次查数据库 | **[重构]** 单例内存缓存 + 后台 ThreadPoolUtils 异步数据库落盘 |
| `yuyansdk/.../keyboard/InputView.kt` | 基础按键处理 | 增加 `isKeyCaptureInput` 逻辑，拦截 Pinyin 拼音输入并实现键值直接捕获；支持语音键盘波形容器 |
| `yuyansdk/.../keyboard/container/VoiceKeyboardContainer.kt` | 【无此文件】 | **[新增]** 离线语音输入键盘容器，带实时音频波形可视化与连续删除/上滑清空手势 |

### 4. 数据库、配置偏好与数据模型文件对比 (Database & Preferences)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/.../database/DataBaseKT.kt` | 插入 5 条默认预设短语 | **[优化]** 移除 5 条默认预设短语并在初始化时自动清理旧有预设，保持自定义短语列表纯净 |
| `yuyansdk/.../database/dao/SkbFunDao.kt` | 基础 DAO | 补充 `deleteBarMenu(name: String)` 支持动态增删菜单图标 |
| `yuyansdk/.../database/dao/PhraseDao.kt` | 基础查询 | 支持对 `t9`、`qwerty`、`lx17` 键码字段的排序与精准检索 |
| `yuyansdk/.../database/entry/Phrase.kt` | 包含 `t9/qwerty/lx17` 字段 | 存储自定义短语及其对应的 9键/26键/LX17 键码 |
| `yuyansdk/.../prefs/AppPrefs.kt` | 基础配置 | 新增 `cnEnInput` (关联 `cn_en_input_enable` 混输开关) |
| `yuyansdk/.../data/theme/ThemeArchiveUtils.kt` | 【无此文件】 | **[新增]** `.yus` 主题皮肤压缩包一键导入与导出工具类 |

### 5. 资源文件与构建配置对比 (Resources & Build Config)

| 文件名 (Relative Path) | 原版特性 (YuyanIme-master) | K版特性 (YuyanImeK-master) |
| :--- | :--- | :--- |
| `yuyansdk/src/main/assets/pinyindb/english_words.txt` | 【无此文件】 | **[新增]** Top 6,000 高频英文词库表（用于 9键/LX17 中英混输匹配） |
| `yuyansdk/src/main/assets/hw/ocrv5.txt` | 旧版/无 | **[新增]** Paddle/ONNX 手写识别字典库 |
| `yuyansdk/src/main/assets/hw/rec.nb` | 旧版/无 | **[新增]** Paddle/ONNX 手写识别神经网络模型文件 |
| `yuyansdk/libs/` (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) | 缺少部分架构 `.so` | **[补全]** 包含 `libyuyanime.so` 与 `libyuyanwriting.so` 完整多架构动态库 |
| `yuyansdk/src/main/res/drawable/ic_menu_voice.xml` | 【无此文件】 | **[新增]** 重新绘制的话筒语音识别矢量图标 |
| `yuyansdk/src/main/res/navigation/settings_nav.xml` | 基础设置路由 | 增加了 `phrasesSettingsFragment` 个性化短语导航路由 |
| `yuyansdk/src/main/res/values/strings.xml` | 基础字符串 | 新增 `<string name="cn_en_input">中英混输</string>` 等语言资源 |
| `app/build.gradle` | `versionName "K52"` / `versionCode 52` | `versionName "K53"` / `versionCode 53`，优化 APK 命名与 AAPT 资源压缩规则 |
