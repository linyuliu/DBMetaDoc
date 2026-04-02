<template>
  <div class="admin-page wizard-page" v-loading="pageLoading">
    <div class="admin-toolbar">
      <h1>导出</h1>
      <el-button @click="handleBackToTemplates">模板</el-button>
    </div>

    <el-card class="admin-card step-card" shadow="never">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="连接" />
        <el-step title="范围" />
        <el-step title="预览" />
      </el-steps>
    </el-card>

    <section v-show="activeStep === 0" class="panel-shell">
      <el-card class="admin-card panel-card" shadow="never">
        <template #header>
          <div class="admin-card-header panel-head">
            <h2>连接</h2>
          </div>
        </template>

        <el-radio-group :model-value="sourceMode" @change="handleSourceModeChange" class="mode-switch">
          <el-radio-button label="template" value="template">模板</el-radio-button>
          <el-radio-button label="manual" value="manual">手工</el-radio-button>
        </el-radio-group>

        <el-form ref="sourceFormRef" :model="form" :rules="rules" label-width="80px" class="source-form">
          <div class="form-grid">
            <el-form-item v-if="sourceMode === 'template'" label="模板">
              <el-select
                :model-value="form.datasourceId"
                placeholder="选择"
                clearable
                filterable
                @change="handleTemplateSelection"
              >
                <el-option
                  v-for="item in datasourceList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                >
                  <div class="option-line">
                    <span>{{ item.name }}</span>
                    <span>{{ item.dbType }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="类型" prop="dbType">
              <el-select v-model="form.dbType" placeholder="选择" @change="handleDriverChange">
                <el-option v-for="driver in drivers" :key="driver.type" :label="driver.label" :value="driver.type" />
              </el-select>
            </el-form-item>

            <el-form-item label="JDBC URL" class="span-2">
              <el-input v-model="form.jdbcUrl" placeholder="选填">
                <template #append>
                  <el-button @click="handleParseJdbcUrl">解析</el-button>
                </template>
              </el-input>
            </el-form-item>

            <div v-if="jdbcFeedback" :class="['jdbc-feedback', `is-${jdbcFeedback.tone}`]">
              <span class="feedback-title">{{ jdbcFeedback.title }}</span>
              <span class="feedback-detail">{{ jdbcFeedback.detail }}</span>
            </div>

            <el-form-item label="主机" prop="host">
              <el-input v-model="form.host" placeholder="127.0.0.1" />
            </el-form-item>

            <el-form-item label="端口">
              <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" />
            </el-form-item>

            <el-form-item label="数据库" prop="database">
              <el-input v-model="form.database" />
            </el-form-item>

            <el-form-item v-if="showSchemaField" label="Schema">
              <el-input v-model="form.schema" />
            </el-form-item>

            <el-form-item label="用户" prop="username">
              <el-input v-model="form.username" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="canUseStoredPassword ? '可留空' : '请输入'"
              />
            </el-form-item>
          </div>

          <el-collapse v-model="extraPanels" class="minor-box">
            <el-collapse-item title="模板" name="template">
              <div class="minor-grid">
                <el-form-item label="名称" class="minor-field">
                  <el-input v-model="form.templateName" />
                </el-form-item>

                <el-form-item label="备注" class="minor-field minor-span-2">
                  <el-input v-model="form.remark" type="textarea" :rows="2" />
                </el-form-item>
              </div>

              <div class="switch-row">
                <el-switch v-model="form.enabled" active-text="启用" />
                <el-switch v-model="form.rememberPassword" active-text="记住密码" />
                <el-switch v-if="canUseStoredPassword" v-model="form.useStoredPassword" active-text="用已存密码" />
              </div>
            </el-collapse-item>
          </el-collapse>

          <div class="action-row">
            <el-button :loading="testing" @click="handleTestConnection(true)">测试</el-button>
            <el-button :loading="saving" @click="handleSaveTemplate">保存</el-button>
            <el-button type="primary" :loading="catalogLoading" @click="handleContinueToContent">下一步</el-button>
          </div>
        </el-form>
      </el-card>
    </section>

    <section v-show="activeStep === 1" class="panel-shell">
      <el-card class="admin-card panel-card" shadow="never">
        <template #header>
          <div class="admin-card-header panel-head">
            <h2>字段</h2>
            <div class="admin-toolbar-actions toolbar-actions">
              <el-button text @click="handleResetSections">默认</el-button>
              <el-button text @click="handleSelectAllSections">全选</el-button>
            </div>
          </div>
        </template>

        <el-checkbox-group v-model="form.exportSections" class="section-grid">
          <label v-for="section in documentOptions?.exportSections || []" :key="section.code" class="section-row">
            <el-checkbox :value="section.code">{{ section.label }}</el-checkbox>
          </label>
        </el-checkbox-group>
      </el-card>

      <el-card class="admin-card panel-card" shadow="never">
        <template #header>
          <div class="admin-card-header panel-head">
            <h2>表</h2>
            <div class="admin-toolbar-actions toolbar-actions">
              <el-button text :loading="catalogLoading" @click="loadCatalog(true)">刷新</el-button>
              <el-button text @click="handleSelectAllTables">全选</el-button>
              <el-button text @click="handleClearTables">清空</el-button>
            </div>
          </div>
        </template>

        <el-table
          ref="tableListRef"
          :data="availableTables"
          row-key="key"
          empty-text="暂无表"
          max-height="460"
          class="admin-table table-select"
          @selection-change="handleTableSelectionChange"
        >
          <el-table-column type="selection" width="52" reserve-selection />
          <el-table-column prop="name" label="表" min-width="260" show-overflow-tooltip />
          <el-table-column v-if="showTableSchemaColumn" label="Schema" width="140">
            <template #default="{ row }">
              {{ row.schema || '-' }}
            </template>
          </el-table-column>
          <el-table-column v-if="showTableCommentColumn" prop="comment" label="注释" min-width="300" show-overflow-tooltip />
          <el-table-column prop="columnCount" label="列" width="88" align="center" />
        </el-table>

        <div class="admin-count-text table-footer">
          已选 {{ selectedTableCount }}
        </div>

        <div class="action-row">
          <el-button @click="activeStep = 0">上一步</el-button>
          <el-button type="primary" @click="handleContinueToPreview">下一步</el-button>
        </div>
      </el-card>
    </section>

    <section v-show="activeStep === 2" class="panel-shell">
      <el-card class="admin-card panel-card" shadow="never">
        <template #header>
          <div class="admin-card-header panel-head">
            <h2>参数</h2>
          </div>
        </template>

        <div class="export-grid">
          <label class="field-block">
            <span class="field-label">标题</span>
            <el-input v-model="form.title" />
          </label>

          <label class="field-block">
            <span class="field-label">导出格式</span>
            <el-select v-model="form.format">
              <el-option v-for="item in documentFormats" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>

          <label class="field-block">
            <span class="field-label">字体预设</span>
            <el-select v-model="form.fontPreset">
              <el-option
                v-for="preset in documentOptions?.fontPresets || []"
                :key="preset.code"
                :label="preset.label"
                :value="preset.code"
              />
            </el-select>
          </label>

          <el-collapse class="advanced-box">
            <el-collapse-item title="高级" name="advanced">
              <div class="advanced-grid">
                <div class="field-block">
                  <span class="field-label">布尔</span>
                  <el-radio-group v-model="form.booleanDisplayStyle">
                    <el-radio-button
                      v-for="item in booleanDisplayOptions"
                      :key="item.value"
                      :label="item.value"
                      :value="item.value"
                    >
                      {{ item.label }}
                    </el-radio-button>
                  </el-radio-group>
                </div>

                <div class="switch-column">
                  <el-switch v-model="form.useCache" active-text="使用缓存" />
                  <el-switch v-model="form.forceRefresh" active-text="强制刷新" />
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="action-row">
          <el-button @click="activeStep = 1">上一步</el-button>
          <el-button :loading="previewing" @click="handlePreview(true)">刷新</el-button>
          <el-button type="primary" :loading="exporting" @click="handleExport">导出</el-button>
        </div>
      </el-card>

      <el-card class="admin-card panel-card preview-card" shadow="never">
        <template #header>
          <div class="admin-card-header panel-head">
            <h2>预览</h2>
            <el-button text :loading="previewing" @click="handlePreview(true)">刷新</el-button>
          </div>
        </template>

        <el-empty v-if="!previewHtml && !previewing" description="暂无" />
        <div v-else class="admin-preview-surface preview-panel" v-loading="previewing" v-html="previewHtml"></div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { TableInstance } from 'element-plus'
import type { TableOption } from '../api/dbmeta'
import { useExportWizardPage } from './export-wizard'

const {
  activeStep,
  sourceMode,
  sourceFormRef,
  form,
  rules,
  pageLoading,
  testing,
  saving,
  catalogLoading,
  previewing,
  exporting,
  drivers,
  datasourceList,
  documentOptions,
  availableTables,
  previewHtml,
  jdbcFeedback,
  canUseStoredPassword,
  selectedTableCount,
  showSchemaField,
  showTableSchemaColumn,
  showTableCommentColumn,
  documentFormats,
  booleanDisplayOptions,
  handleDriverChange,
  handleSourceModeChange,
  handleTemplateSelection,
  handleParseJdbcUrl,
  handleTestConnection,
  handleContinueToContent,
  handleSaveTemplate,
  handleSelectAllTables,
  handleClearTables,
  handleResetSections,
  handleSelectAllSections,
  loadCatalog,
  handleContinueToPreview,
  handlePreview,
  handleExport,
  handleBackToTemplates
} = useExportWizardPage()

const tableListRef = ref<TableInstance>()
const extraPanels = ref<string[]>([])
let syncingTableSelection = false

function handleTableSelectionChange(selection: TableOption[]) {
  if (syncingTableSelection) {
    return
  }
  form.selectedTableKeys = selection.map(item => item.key)
}

async function syncTableSelection() {
  await nextTick()
  const table = tableListRef.value
  if (!table) {
    return
  }

  syncingTableSelection = true
  table.clearSelection()
  const selectedKeys = new Set(form.selectedTableKeys)
  for (const row of availableTables.value) {
    if (selectedKeys.has(row.key)) {
      table.toggleRowSelection(row, true)
    }
  }
  syncingTableSelection = false
}

watch([availableTables, () => form.selectedTableKeys.join('|')], () => {
  void syncTableSelection()
}, { immediate: true })

watch(activeStep, step => {
  if (step === 1) {
    void syncTableSelection()
  }
})
</script>

<style scoped>
.step-card {
  margin-top: 16px;
}

.step-card :deep(.el-card__body) {
  padding: 14px 20px;
}

.panel-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.panel-head,
.toolbar-actions,
.action-row,
.switch-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-head {
  justify-content: space-between;
}

.toolbar-actions,
.action-row,
.switch-row {
  flex-wrap: wrap;
}

.mode-switch {
  margin-bottom: 16px;
}

.source-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.span-2,
.minor-span-2,
.jdbc-feedback {
  grid-column: 1 / -1;
}

.jdbc-feedback {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #f8fafc;
  font-size: 13px;
  line-height: 1.6;
}

.jdbc-feedback.is-success {
  border-color: #d9ecff;
  background: #f5f9ff;
}

.jdbc-feedback.is-warning {
  border-color: #f3d19e;
  background: #fffaf0;
}

.feedback-title {
  font-weight: 600;
  color: #1f2937;
}

.feedback-detail {
  color: #606266;
}

.option-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.option-line span:first-child {
  font-weight: 600;
}

.minor-box {
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.minor-box :deep(.el-collapse-item__header) {
  padding: 0 12px;
  font-size: 14px;
  color: #303133;
}

.minor-box :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.minor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
  padding: 4px 0 8px;
}

.minor-field {
  margin-bottom: 14px;
}

.section-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.section-row {
  display: flex;
  align-items: center;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #ffffff;
}

.table-select :deep(.el-table__cell) {
  padding: 10px 0;
}

.table-footer {
  margin-top: 12px;
}

.export-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  color: #606266;
}

.advanced-box {
  grid-column: 1 / -1;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.advanced-box :deep(.el-collapse-item__header) {
  padding: 0 12px;
  font-size: 14px;
  color: #303133;
}

.advanced-box :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.advanced-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(240px, 1fr);
  gap: 16px;
  padding: 4px 0 12px;
}

.switch-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.preview-card :deep(.el-card__body) {
  padding: 0 20px 20px;
}

.preview-panel {
  max-height: 80vh;
}

@media (max-width: 1120px) {
  .section-grid,
  .export-grid,
  .advanced-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .form-grid,
  .minor-grid,
  .section-grid,
  .export-grid,
  .advanced-grid {
    grid-template-columns: 1fr;
  }
}
</style>
