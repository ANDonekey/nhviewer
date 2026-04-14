# nhentai API 集成说明（v2）

## 1. 文档来源
- 在线文档：`https://nhentai.net/api/v2/docs#/`
- OpenAPI：`https://nhentai.net/api/v2/openapi.json`

项目内快照：
- `docs/external/nhentai-api-v2-openapi.json`
- `docs/external/nhentai-api-v2-docs.html`

## 2. 认证方式
- API Key：`Authorization: Key YOUR_API_KEY`
- 请求头由网络层统一注入，不在页面层拼装。

## 3. 当前使用的主要接口

### 3.1 画廊列表与搜索
- `GET /api/v2/galleries`：首页最近列表
- `GET /api/v2/galleries/popular`：热门列表
- `GET /api/v2/search`：关键词搜索
- `GET /api/v2/galleries/tagged`：按 tag 过滤

### 3.2 详情与评论
- `GET /api/v2/galleries/{gallery_id}`：详情
- `GET /api/v2/galleries/{gallery_id}/comments`：评论

### 3.3 标签
- `POST /api/v2/tags/search`：标签搜索建议
- `GET /api/v2/tags/ids`：根据 `tag_ids` 批量查标签（用于列表语言展示）

### 3.4 收藏与用户
- `GET /api/v2/favorites`：在线收藏列表
- `GET/POST/DELETE /api/v2/galleries/{gallery_id}/favorite`：收藏状态与增删
- `GET /api/v2/user`：当前用户
- `GET /api/v2/users/{user_id}/{slug}`：用户公开资料

## 4. 代码映射
- Service：`data/remote/NhentaiService.kt`
- RemoteDataSource：`data/remote/NhentaiRemoteDataSource.kt`
- Repository：`data/repository/GalleryRepositoryImpl.kt`
- DTO：`data/remote/dto/NhentaiDtos.kt`
- 映射：`data/mapper/GalleryMapper.kt`

## 5. 错误处理约定
- 网络层统一转换为 `ApiResult`。
- Repository 转为 `Result<T>` 返回上层。
- UI 层统一通过 `ErrorText` 输出用户可读提示（401/429/网络错误等）。

## 6. 开发约束
1. 禁止在 Activity/Fragment 直接请求 API。
2. 禁止在 UI 层解析 DTO。
3. 新增接口时必须同步更新本文件和测试覆盖矩阵。
