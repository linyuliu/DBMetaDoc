# ${title}

**Database:** ${database.databaseName!database.name}
**Type:** ${database.type}
<#if database.schemaName??>**Schema:** ${database.schemaName}
</#if>
<#if database.charset??>**Charset:** ${database.charset}
</#if>
<#if database.collation??>**Collation:** ${database.collation}
</#if>
<#if database.version??>**Version:** ${database.version}
</#if>

---

<#if database.tables?has_content>
<#list database.tables as table>
## ${table_index + 1}. ${table.name}<#if table.comment??> - ${table.comment}</#if>

**Schema:** ${table.schema!""}
<#if table.engine??> | **Engine:** ${table.engine}</#if>
<#if table.charset??> | **Charset:** ${table.charset}</#if>
<#if table.collation??> | **Collation:** ${table.collation}</#if>
<#if table.rowFormat??> | **Row Format:** ${table.rowFormat}</#if>

### Columns

| # | Column | Type | Raw Type | Java Type | Length | Nullable | Default | Auto | Generated | Comment |
|---|--------|------|----------|-----------|--------|----------|---------|------|-----------|---------|
<#if table.columns?has_content>
<#list table.columns as col>
| ${col_index + 1} | **<#if col.primaryKey>PK </#if>${col.name}** | ${col.type!""} | ${col.rawType!""} | ${col.javaType!""} | ${col.length!""} | ${col.nullable?string("YES","NO")} | ${col.defaultValue!""} | ${col.autoIncrement?string("YES","NO")} | ${col.generated?string("YES","NO")} | ${col.comment!""} |
</#list>
</#if>

<#if table.indexes?has_content>
### Indexes

| Index Name | Columns | Unique | Type |
|------------|---------|--------|------|
<#list table.indexes as idx>
| ${idx.name!""} | ${idx.columnNames?join(", ")} | ${idx.unique?string("YES","NO")} | ${idx.type!""} |
</#list>

</#if>
<#if table.foreignKeys?has_content>
### Foreign Keys

| FK Name | Column | Referenced Table | Referenced Column |
|---------|--------|-----------------|-------------------|
<#list table.foreignKeys as fk>
| ${fk.name!""} | ${fk.columnName!""} | ${fk.referencedTable!""} | ${fk.referencedColumn!""} |
</#list>

</#if>
---

</#list>
</#if>
