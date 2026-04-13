# 技术架构 v1

## 1. 架构目标
- 高内聚低耦合，页面与数据源解耦。
- 可替换旧模块，支持渐进迁移。
- 统一状态与错误模型，减少页面重复逻辑。

## 2. 分层结构

```text
app/
  ui/
    feature-home/
    feature-search/
    feature-detail/
    feature-reader/
    feature-favorites/
    feature-history/
    feature-settings/
  domain/
    model/
    repository/
    usecase/
  data/
    remote/
    local/
    repository-impl/
  core/
    network/
    database/
    datastore/
    common/
```

## 3. 依赖方向
- `ui -> domain`
- `domain` 不依赖 Android Framework 与具体库实现
- `data -> domain`（实现 domain repository 接口）
- `core` 被 `data/ui` 引用，不反向依赖业务层

## 4. 网络层
- `Retrofit + OkHttp + Kotlinx Serialization`
- 统一 `ApiResult<T>`：`Success` / `HttpError` / `NetworkError` / `UnknownError`
- 统一拦截器：UA、日志、重试（指数退避，有限次数）
- API 封装：`NhentaiService + NhentaiRemoteDataSource`

## 5. API 优先约束（新增）
- 线上功能数据统一来自 `nhentai API v2`，不走页面抓取。
- OpenAPI 快照存放：`docs/external/nhentai-api-v2-openapi.json`
- 文档页快照存放：`docs/external/nhentai-api-v2-docs.html`
- 刷新脚本：`scripts/fetch-nhentai-api-docs.ps1`

## 6. 本地存储
- `Room`
- 表：`favorites`、`history`、`download_tasks`、`reading_progress`
- `DataStore`
- 键：`image_quality`、`cache_policy`、`max_concurrency`、`theme_mode`、`language`

## 7. 状态管理
- 每个 feature 提供 `ViewModel` + `UiState(StateFlow)` + `UiEvent`
- 标准状态：`Loading`、`Content`、`Empty`、`Error`
- 单次事件：`SharedFlow`（Toast、导航、弹窗）

## 8. 关键接口（示例）

```kotlin
interface GalleryRepository {
    suspend fun getPopular(page: Int): Result<Page<Gallery>>
    suspend fun search(query: String, page: Int, sort: Sort, tags: List<Tag>): Result<Page<Gallery>>
    suspend fun getDetail(galleryId: String): Result<GalleryDetail>
}

interface ReaderRepository {
    suspend fun getPageImages(galleryId: String): Result<List<PageImage>>
    suspend fun saveProgress(galleryId: String, page: Int)
    suspend fun getProgress(galleryId: String): Int?
}
```

## 9. 渐进替换策略
- 通过路由开关控制新旧页面切换。
- 新模块接入后先灰度到内部渠道。
- 每替换一页必须补充最小回归用例后才继续下一页。

## 10. 最小测试矩阵
- Data：API 解析、错误映射、Repository 合并策略。
- Domain：UseCase 参数校验与组合逻辑。
- UI：关键流程（搜索 -> 详情 -> 阅读 -> 续读）与空/错态展示。
