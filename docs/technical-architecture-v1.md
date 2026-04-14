# 技术架构 v1（当前实现基线）

## 1. 架构目标
- 保持 `data / domain / ui` 单向依赖。
- 避免 Activity 直接访问数据层实现。
- 所有页面状态统一为 `LoadState`（Loading/Content/Empty/Error）。

## 2. 分层说明

```text
app/
  src/main/java/com/nhviewer/
    ui/          # Activity, Adapter, ViewModel
    domain/      # model, repository interface, usecase
    data/        # remote/local/repository impl
    core/        # network/common
    app/         # AppGraph, Application, Theme
```

## 3. 依赖方向
- `ui -> domain`
- `data -> domain`
- `core` 被 `ui/data` 共用
- `domain` 不依赖 Android Framework

## 4. 网络层
- 技术栈：`Retrofit + OkHttp + Kotlinx Serialization`
- 统一结果：`ApiResult.Success/HttpError/NetworkError/UnknownError`
- 统一请求头：
  - `User-Agent`
  - `Authorization: Key <apiKey>`（由设置注入）

## 5. 数据层
- Room：
  - `favorites`
  - `history`
  - `reading_progress`
- DataStore：
  - 主题、语言、并发、首页筛选
  - API Key
  - blacklisted 隐藏开关
  - 收藏源（local/online）

## 6. 状态管理
- ViewModel 使用 `StateFlow` 暴露 UI 状态。
- 页面只订阅 ViewModel，不直接访问 Repository。
- 设置流由 ViewModel 暴露给 Activity（收口 UI 依赖）。

## 7. 当前已知技术债
1. 依赖注入仍是 `AppGraph`（Service Locator），未迁移到 Hilt/Koin。
2. 下载能力已移出后续计划，但代码仍保留（仅历史兼容）。
3. UI 层仍有少量业务逻辑可继续下沉（逐步治理）。

## 8. 下一步架构演进
1. 引入 DI 容器，替换 `NhViewModelFactory + AppGraph` 组合。
2. Repository 异常模型标准化（错误码 + 语义化提示）。
3. 构建覆盖率门槛与 CI 质量闸门。
