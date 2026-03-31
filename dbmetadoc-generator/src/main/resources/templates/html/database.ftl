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

  <div class="screen-only-block screen-head" id="catalog">
    <div>
      <p class="screen-kicker">表结构</p>
      <h1>${view.databaseName!view.title}</h1>
      <p>共 ${view.tableCount!0} 张表</p>
    </div>
  </div>

  <#if view.hasTables>
  <details class="screen-only-block preview-directory">
    <summary>
      <span>预览目录</span>
      <small>共 ${view.tableCount!0} 张表</small>
    </summary>
    <ul class="directory-grid">
      <#list view.tables![] as table>
      <li>
        <a href="#table-${table.tableNo!0}">
          <span class="mono">${table.tableNo!0}. ${table.schema!""}<#if table.schema?has_content>.</#if>${table.name!""}</span>
          <small>${table.comment!""}</small>
        </a>
      </li>
      </#list>
    </ul>
  </details>
  </#if>

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
      <section class="screen-only-block section">
        <div class="section-head">
          <h2 class="section-title">一、表目录</h2>
          <a class="screen-only-link" href="#catalog">返回目录</a>
        </div>
        <table class="stripe compact-table table-overview-table">
          <thead>
            <tr>
              <th style="width:12mm;" class="center">序号</th>
              <th>表名</th>
              <th style="width:42mm;">表说明</th>
            </tr>
          </thead>
          <tbody>
            <#list view.tableOverviewRows![] as table>
            <tr>
              <td class="center">${table.tableNo!0}</td>
              <td class="mono center">${table.schema!""}<#if table.schema?has_content>.</#if>${table.name!""}</td>
              <td>${table.comment!""}</td>
            </tr>
            </#list>
          </tbody>
        </table>
      </section>
      </#if>

      <#if view.hasTables>
        <#list view.tables![] as table>
        <section class="section chapter" id="table-${table.tableNo!0}">
          <div class="section-head chapter-head">
            <h2 class="section-title table-section-title">${table.tableNo!0}. ${table.schema!""}<#if table.schema?has_content>.</#if>${table.name!""}</h2>
            <a class="screen-only-link" href="#catalog">返回目录</a>
          </div>
          <p class="table-comment-line">${table.comment!""}</p>

          <#if table.hasBasicColumns>
          <h3 class="sub-title">字段清单</h3>
          <table class="stripe grid-table">
            <thead>
              <tr>
                <th class="screen-only-cell center" style="width:10mm;">序号</th>
                <th style="width:28mm;">列名</th>
                <th style="width:22mm;">数据类型</th>
                <th class="screen-only-cell center" style="width:18mm;">长度</th>
                <th class="screen-only-cell center" style="width:22mm;">精度/小数位</th>
                <th style="width:10mm;" class="center">主键</th>
                <th class="screen-only-cell center" style="width:10mm;">自增</th>
                <th style="width:12mm;" class="center">允许空</th>
                <th style="width:22mm;">默认值</th>
                <th>列说明</th>
              </tr>
            </thead>
            <tbody>
              <#list table.columns![] as column>
              <tr>
                <td class="screen-only-cell center">${column.orderNo!0}</td>
                <td class="mono">${column.name!""}</td>
                <td class="mono">${column.type!""}</td>
                <td class="screen-only-cell center">${column.lengthText!""}</td>
                <td class="screen-only-cell center">${column.precisionScaleText!""}</td>
                <td class="center">${column.primaryKeyText!""}</td>
                <td class="screen-only-cell center">${column.autoIncrementText!""}</td>
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
          <table class="stripe compact-table">
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
          <table class="stripe compact-table">
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
          <table class="stripe compact-table">
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
  </main>
</div>
</body>
</html>
