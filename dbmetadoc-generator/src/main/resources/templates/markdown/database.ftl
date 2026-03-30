# ${view.title}

${view.subtitle}

生成时间：${view.generatedAt!""}

<#if view.showDatabaseOverview>
## 一、库概览

| 项目 | 内容 |
| --- | --- |
| 数据库名称 | ${view.databaseName!""} |
| 数据库类型 | ${view.type!""} |
| Schema | ${view.schemaName!""} |
| Catalog | ${view.catalogName!""} |
| 字符集 | ${view.charset!""} |
| 排序规则 | ${view.collation!""} |
| 版本 | ${view.version!""} |
| 表数量 | ${view.tableCount!0} |

</#if>
<#if view.showTableOverview && view.hasTables>
## 二、表概览

| 序号 | 表名 | Schema | 列数 | 注释 |
| --- | --- | --- | --- | --- |
<#list view.tableOverviewRows![] as table>
| ${table.tableNo!0} | ${table.name!""} | ${table.schema!""} | ${table.columnCount!0} | ${table.comment!""} |
</#list>

</#if>
<#if view.hasTables>
<#list view.tables![] as table>
## 三.${table.tableNo!0} ${table.name!""}

> ${table.comment!""}

<#if table.showTableOverview>
### 表基本属性

| 表属性 | 内容 | 表属性 | 内容 |
| --- | --- | --- | --- |
| Schema | ${table.schema!""} | 主键 | ${table.primaryKey!""} |
| 引擎 | ${table.engine!""} | 字符集 | ${table.charset!""} |
| 排序规则 | ${table.collation!""} | 行格式 | ${table.rowFormat!""} |
| 表类型 | ${table.tableType!""} | 字段数量 | ${table.columnCount!0} |

</#if>
<#if table.hasBasicColumns>
### 核心字段清单

主字段表固定保留 6 列核心字段，适合中文 A4 文档阅读和打印。

| 字段名 | 类型 | 主键 | 可空 | 默认值 | 注释 |
| --- | --- | --- | --- | --- | --- |
<#list table.columns![] as column>
| ${column.name!""} | ${column.type!""} | ${column.primaryKeyText!""} | ${column.nullableText!""} | ${column.defaultValue!""} | ${column.comment!""} |
</#list>

</#if>
<#if table.hasExtendedColumns>
### 字段扩展补充

扩展信息下沉为补充区，不再额外扩宽主字段表。

| 序号 | 字段名 | 原始类型 | Java 类型 | 扩展说明 |
| --- | --- | --- | --- | --- |
<#list table.extendedColumns![] as column>
| ${column.orderNo!0} | ${column.name!""} | ${column.rawType!""} | ${column.javaType!""} | ${column.extendedSummary!""} |
</#list>

</#if>
<#if table.hasIndexes>
### 索引信息

| 索引名 | 包含字段 | 唯一 | 类型 |
| --- | --- | --- | --- |
<#list table.indexes![] as index>
| ${index.name!""} | ${index.columnNamesText!""} | ${index.uniqueText!""} | ${index.type!""} |
</#list>

</#if>
<#if table.hasForeignKeys>
### 外键信息

| 外键名 | 本表字段 | 引用表 | 引用字段 |
| --- | --- | --- | --- |
<#list table.foreignKeys![] as foreignKey>
| ${foreignKey.name!""} | ${foreignKey.columnName!""} | ${foreignKey.referencedTable!""} | ${foreignKey.referencedColumn!""} |
</#list>

</#if>
---

</#list>
<#else>
当前未查询到可导出的表结构。
</#if>
