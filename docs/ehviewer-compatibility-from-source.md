# EHViewer 交互兼容清单（基于源码）

## 1. 参考源码位置
- 源码根目录：`F:\codx\_clean_Ehviewer_CN_SXJ`
- Android 主模块：`F:\codx\_clean_Ehviewer_CN_SXJ\app`

本清单只提炼交互行为与产品规则，不直接复用旧实现代码。

## 2. 核心页面入口（证据）
- 启动与主入口：`SplashActivity`、`MainActivity`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\AndroidManifest.xml`
- 阅读器：`GalleryActivity`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\GalleryActivity.java`
- 列表与搜索：`GalleryListScene`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\scene\gallery\list\GalleryListScene.java`
- 详情：`GalleryDetailScene`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\scene\gallery\detail\GalleryDetailScene.java`
- 历史：`HistoryScene`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\scene\history\HistoryScene.java`
- 设置阅读项：`ReadFragment`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\fragment\ReadFragment.java`
- 设置下载项：`DownloadFragment`
  - `F:\codx\_clean_Ehviewer_CN_SXJ\app\src\main\java\com\hippo\ehviewer\ui\fragment\DownloadFragment.java`

## 3. 必须保留的 EH 交互行为（首发）

### 3.1 列表页
- 点击条目进入详情，带转场元素（封面共享元素）。
  - 证据：`GalleryListScene.java:1272-1275`
- 长按条目触发操作菜单。
  - 证据：`GalleryListScene.java:1280-1285`
- 支持标签筛选开关与叠加筛选。
  - 证据：`GalleryListScene.java:844-847`
- 支持快速搜索（Quick Search）入口与引导。
  - 证据：`GalleryListScene.java:953`、`GalleryListScene.java:886-902`
- 返回键优先关闭弹层/FAB 展开态，而非直接退出页面。
  - 证据：`GalleryListScene.java:1204-1212`

### 3.2 详情页
- 标签支持上下文操作（长按标签弹出操作）。
  - 证据：`GalleryDetailScene.java:1653-1661`
- 详情页提供筛选入口（按上传者/标签过滤）。
  - 证据：`GalleryDetailScene.java:1608-1623`
- 预览网格显示页码并支持从预览跳转阅读。
  - 证据：`GalleryDetailScene.java:1185`、`GalleryDetailScene.java:1210`

### 3.3 阅读器
- 阅读器支持阅读方向、缩放模式、起始位置、起始页等参数化配置。
  - 证据：`GalleryActivity.java:374`
- 返回时回传画廊信息，保证上一级状态可刷新。
  - 证据：`GalleryActivity.java:530-534`
- 支持页面操作菜单（刷新/分享/保存/另存）。
  - 证据：`GalleryActivity.java:1106-1113`
- 支持滚轮翻页/滑动输入响应（外设输入兼容）。
  - 证据：`GalleryActivity.java:694-704`
- 阅读进度通过 `KEY_PAGE` 等参数保存与恢复。
  - 证据：`GalleryActivity.java:125`、`GalleryActivity.java:296-319`

### 3.4 历史与收藏风格
- 历史支持点击进入详情，长按弹出操作。
  - 证据：`HistoryScene.java:256`、`HistoryScene.java:274-282`
- 历史支持“清空全部”确认对话框。
  - 证据：`HistoryScene.java:219-226`
- 历史列表具备空态切换（内容/提示切换）。
  - 证据：`HistoryScene.java:188`（`updateLazyList` 注释）与 `updateView` 逻辑

### 3.5 设置中心
- 阅读设置、下载设置分区存在且可直接修改行为参数。
  - 证据：`ReadFragment.java`、`DownloadFragment.java`
- 下载设置包含目录选择、多线程、超时、预加载、导入导出等。
  - 证据：`DownloadFragment.java:85-105`、`DownloadFragment.java:147-205`

## 4. 映射到 NHViewer 新架构（data/domain/ui）

### 4.1 ui 层
- `feature-list`：首页/热门/搜索/标签筛选/快速搜索/长按菜单/返回行为。
- `feature-detail`：详情信息、标签操作、预览网格、从预览跳阅读器。
- `feature-reader`：翻页、缩放、方向、进度、页面菜单。
- `feature-history`：历史列表、长按菜单、清空确认、续读入口。
- `feature-settings`：阅读和下载子页，参数即时生效。

### 4.2 domain 层
- `ListUseCase`、`SearchUseCase`、`FilterByTagUseCase`
- `GetGalleryDetailUseCase`、`GetPreviewPagesUseCase`
- `ReadProgressUseCase`（保存/恢复）
- `HistoryUseCase`、`FavoriteUseCase`
- `SettingsUseCase`（读取/更新）

### 4.3 data 层
- 远端全部走 nhentai API v2（见 `docs/nhentai-api-integration.md`）。
- 本地用 Room + DataStore 实现 EH 风格行为记忆。

## 5. 迁移约束
- 目标是“行为兼容”，不是“代码复用”。
- 新实现必须在每个 feature 给出对应行为验收点：
  - 点击/长按行为
  - 返回键行为
  - 筛选与快速操作行为
  - 阅读进度保存与恢复
  - 空态/错误态/加载态展示
