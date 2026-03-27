# ${title}

**Database:** ${database.name}
**Type:** ${database.type}
<#if database.version??>>**Version:** ${database.version}
</#if>

---

<#if database.tables?has_content>
<#list database.tables as table>
## ${table_index + 1}. ${table.name}<#if table.comment??> - ${table.comment}</#if>

### Columns

| # | Column | Type | Length | Nullable | Default | Comment |
|---|--------|------|--------|----------|---------|---------|
<#if table.columns?has_content>
<#list table.columns as col>
| ${col_index + 1} | **<#if col.primaryKey>PK </#if>${col.name}** | ${col.type!""} | ${col.length!""} | ${col.nullable?string("YES","NO")} | ${col.defaultValue!""} | ${col.comment!""} |
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
