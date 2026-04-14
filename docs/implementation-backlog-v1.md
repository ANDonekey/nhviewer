# 实施 Backlog v1

> 注：本文件为历史版本。最新计划请使用 docs/implementation-plan-v2.md。

## 0. 基线准备（Day 0）
- [x] 初始化模块骨架（ui/domain/data/core）
- [ ] 接入依赖注入（Hilt/Koin 二选一）
- [x] 建立统一错误模型与结果封装

## 1. 产品范围定义（Day 1-2）
- [x] PRD v1
- [x] 信息架构与页面流
- [x] 低保真线框
- [ ] 评审并冻结首发范围

## 2. 技术架构重建（Day 3-5）
- [x] 网络层封装（Retrofit/OkHttp/Kotlinx Serialization）
- [x] nhentai API 数据模型与映射
- [x] Room schema（收藏/历史/下载/进度）
- [x] DataStore 设置项与默认值
- [x] 基础 Repository 接口与实现骨架

## 3. 第一阶段能力（Week 2-3）
- [x] 首页/热门列表（基础列表页面 + 状态展示已接线）
- [x] 搜索 + 分页 + 排序 + 标签筛选（关键词 + 标签关键词筛选 + 翻页已接线）
- [x] 详情页 + 预览（已接线：首页跳转详情 + 预览网格）
- [x] 阅读器（pinch缩放/双击/平移/方向切换/预加载增强 已完成）
- [x] 收藏与历史（Room 持久化已接线）
- [x] 设置中心（DataStore 基础项已接线）

## 4. 第二阶段体验对齐（Week 3-4）
- [x] 长按菜单（画廊列表长按弹出收藏/打开菜单）
- [ ] 批量操作
- [x] 快速筛选（首页内联 ChipGroup：语言+排序）
- [x] 返回位置记忆（LinearLayoutManager scrollToPositionWithOffset）
- [ ] 性能专项（滚动、缓存、预取、低端机策略）

## 5. 下载与文件系统（并行）
- [ ] （已移出后续计划）下载功能不再纳入迭代范围
- [x] 下载队列与状态机
- [x] 暂停/恢复/失败重试
- [x] 通知栏进度与前台服务（DownloadService + NotificationChannel）
- [x] 去重策略（同 galleryId 已 QUEUED/RUNNING/PAUSED 时不重新入队）
- [ ] 媒体扫描（MediaScannerConnection）

## 6. 质量与发布（Week 4）
- [ ] API 单测
- [ ] Repository 单测
- [ ] 关键 UI 流程测试
- [ ] 崩溃与日志埋点
- [ ] CI/CD（Debug/Release/签名/渠道）
- [ ] 灰度回归清单

## 7. DoD（完成定义）
- [ ] 可编译
- [ ] 可运行
- [ ] 可回归
- [ ] 有基础测试
- [ ] 文档同步更新

