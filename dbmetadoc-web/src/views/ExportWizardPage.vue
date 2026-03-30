<template>
  <div class="wizard-page" v-loading="pageLoading">
    <header class="wizard-head">
      <div>
        <p class="page-kicker">DBMetaDoc</p>
        <h1>导出向导</h1>
        <p>按数据源、表和字段组逐步筛选，最后再预览和导出，避免一次性把全部配置堆在一起。</p>
      </div>
      <el-button @click="handleBackToTemplates">返回模板中心</el-button>
    </header>

    <el-card class="step-card" shadow="never">
      <el-steps :active="activeStep" align-center finish-status="success">
        <el-step title="数据源与密码" description="选择模板或手工填写连接" />
        <el-step title="导出内容" description="选择表和字段组" />
        <el-step title="预览与导出" description="预览样式并生成文件" />
      </el-steps>
    </el-card>

    <section v-show="activeStep === 0" class="panel-shell">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <div>
              <h2>第一步：数据源与密码</h2>
              <p>模板密码只保存密文，测试、预览、导出都会按“输入密码优先，其次已存密码”处理。</p>
            </div>
          </div>
        </template>

        <el-radio-group :model-value="sourceMode" @change="handleSourceModeChange" class="mode-switch">
          <el-radio-button label="template" value="template">使用模板</el-radio-button>
          <el-radio-button label="manual" value="manual">手工填写</el-radio-button>
        </el-radio-group>

        <el-form ref="sourceFormRef" :model="form" :rules="rules" label-width="108px" class="source-form">
          <div class="form-grid">
            <el-form-item v-if="sourceMode === 'template'" label="选择模板">
              <el-select
                :model-value="form.datasourceId"
                placeholder="请选择已保存模板"
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

            <el-form-item label="模板名称">
              <el-input v-model="form.templateName" placeholder="保存模板时使用，例如：生产库文档" />
            </el-form-item>

            <el-form-item label="数据库类型" prop="dbType">
              <el-select v-model="form.dbType" placeholder="请选择数据库类型" @change="handleDriverChange">
                <el-option v-for="driver in drivers" :key="driver.type" :label="driver.label" :value="driver.type" />
              </el-select>
            </el-form-item>

            <el-form-item label="JDBC URL" class="span-2">
              <el-input v-model="form.jdbcUrl" placeholder="可选，未填写的结构化参数会尝试从这里补齐">
                <template #append>
                  <el-button @click="handleParseJdbcUrl">解析回填</el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="例如：127.0.0.1" />
            </el-form-item>

            <el-form-item label="端口">
              <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" />
            </el-form-item>

            <el-form-item label="数据库名" prop="database">
              <el-input v-model="form.database" placeholder="MySQL/PG 为数据库名，Oracle 可填服务名" />
            </el-form-item>

            <el-form-item label="Schema">
              <el-input v-model="form.schema" placeholder="可选，PG / Kingbase 常用" />
            </el-form-item>

            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="数据库用户名" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="canUseStoredPassword ? '留空则使用已保存密码' : '请输入数据库密码'"
              />
            </el-form-item>

            <el-form-item label="模板备注" class="span-2">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选，用于标记数据源用途" />
            </el-form-item>
          </div>

          <div class="switch-row">
            <el-switch v-model="form.enabled" active-text="模板启用" />
            <el-switch v-model="form.rememberPassword" active-text="保存密码（简单加密）" />
            <el-switch v-model="form.useStoredPassword" active-text="使用已保存密码" :disabled="!canUseStoredPassword" />
          </div>

          <div v-if="currentTemplate" class="template-tip">
            <span>当前模板：{{ currentTemplate.name }}</span>
            <span v-if="currentTemplate.passwordSaved">已保存密码，可直接测试、预览和导出</span>
            <span v-else>未保存密码，本次仍需输入密码</span>
          </div>

          <div class="action-row">
            <el-button @click="handleBackToTemplates">返回</el-button>
            <el-button :loading="testing" @click="handleTestConnection(true)">测试连接</el-button>
            <el-button type="warning" :loading="saving" @click="handleSaveTemplate">保存模板</el-button>
            <el-button type="primary" :loading="catalogLoading" @click="handleContinueToContent">下一步：选择内容</el-button>
          </div>
        </el-form>
      </el-card>
    </section>

    <section v-show="activeStep === 1" class="panel-shell two-column">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <div>
              <h2>第二步：字段组</h2>
              <p>默认按 A4 打印版式导出；列扩展会以下方补充区展示，不会扩宽主字段表。</p>
            </div>
            <div class="inline-actions">
              <el-button text @click="handleResetSections">恢复默认</el-button>
              <el-button text @click="handleSelectAllSections">全选</el-button>
            </div>
          </div>
        </template>

        <el-checkbox-group v-model="form.exportSections" class="section-list">
          <label v-for="section in documentOptions?.exportSections || []" :key="section.code" class="section-item">
            <el-checkbox :value="section.code">{{ section.label }}</el-checkbox>
            <p>{{ section.description }}</p>
          </label>
        </el-checkbox-group>

        <div class="summary-strip">
          <span>已选字段组 {{ selectedSectionCount }} 个</span>
        </div>
      </el-card>

      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <div>
              <h2>第二步：表选择</h2>
              <p>仅导出你勾选的表，未勾选的表不会出现在预览和最终文件中。</p>
            </div>
            <div class="inline-actions">
              <el-button text @click="loadCatalog(true)" :loading="catalogLoading">刷新目录</el-button>
              <el-button text @click="handleSelectAllTables">全选</el-button>
              <el-button text @click="handleClearTables">清空</el-button>
            </div>
          </div>
        </template>

        <el-empty v-if="!catalog?.tables?.length && !catalogLoading" description="还没有可选表，请先加载目录" />
        <el-checkbox-group v-else v-model="form.selectedTableKeys" class="table-grid">
          <label v-for="table in catalog?.tables || []" :key="table.key" class="table-item">
            <el-checkbox :value="table.key">
              <span class="table-title">{{ table.name }}</span>
            </el-checkbox>
            <p>{{ table.schema || '默认 Schema' }} · {{ table.columnCount }} 列</p>
            <span class="table-comment">{{ table.comment || '无注释' }}</span>
          </label>
        </el-checkbox-group>

        <div class="summary-strip">
          <span>数据库：{{ catalog?.databaseName || '未加载' }}</span>
          <span>已选表 {{ selectedTableCount }} 张</span>
        </div>

        <div class="action-row">
          <el-button @click="activeStep = 0">上一步</el-button>
          <el-button type="primary" @click="handleContinueToPreview">下一步：预览与导出</el-button>
        </div>
      </el-card>
    </section>

    <section v-show="activeStep === 2" class="panel-shell preview-layout">
      <el-card class="panel-card preview-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <div>
              <h2>第三步：预览</h2>
              <p>切换表、字段组或字体预设后，预览会自动刷新。</p>
            </div>
            <el-button text :loading="previewing" @click="handlePreview(true)">手动刷新</el-button>
          </div>
        </template>

        <el-empty v-if="!previewHtml && !previewing" description="预览将在这里显示" />
        <div v-else class="preview-panel" v-loading="previewing" v-html="previewHtml"></div>
      </el-card>

      <el-card class="panel-card export-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <div>
              <h2>第三步：导出参数</h2>
              <p>格式、字体和缓存策略统一放在这里调整。</p>
            </div>
          </div>
        </template>

        <div class="export-form">
          <label class="field-block">
            <span>文档标题</span>
            <el-input v-model="form.title" placeholder="默认使用“数据库名 + 数据库文档”" />
          </label>

          <label class="field-block">
            <span>导出格式</span>
            <el-select v-model="form.format">
              <el-option v-for="item in documentFormats" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>

          <label class="field-block">
            <span>字体预设</span>
            <el-select v-model="form.fontPreset">
              <el-option
                v-for="preset in documentOptions?.fontPresets || []"
                :key="preset.code"
                :label="preset.label"
                :value="preset.code"
              >
                <div class="font-option">
                  <span>{{ preset.label }}</span>
                  <small>{{ preset.titleFont }} / {{ preset.bodyFont }} / {{ preset.monoFont }}</small>
                </div>
              </el-option>
            </el-select>
          </label>

          <el-collapse class="advanced-box">
            <el-collapse-item title="高级选项" name="advanced">
              <div class="switch-column">
                <el-switch v-model="form.useCache" active-text="优先使用缓存" />
                <el-switch v-model="form.forceRefresh" active-text="强制刷新元数据" />
              </div>
            </el-collapse-item>
          </el-collapse>

          <div class="summary-box">
            <span>已选表 {{ selectedTableCount }} 张</span>
            <span>已选字段组 {{ selectedSectionCount }} 个</span>
          </div>

          <div class="action-row">
            <el-button @click="activeStep = 1">上一步</el-button>
            <el-button :loading="previewing" @click="handlePreview(true)">刷新预览</el-button>
            <el-button type="success" :loading="exporting" @click="handleExport">导出文件</el-button>
          </div>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
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
  catalog,
  previewHtml,
  currentTemplate,
  canUseStoredPassword,
  selectedTableCount,
  selectedSectionCount,
  documentFormats,
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
</script>

<style scoped>
.wizard-page {
  max-width: 1420px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.wizard-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.page-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: #4e6c79;
  text-transform: uppercase;
}

.wizard-head h1 {
  margin: 0;
  font-size: 34px;
  color: #112833;
}

.wizard-head p:last-child {
  max-width: 780px;
  margin: 12px 0 0;
  line-height: 1.8;
  color: #5f7886;
}

.step-card,
.panel-card {
  border: 1px solid rgba(20, 58, 70, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
}

.step-card {
  margin-top: 20px;
}

.panel-shell {
  margin-top: 20px;
}

.two-column {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 18px;
}

.preview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(360px, 0.75fr);
  gap: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.panel-head h2 {
  margin: 0;
  font-size: 21px;
  color: #183543;
}

.panel-head p {
  margin: 6px 0 0;
  color: #617986;
}

.inline-actions,
.action-row,
.switch-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.mode-switch {
  margin-bottom: 18px;
}

.source-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 18px;
}

.span-2 {
  grid-column: span 2;
}

.template-tip,
.summary-strip,
.summary-box {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f2f7f9;
  color: #546d7b;
}

.section-list,
.table-grid {
  display: grid;
  gap: 12px;
}

.section-item,
.table-item {
  display: block;
  padding: 14px 16px;
  border: 1px solid rgba(22, 61, 80, 0.1);
  border-radius: 16px;
  background: #fcfdfd;
}

.section-item p,
.table-item p,
.table-comment {
  display: block;
  margin: 8px 0 0;
  color: #607987;
  line-height: 1.7;
}

.table-grid {
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.table-title,
.option-line span:first-child {
  font-family: "Cascadia Mono", "JetBrains Mono", "LXGW WenKai Mono Screen", "Consolas", monospace;
}

.table-comment {
  font-size: 13px;
}

.option-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.preview-panel {
  min-height: 72vh;
  overflow: auto;
  padding: 8px;
  border-radius: 18px;
  background: #fff;
}

.export-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #183543;
}

.font-option {
  display: flex;
  flex-direction: column;
}

.font-option small {
  margin-top: 4px;
  color: #64808f;
}

.advanced-box {
  border: 1px solid rgba(22, 61, 80, 0.08);
  border-radius: 16px;
  overflow: hidden;
}

.switch-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 1160px) {
  .two-column,
  .preview-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .wizard-page {
    padding: 18px 14px 28px;
  }

  .wizard-head {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
