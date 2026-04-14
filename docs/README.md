# NHViewer 文档索引（v2）

## 核心文档
- `PRD_v1.md`：产品目标、范围、验收标准。
- `information-architecture-and-user-flow.md`：信息架构与页面流。
- `low-fidelity-wireframes.md`：低保真线框草图说明。
- `technical-architecture-v1.md`：当前技术架构与分层约束。
- `nhentai-api-integration.md`：nhentai API 映射与接入规范。

## 计划与执行
- `implementation-backlog-v1.md`：历史 backlog（仅作参考）。
- `implementation-plan-v2.md`：当前执行计划（唯一有效版本）。
- `release-regression-checklist-v1.md`：首发回归清单。
- `test-coverage-matrix-v1.md`：测试覆盖矩阵（功能 -> 自动化测试）。

## 外部快照
- `external/README.md`：第三方文档快照目录说明。
- `external/nhentai-api-v2-openapi.json`：OpenAPI 快照。
- `external/nhentai-api-v2-docs.html`：文档页面快照。

## 维护约定
1. 新功能必须同步更新 `implementation-plan-v2.md` 和回归清单。
2. 新增测试必须更新 `test-coverage-matrix-v1.md`。
3. 文档统一 UTF-8 编码，不使用系统默认编码。
