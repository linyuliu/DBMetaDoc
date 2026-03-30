<!DOCTYPE html>
<html lang="zh-CN" xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta charset="UTF-8"/>
<title>${view.title}</title>
<#include "_styles.ftl">
</head>
<body>
<div class="document">
  <section class="cover">
    <p class="cover-label">数据库结构文档</p>
    <h1 class="cover-title">${view.title}</h1>
    <p class="cover-subtitle">${view.subtitle}</p>
    <table class="cover-meta">
      <tbody>
        <tr>
          <td class="label">数据库名称</td>
          <td>${view.databaseName!""}</td>
          <td class="label">数据库类型</td>
          <td>${view.type!""}</td>
        </tr>
        <tr>
          <td class="label">Schema</td>
          <td class="mono">${view.schemaName!""}</td>
          <td class="label">Catalog</td>
          <td class="mono">${view.catalogName!""}</td>
        </tr>
        <tr>
          <td class="label">表数量</td>
          <td>${view.tableCount!0}</td>
          <td class="label">生成时间</td>
          <td>${view.generatedAt!""}</td>
        </tr>
      </tbody>
    </table>
  </section>

  <#if view.showDatabaseOverview>
  <section class="section">
    <h2 class="section-title">一、库概览</h2>
    <table class="kv-table stripe">
      <tbody>
        <tr>
          <th>数据库名称</th>
          <td>${view.databaseName!""}</td>
          <th>数据库类型</th>
          <td>${view.type!""}</td>
        </tr>
        <tr>
          <th>Schema</th>
          <td class="mono">${view.schemaName!""}</td>
          <th>Catalog</th>
          <td class="mono">${view.catalogName!""}</td>
        </tr>
        <tr>
          <th>字符集</th>
          <td class="mono">${view.charset!""}</td>
          <th>排序规则</th>
          <td class="mono">${view.collation!""}</td>
        </tr>
        <tr>
          <th>数据库版本</th>
          <td>${view.version!""}</td>
          <th>表数量</th>
          <td>${view.tableCount!0}</td>
        </tr>
      </tbody>
    </table>
  </section>
  </#if>

  <#if view.showTableOverview && view.hasTables>
  <section class="section">
    <h2 class="section-title">二、表概览</h2>
    <table class="stripe">
      <thead>
        <tr>
          <th style="width:12mm;" class="center">序号</th>
          <th style="width:38mm;">表名</th>
          <th style="width:24mm;">Schema</th>
          <th style="width:16mm;" class="center">列数</th>
          <th>注释</th>
        </tr>
      </thead>
      <tbody>
        <#list view.tableOverviewRows![] as table>
        <tr>
          <td class="center">${table.tableNo!0}</td>
          <td class="mono">${table.name!""}</td>
          <td class="mono">${table.schema!""}</td>
          <td class="center">${table.columnCount!0}</td>
          <td>${table.comment!""}</td>
        </tr>
        </#list>
      </tbody>
    </table>
  </section>
  </#if>

  <#if view.hasTables>
    <#list view.tables![] as table>
    <section class="section chapter">
      <h2 class="section-title">三.${table.tableNo!0} ${table.name!""}</h2>
      <div class="comment-box">${table.comment!""}</div>

      <#if table.showTableOverview>
      <h3 class="sub-title">表基本属性</h3>
      <table class="kv-table stripe">
        <tbody>
          <tr>
            <th>Schema</th>
            <td class="mono">${table.schema!""}</td>
            <th>主键</th>
            <td class="mono">${table.primaryKey!""}</td>
          </tr>
          <tr>
            <th>引擎</th>
            <td>${table.engine!""}</td>
            <th>字符集</th>
            <td class="mono">${table.charset!""}</td>
          </tr>
          <tr>
            <th>排序规则</th>
            <td class="mono">${table.collation!""}</td>
            <th>行格式</th>
            <td>${table.rowFormat!""}</td>
          </tr>
          <tr>
            <th>表类型</th>
            <td>${table.tableType!""}</td>
            <th>字段数量</th>
            <td>${table.columnCount!0}</td>
          </tr>
        </tbody>
      </table>
      </#if>

      <#if table.hasBasicColumns>
      <h3 class="sub-title">核心字段清单</h3>
      <p class="section-note">主字段表固定保留 6 列核心字段，适合中文 A4 文档阅读和打印。</p>
      <table class="stripe">
        <thead>
          <tr>
            <th style="width:28mm;">字段名</th>
            <th style="width:26mm;">类型</th>
            <th style="width:12mm;" class="center">主键</th>
            <th style="width:12mm;" class="center">可空</th>
            <th style="width:24mm;">默认值</th>
            <th>注释</th>
          </tr>
        </thead>
        <tbody>
          <#list table.columns![] as column>
          <tr>
            <td class="mono">${column.name!""}</td>
            <td class="mono">${column.type!""}</td>
            <td class="center">${column.primaryKeyText!""}</td>
            <td class="center">${column.nullableText!""}</td>
            <td>${column.defaultValue!""}</td>
            <td>${column.comment!""}</td>
          </tr>
          </#list>
        </tbody>
      </table>
      </#if>

      <#if table.hasExtendedColumns>
      <h3 class="sub-title">字段扩展补充</h3>
      <p class="section-note">扩展信息下沉为补充区，不再额外扩宽主字段表。</p>
      <table class="stripe">
        <thead>
          <tr>
            <th style="width:12mm;" class="center">序号</th>
            <th style="width:28mm;">字段名</th>
            <th style="width:32mm;">原始类型</th>
            <th style="width:24mm;">Java 类型</th>
            <th>扩展说明</th>
          </tr>
        </thead>
        <tbody>
          <#list table.extendedColumns![] as column>
          <tr>
            <td class="center">${column.orderNo!0}</td>
            <td class="mono">${column.name!""}</td>
            <td class="mono">${column.rawType!""}</td>
            <td class="mono">${column.javaType!""}</td>
            <td>${column.extendedSummary!""}</td>
          </tr>
          </#list>
        </tbody>
      </table>
      </#if>

      <#if table.hasIndexes>
      <h3 class="sub-title">索引信息</h3>
      <table class="stripe">
        <thead>
          <tr>
            <th style="width:34mm;">索引名</th>
            <th>包含字段</th>
            <th style="width:14mm;" class="center">唯一</th>
            <th style="width:24mm;">类型</th>
          </tr>
        </thead>
        <tbody>
          <#list table.indexes![] as index>
          <tr>
            <td class="mono">${index.name!""}</td>
            <td>${index.columnNamesText!""}</td>
            <td class="center">${index.uniqueText!""}</td>
            <td>${index.type!""}</td>
          </tr>
          </#list>
        </tbody>
      </table>
      </#if>

      <#if table.hasForeignKeys>
      <h3 class="sub-title">外键信息</h3>
      <table class="stripe">
        <thead>
          <tr>
            <th style="width:34mm;">外键名</th>
            <th style="width:24mm;">本表字段</th>
            <th>引用表</th>
            <th style="width:24mm;">引用字段</th>
          </tr>
        </thead>
        <tbody>
          <#list table.foreignKeys![] as foreignKey>
          <tr>
            <td class="mono">${foreignKey.name!""}</td>
            <td class="mono">${foreignKey.columnName!""}</td>
            <td class="mono">${foreignKey.referencedTable!""}</td>
            <td class="mono">${foreignKey.referencedColumn!""}</td>
          </tr>
          </#list>
        </tbody>
      </table>
      </#if>
    </section>
    </#list>
  <#else>
  <div class="empty-box">当前未查询到可导出的表结构。</div>
  </#if>
</div>
</body>
</html>
