# DBMetaDoc - 数据库文档生成工具

本项目的目标是提供一个简单、高效的工具，能够自动生成数据库结构文档。该工具支持 MySQL、PostgreSQL 及国产数据库（如人大金仓）的元数据提取，并支持生成多种格式的文档，包括 PDF、HTML、Markdown 和 Word（docx）。文档样式可定制，用户可以根据需求调整字体、排版和样式，确保文档美观且稳定。

## 功能特性

- 多数据库支持：MySQL、PostgreSQL、Oracle、人大金仓（Kingbase）、达梦
- 多格式输出：HTML、Markdown、PDF、Word（docx）
- 单体部署：前端构建产物直接打进 Spring Boot 应用
- REST API：统一 `R<T>` 返回，导出接口返回文件流
- 数据源模板：保存前强制连接测试，不持久化密码
- 内置开源字体兜底：`Source Han Sans`、`Noto Sans SC`、`JetBrains Mono`
- 本机品牌字体优先：支持 `SF Pro`、`PingFang SC`、`MiSans`、`HarmonyOS Sans SC` 作为可选回退

## 项目结构

```
DBMetaDoc/
├── pom.xml                    # 父 POM（多模块）
├── dbmetadoc-common/          # 公共模型（TableInfo, ColumnInfo 等）
├── dbmetadoc-db/              # 数据库能力聚合目录
│   ├── dbmetadoc-db-core/     # SPI、连接信息、通用抽取支持
│   ├── dbmetadoc-db-mysql/    # MySQL 实现
│   ├── dbmetadoc-db-postgresql/ # PostgreSQL 实现
│   ├── dbmetadoc-db-oracle/   # Oracle 实现
│   ├── dbmetadoc-db-kingbase/ # Kingbase 实现
│   ├── dbmetadoc-db-dameng/   # 达梦实现
│   └── dbmetadoc-db-bundle/   # 供 app 引入的聚合实现
│   └── lib/
│       └── kingbase8-8.6.0.jar  # KingBase 驱动存根（替换为真实驱动）
├── dbmetadoc-generator/       # 文档生成（HTML/MD/PDF/Word）
│   └── src/main/resources/templates/
│       ├── html/database.ftl  # HTML 模板
│       └── markdown/database.ftl  # Markdown 模板
├── dbmetadoc-app/             # Spring Boot 应用 + REST API
├── dbmetadoc-web/             # Vue 3 + Vite 前端
├── scripts/build/             # 构建脚本（处理 Windows UTF-8 代码页）
└── .github/workflows/         # CI/CD 工作流
```

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.12 + JDK 17 |
| 模板引擎 | FreeMarker 2.3.34 |
| PDF 生成 | Flying Saucer 9.4.0 + OpenPDF |
| Word 生成 | Apache POI 5.5.1 |
| 数据库驱动 | mysql-connector-j 8.4.0、PostgreSQL 42.7.10 |
| 前端框架 | Vue 3 + Vite + TypeScript + Element Plus |
| 构建工具 | Maven |

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+（可选，仅前端开发需要）

## 字体说明

- 仓库内只内置可再分发字体，具体文件与许可证见 `THIRD_PARTY_FONTS.md`
- Apple SF / PingFang、MiSans、HarmonyOS Sans 不会随仓库分发，可放入 `dbmetadoc-local-fonts/` 供本机导出优先使用
- 运行时也可以通过环境变量 `DBMETADOC_FONT_DIRECTORIES` 追加自定义字体目录，多个目录用逗号分隔
