# DBMetaDoc - 数据库文档生成工具

本项目的目标是提供一个简单、高效的工具，能够自动生成数据库结构文档。该工具支持 MySQL、PostgreSQL 及国产数据库（如人大金仓）的元数据提取，并支持生成多种格式的文档，包括 PDF、HTML、Markdown 和 Word（docx）。文档样式可定制，用户可以根据需求调整字体、排版和样式，确保文档美观且稳定。

## 功能特性

- 🗄️ **多数据库支持**：MySQL 5.x/8.x、PostgreSQL、人大金仓（KingBase）
- 📄 **多格式输出**：HTML、Markdown、PDF、Word（docx）
- 🎨 **样式可定制**：基于 FreeMarker 模板，支持自定义样式
- 🌐 **前端界面**：Vue 3 + Element Plus，支持在线预览和文档下载
- 🔌 **REST API**：Spring Boot 3 提供标准 HTTP 接口

## 项目结构

```
DBMetaDoc/
├── pom.xml                    # 父 POM（多模块）
├── dbmetadoc-common/          # 公共模型（TableInfo, ColumnInfo 等）
├── dbmetadoc-db/              # 数据库元数据提取
│   └── lib/
│       └── kingbase8-8.6.0.jar  # KingBase 驱动存根（替换为真实驱动）
├── dbmetadoc-generator/       # 文档生成（HTML/MD/PDF/Word）
│   └── src/main/resources/templates/
│       ├── html/database.ftl  # HTML 模板
│       └── markdown/database.ftl  # Markdown 模板
├── dbmetadoc-web/             # Spring Boot Web 应用 + REST API
└── dbmetadoc-frontend/        # Vue 3 + Vite 前端
```

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.3 + JDK 17 |
| 模板引擎 | FreeMarker 2.3.32 |
| PDF 生成 | Flying Saucer + OpenPDF |
| Word 生成 | Apache POI 5.2.5 |
| 数据库驱动 | mysql-connector-j 8.3.0、PostgreSQL 42.7.2 |
| 前端框架 | Vue 3 + Vite + Element Plus |
| 构建工具 | Maven |

## 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+（可选，仅前端开发需要）

### 编译后端

```bash
mvn compile -pl dbmetadoc-common,dbmetadoc-db,dbmetadoc-generator,dbmetadoc-web -am
```

### 打包运行

```bash
cd dbmetadoc-web
mvn spring-boot:run
```

服务启动后访问：`http://localhost:8080`

### 前端开发

```bash
cd dbmetadoc-frontend
npm install
npm run dev
```

前端开发服务器启动后访问：`http://localhost:5173`

### KingBase（人大金仓）支持

由于 KingBase JDBC 驱动不在 Maven Central，需手动安装：

```bash
mvn install:install-file \
  -Dfile=/path/to/kingbase8-real.jar \
  -DgroupId=com.kingbase8 \
  -DartifactId=kingbase8 \
  -Dversion=8.6.0 \
  -Dpackaging=jar
```

然后将 `dbmetadoc-db/lib/kingbase8-8.6.0.jar` 替换为真实驱动文件。

## API 接口

### 生成文档

```
POST /api/document/generate
Content-Type: application/json

{
  "dbType": "MYSQL",      // MYSQL | POSTGRESQL | KINGBASE
  "host": "localhost",
  "port": 3306,
  "database": "mydb",
  "username": "root",
  "password": "password",
  "format": "HTML",        // HTML | MARKDOWN | PDF | WORD
  "title": "数据库设计文档"
}
```

### 预览文档（HTML）

```
POST /api/document/preview
Content-Type: application/json
// 同上，返回 HTML 字符串
```

### 健康检查

```
GET /api/document/health
```

## 自定义模板

HTML 和 Markdown 模板位于 `dbmetadoc-generator/src/main/resources/templates/` 目录下，可直接修改 FreeMarker 模板文件（`.ftl`）来定制输出样式。

## 后续计划

- 支持 Oracle、SQL Server 等更多数据库
- 支持 Excel 格式输出
- 用户自定义模板上传
- ER 图自动生成
- 数据库连接配置持久化
