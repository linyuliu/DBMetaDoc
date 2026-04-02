<template>
  <div class="admin-page template-page">
    <div class="admin-toolbar">
      <div class="toolbar-heading">
        <h1>模板</h1>
        <span class="admin-muted-text">共 {{ datasourceList.length }}</span>
      </div>
      <div class="admin-toolbar-actions">
        <el-button @click="loadDatasourceList" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="handleNewExport">新建</el-button>
      </div>
    </div>

    <el-card class="admin-card list-card" shadow="never">
      <template #header>
        <div class="admin-card-header">
          <h2>数据源</h2>
          <span class="admin-muted-text">可直接复用</span>
        </div>
      </template>

      <el-table :data="datasourceList" v-loading="loading" empty-text="暂无模板" class="admin-table template-table">
        <el-table-column label="名称" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="template-name-cell">
              <div class="template-name">{{ row.name }}</div>
              <div v-if="row.remark" class="template-remark">{{ row.remark }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140" align="center">
          <template #default="{ row }">
            <span class="type-pill">{{ row.dbType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="连接" min-width="220">
          <template #default="{ row }">
            <span class="connection-text">{{ row.host || '-' }}<template v-if="row.port">:{{ row.port }}</template></span>
          </template>
        </el-table-column>
        <el-table-column label="库/Schema" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="database-text">{{ formatDatabaseLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleUseTemplate(row.id)">使用</el-button>
            <el-button text type="danger" @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useTemplateCenterPage } from './template-center'

const {
  loading,
  datasourceList,
  loadDatasourceList,
  handleNewExport,
  handleUseTemplate,
  handleRemove,
  formatDatabaseLabel
} = useTemplateCenterPage()
</script>

<style scoped>
.list-card {
  margin-top: 16px;
}

.list-card :deep(.el-card__body) {
  padding: 0;
}

.template-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 0;
}

.template-name {
  font-size: 14px;
  font-weight: 700;
  color: #17212b;
}

.template-remark {
  font-size: 12px;
  color: #6a7788;
}

.type-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #d7e2f5;
  border-radius: 999px;
  background: #f4f8ff;
  color: #2853a3;
  font-size: 12px;
  font-weight: 700;
}

.connection-text,
.database-text {
  color: #3b4a5d;
}

.connection-text {
  font-family: var(--dbm-font-mono);
  font-size: 12px;
}
</style>
