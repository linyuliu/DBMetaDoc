<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta charset="UTF-8"/>
<title>${title}</title>
<style>
body { font-family: Arial, sans-serif; margin: 20px; color: #333; }
h1 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
h2 { color: #2980b9; margin-top: 30px; }
h3 { color: #16a085; }
table { width: 100%; border-collapse: collapse; margin: 10px 0; }
th { background-color: #3498db; color: white; padding: 8px; text-align: left; }
td { padding: 8px; border: 1px solid #ddd; }
tr:nth-child(even) { background-color: #f2f2f2; }
.db-info { background: #ecf0f1; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
.pk { font-weight: bold; color: #e74c3c; }
</style>
</head>
<body>
<h1>${title}</h1>
<div class="db-info">
  <strong>Database:</strong> ${database.databaseName!database.name}&#160;
  <strong>Type:</strong> ${database.type}&#160;
  <#if database.schemaName??><strong>Schema:</strong> ${database.schemaName}&#160;</#if>
  <#if database.charset??><strong>Charset:</strong> ${database.charset}&#160;</#if>
  <#if database.collation??><strong>Collation:</strong> ${database.collation}&#160;</#if>
  <#if database.version??><strong>Version:</strong> ${database.version}</#if>
</div>

<#if database.tables?has_content>
<#list database.tables as table>
<h2>${table_index + 1}. ${table.name}<#if table.comment??> - ${table.comment}</#if></h2>
<p>
  <strong>Schema:</strong> ${table.schema!""}
  <#if table.engine??>&#160;<strong>Engine:</strong> ${table.engine}</#if>
  <#if table.charset??>&#160;<strong>Charset:</strong> ${table.charset}</#if>
  <#if table.collation??>&#160;<strong>Collation:</strong> ${table.collation}</#if>
  <#if table.rowFormat??>&#160;<strong>Row Format:</strong> ${table.rowFormat}</#if>
</p>

<h3>Columns</h3>
<table>
  <tr><th>#</th><th>Column Name</th><th>Type</th><th>Raw Type</th><th>Java Type</th><th>Length</th><th>Nullable</th><th>Default</th><th>Auto</th><th>Generated</th><th>Comment</th></tr>
  <#if table.columns?has_content>
  <#list table.columns as col>
  <tr>
    <td>${col_index + 1}</td>
    <td class="${col.primaryKey?string("pk", "")}">${col.name}</td>
    <td>${col.type!""}</td>
    <td>${col.rawType!""}</td>
    <td>${col.javaType!""}</td>
    <td>${col.length!""}</td>
    <td>${col.nullable?string("YES","NO")}</td>
    <td>${col.defaultValue!""}</td>
    <td>${col.autoIncrement?string("YES","NO")}</td>
    <td>${col.generated?string("YES","NO")}</td>
    <td>${col.comment!""}</td>
  </tr>
  </#list>
  </#if>
</table>

<#if table.indexes?has_content>
<h3>Indexes</h3>
<table>
  <tr><th>Index Name</th><th>Columns</th><th>Unique</th><th>Type</th></tr>
  <#list table.indexes as idx>
  <tr>
    <td>${idx.name!""}</td>
    <td>${idx.columnNames?join(", ")}</td>
    <td>${idx.unique?string("YES","NO")}</td>
    <td>${idx.type!""}</td>
  </tr>
  </#list>
</table>
</#if>

<#if table.foreignKeys?has_content>
<h3>Foreign Keys</h3>
<table>
  <tr><th>FK Name</th><th>Column</th><th>Referenced Table</th><th>Referenced Column</th></tr>
  <#list table.foreignKeys as fk>
  <tr>
    <td>${fk.name!""}</td>
    <td>${fk.columnName!""}</td>
    <td>${fk.referencedTable!""}</td>
    <td>${fk.referencedColumn!""}</td>
  </tr>
  </#list>
</table>
</#if>

</#list>
</#if>
</body>
</html>
