<template>
  <div class="admin-page template-page">
    <div class="admin-toolbar">
      <h1>模板</h1>
      <div class="admin-toolbar-actions">
        <el-button @click="loadDatasourceList" :loading="loading">刷新</el-button>
        <el-button type="primary" @click="handleNewExport">新建</el-button>
      </div>
    </div>

    <el-card class="admin-card list-card" shadow="never">
      <el-table :data="datasourceList" v-loading="loading" empty-text="暂无模板" class="admin-table template-table">
        <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="dbType" label="类型" width="120" />
        <el-table-column label="连接" min-width="220">
          <template #default="{ row }">
            {{ row.host || '-' }}<template v-if="row.port">:{{ row.port }}</template>
          </template>
        </el-table-column>
        <el-table-column label="库/Schema" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatDatabaseLabel(row) }}
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
</style>
