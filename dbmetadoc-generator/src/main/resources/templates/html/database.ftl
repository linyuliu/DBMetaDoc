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
    <h1 class="cover-title">${view.databaseName!view.title}</h1>
    <p class="cover-subtitle">共 ${view.tableCount!0} 张表</p>
  </section>

  <div class="screen-only-block screen-head">
    <div>
      <p class="screen-kicker">表结构</p>
      <h1>${view.databaseName!view.title}</h1>
      <p>共 ${view.tableCount!0} 张表</p>
    </div>
  </div>

  <main class="content-pane">
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
              <td>${view.schemaName!""}</td>
              <th>Catalog</th>
              <td>${view.catalogName!""}</td>
            </tr>
            <tr>
              <th>字符集</th>
              <td>${view.charset!""}</td>
              <th>排序规则</th>
              <td>${view.collation!""}</td>
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

      <#if view.hasTables>
        <#list view.tables![] as table>
        <section class="section chapter" id="table-${table.tableNo!0}">
          <div class="section-head chapter-head">
            <h2 class="section-title table-section-title">${table.chapterTitle!""}</h2>
          </div>
          <#if table.comment?has_content>
          <p class="table-comment-line">${table.comment!""}</p>
          </#if>

          <#if table.hasBasicColumns>
          <h3 class="sub-title">字段清单</h3>
          <table class="stripe grid-table">
            <colgroup>
              <#list table.basicColumnLayout.columns![] as layoutColumn>
              <col style="width:${layoutColumn.htmlWidthPercent!''};" />
              </#list>
            </colgroup>
            <thead>
              <tr>
                <th class="center head-short head-nowrap narrow-head">序号</th>
                <th>列名</th>
                <th>数据类型</th>
                <th class="center head-short head-nowrap narrow-head">主键</th>
                <th class="center head-short head-nowrap narrow-head">可空</th>
                <th>默认值</th>
                <th>列说明</th>
              </tr>
            </thead>
            <tbody>
              <#list table.columns![] as column>
              <tr class="${table.basicColumnLayout.rowClasses[column?index]!''}">
                <td class="center cell-strong order-cell nowrap-cell narrow-cell">${column.orderNo!0}</td>
                <td class="cell-strong">${column.name!""}</td>
                <td class="cell-strong">${column.type!""}</td>
                <td class="center cell-strong bool-cell nowrap-cell narrow-cell">${column.primaryKeyText!""}</td>
                <td class="center cell-strong bool-cell nowrap-cell narrow-cell">${column.nullableText!""}</td>
                <td>${column.defaultValue!""}</td>
                <td>${column.comment!""}</td>
              </tr>
              </#list>
            </tbody>
          </table>
          </#if>

          <#if table.hasExtendedColumns>
          <h3 class="sub-title">字段扩展补充</h3>
          <table class="stripe compact-table">
            <colgroup>
              <#list table.extendedColumnLayout.columns![] as layoutColumn>
              <col style="width:${layoutColumn.htmlWidthPercent!''};" />
              </#list>
            </colgroup>
            <thead>
              <tr>
                <th class="center head-short head-nowrap narrow-head">序号</th>
                <th>字段名</th>
                <th>原始类型</th>
                <th>Java 类型</th>
                <th>扩展说明</th>
              </tr>
            </thead>
            <tbody>
              <#list table.extendedColumns![] as column>
              <tr class="${table.extendedColumnLayout.rowClasses[column?index]!''}">
                <td class="center cell-strong order-cell nowrap-cell narrow-cell">${column.orderNo!0}</td>
                <td class="cell-strong">${column.name!""}</td>
                <td class="cell-strong">${column.rawType!""}</td>
                <td class="cell-strong">${column.javaType!""}</td>
                <td>${column.extendedSummary!""}</td>
              </tr>
              </#list>
            </tbody>
          </table>
          </#if>

          <#if table.hasIndexes>
          <h3 class="sub-title">索引信息</h3>
          <table class="stripe compact-table">
            <colgroup>
              <#list table.indexLayout.columns![] as layoutColumn>
              <col style="width:${layoutColumn.htmlWidthPercent!''};" />
              </#list>
            </colgroup>
            <thead>
              <tr>
                <th>索引名</th>
                <th>包含字段</th>
                <th class="center head-short head-nowrap narrow-head">唯一</th>
                <th>类型</th>
              </tr>
            </thead>
            <tbody>
              <#list table.indexes![] as index>
              <tr class="${table.indexLayout.rowClasses[index?index]!''}">
                <td class="cell-strong">${index.name!""}</td>
                <td>${index.columnNamesText!""}</td>
                <td class="center cell-strong bool-cell nowrap-cell narrow-cell">${index.uniqueText!""}</td>
                <td class="cell-strong">${index.type!""}</td>
              </tr>
              </#list>
            </tbody>
          </table>
          </#if>

          <#if table.hasForeignKeys>
          <h3 class="sub-title">外键信息</h3>
          <table class="stripe compact-table">
            <colgroup>
              <#list table.foreignKeyLayout.columns![] as layoutColumn>
              <col style="width:${layoutColumn.htmlWidthPercent!''};" />
              </#list>
            </colgroup>
            <thead>
              <tr>
                <th>外键名</th>
                <th>本表字段</th>
                <th>引用表</th>
                <th>引用字段</th>
              </tr>
            </thead>
            <tbody>
              <#list table.foreignKeys![] as foreignKey>
              <tr class="${table.foreignKeyLayout.rowClasses[foreignKey?index]!''}">
                <td class="cell-strong">${foreignKey.name!""}</td>
                <td class="cell-strong">${foreignKey.columnName!""}</td>
                <td class="cell-strong">${foreignKey.referencedTable!""}</td>
                <td class="cell-strong">${foreignKey.referencedColumn!""}</td>
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
  </main>
</div>
</body>
</html>
