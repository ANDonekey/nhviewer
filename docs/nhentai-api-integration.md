# nhentai API 集成说明（v2）

## 1. 文档来源与项目内存储
- Swagger 文档页：`https://nhentai.net/api/v2/docs#/`
- OpenAPI 描述：`https://nhentai.net/api/v2/openapi.json`

已落地文件：
- `docs/external/nhentai-api-v2-docs.html`
- `docs/external/nhentai-api-v2-openapi.json`
- `docs/external/nhentai-api-v2-snapshot.md`（可通过脚本更新）

## 2. 功能到 API 映射（首发）

| 功能 | API | 方法 | 备注 |
|---|---|---|---|
| 首页/热门 | `/api/v2/galleries/popular` | `GET` | 热门列表 |
| 首页/最新（可选） | `/api/v2/galleries` | `GET` | 分页参数 `page`,`per_page` |
| 搜索 | `/api/v2/search` | `GET` | 参数 `query`,`sort`,`page` |
| 标签筛选 | `/api/v2/tags/search` | `POST` | 标签搜索建议 |
| 标签筛选结果 | `/api/v2/galleries/tagged` | `GET` | 参数 `tag_id`,`sort`,`page`,`per_page` |
| 详情 | `/api/v2/galleries/{gallery_id}` | `GET` | 参数 `include` 可扩展 |
| 相关推荐 | `/api/v2/galleries/{gallery_id}/related` | `GET` | 详情页补充 |
| 收藏列表 | `/api/v2/favorites` | `GET` | 本地收藏可与远端同步 |
| 收藏状态 | `/api/v2/galleries/{gallery_id}/favorite` | `GET` | 检查是否已收藏 |
| 收藏增删 | `/api/v2/galleries/{gallery_id}/favorite` | `POST/DELETE` | 与本地 Room 双写策略 |
| 配置 | `/api/v2/config` | `GET` | 全局配置 |
| CDN 配置 | `/api/v2/cdn` | `GET` | 图片地址构建可用 |

## 3. Retrofit 契约草案

```kotlin
interface NhentaiService {
    @GET("/api/v2/galleries/popular")
    suspend fun getPopular(): PopularResponse

    @GET("/api/v2/galleries")
    suspend fun getGalleries(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): GalleryPageResponse

    @GET("/api/v2/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("sort") sort: String?,
        @Query("page") page: Int
    ): GalleryPageResponse

    @GET("/api/v2/galleries/tagged")
    suspend fun getTaggedGalleries(
        @Query("tag_id") tagId: Long,
        @Query("sort") sort: String?,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): GalleryPageResponse

    @GET("/api/v2/galleries/{gallery_id}")
    suspend fun getGalleryDetail(
        @Path("gallery_id") galleryId: Long,
        @Query("include") include: String? = null
    ): GalleryDetailResponse
}
```

## 4. 约束与实现策略
- 所有在线数据获取统一走 `NhentaiService`，禁止在 UI 层拼接 URL 直连。
- `data/remote` 层只暴露 DTO，`data/repository-impl` 完成 DTO -> Domain 映射。
- `favorites/history/download/progress` 本地状态由 Room 托管，远端接口作为同步来源。
- 请求失败统一映射到 `ApiResult`，供 UI 显示空态/错误态/重试。
