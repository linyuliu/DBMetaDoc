<template>
  <div class="page-shell">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">DBMetaDoc</p>
        <h1>数据库元数据文档台</h1>
        <p class="hero-copy">
          单体部署，GET/POST 接口，保存模板前自动测试连接。预览返回统一 `R`，导出保持文件流。
        </p>
      </div>
      <div class="hero-stats">
        <div class="stat-card">
          <span class="stat-label">支持驱动</span>
          <strong>{{ drivers.length }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">已存模板</span>
          <strong>{{ datasourceList.length }}</strong>
        </div>
      </div>
    </section>

    <div class="content-grid">
      <el-card class="main-card" shadow="never">
        <template #header>
          <div class="card-head">
            <div>
              <h2>连接与导出</h2>
              <p>保存模板时会先执行连接测试，不存密码。</p>
            </div>
            <el-tag type="success" effect="light">仅 GET / POST</el-tag>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="108px" class="doc-form">
          <div class="form-grid">
            <el-form-item label="模板名称" prop="sourceName">
              <el-input v-model="form.sourceName" placeholder="保存模板时使用，例如：生产 MySQL" />
            </el-form-item>

            <el-form-item label="数据库类型" prop="dbType">
              <el-select v-model="form.dbType" placeholder="请选择数据库类型" @change="handleDriverChange">
                <el-option
                  v-for="driver in drivers"
                  :key="driver.type"
                  :label="driver.label"
                  :value="driver.type"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="JDBC URL" class="span-2">
              <el-input
                v-model="form.jdbcUrl"
                placeholder="可选。结构化字段优先，未填写的 host / port / database / schema 会尝试从这里解析"
              />
            </el-form-item>

            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="例如：127.0.0.1" />
            </el-form-item>

            <el-form-item label="端口" prop="port">
              <el-input-number v-model="form.port" :min="1" :max="65535" controls-position="right" />
            </el-form-item>

            <el-form-item label="数据库名" prop="database">
              <el-input v-model="form.database" placeholder="MySQL/PG 为数据库名，Oracle 可填服务名" />
            </el-form-item>

            <el-form-item label="Schema">
              <el-input v-model="form.schema" placeholder="可选。PG / Kingbase 可填，Oracle / 达梦默认用户名大写" />
            </el-form-item>

            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="数据库用户名" />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="保存模板前必须测试连接" />
            </el-form-item>

            <el-form-item label="文档标题">
              <el-input v-model="form.title" placeholder="默认使用“数据库名 + 数据库文档”" />
            </el-form-item>

            <el-form-item label="导出格式" prop="format">
              <el-select v-model="form.format">
                <el-option label="HTML" value="HTML" />
                <el-option label="MARKDOWN" value="MARKDOWN" />
                <el-option label="PDF" value="PDF" />
                <el-option label="WORD" value="WORD" />
              </el-select>
            </el-form-item>

            <el-form-item label="模板备注" class="span-2">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选，用于说明连接用途" />
            </el-form-item>
          </div>

          <div class="option-row">
            <el-switch v-model="form.enabled" active-text="模板启用" />
            <el-switch v-model="form.useCache" active-text="读取缓存" />
            <el-switch v-model="form.forceRefresh" active-text="强制刷新" />
          </div>

          <div class="driver-strip" v-if="activeDriver">
            <el-tag>{{ activeDriver.driverClass }}</el-tag>
            <el-tag type="info">测试语句：{{ activeDriver.testSql }}</el-tag>
            <el-tag type="info">策略：{{ activeDriver.metadataStrategy }}</el-tag>
            <el-tag v-if="activeDriver.domestic" type="warning">国产数据库</el-tag>
            <el-tag v-if="activeDriver.mysqlLike" type="success">MySQL-like</el-tag>
            <el-tag v-if="activeDriver.pgLike" type="primary">PG-like</el-tag>
            <el-tag v-if="activeDriver.oracleLike" type="danger">Oracle-like</el-tag>
            <el-tag v-if="activeDriver.supportsJdbcUrl" type="info">支持 JDBC URL</el-tag>
          </div>

          <div class="action-row">
            <el-button @click="resetForm">重置</el-button>
            <el-button type="info" :loading="testing" @click="handleTest">测试连接</el-button>
            <el-button type="warning" :loading="saving" @click="handleSave">保存模板</el-button>
            <el-button type="primary" :loading="previewing" @click="handlePreview">预览</el-button>
            <el-button type="success" :loading="exporting" @click="handleExport">导出</el-button>
          </div>
        </el-form>
      </el-card>

      <div class="side-stack">
        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="card-head compact">
              <div>
                <h2>已保存模板</h2>
                <p>点击填充会回带连接信息，但不会回带密码。</p>
              </div>
              <el-button text @click="loadDatasourceList" :loading="loadingSources">刷新</el-button>
            </div>
          </template>

          <el-empty v-if="!datasourceList.length && !loadingSources" description="暂无模板" />
          <div v-else class="datasource-list">
            <div v-for="item in datasourceList" :key="item.id" class="datasource-item">
              <div class="datasource-info">
                <strong>{{ item.name }}</strong>
                <span>{{ item.dbType }} · {{ item.host }}:{{ item.port }}</span>
                <span>{{ item.database }}<template v-if="item.schema"> / {{ item.schema }}</template></span>
              </div>
              <div class="datasource-actions">
                <el-button text type="primary" @click="applyDatasource(item.id)">填充</el-button>
                <el-button text type="danger" @click="handleRemove(item.id)">删除</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="card-head compact">
              <div>
                <h2>驱动能力</h2>
                <p>首批内置 MySQL / PostgreSQL / Oracle / Kingbase / 达梦。</p>
              </div>
            </div>
          </template>
          <div class="driver-list">
            <div v-for="driver in drivers" :key="driver.type" class="driver-item">
              <div class="driver-title">
                <strong>{{ driver.label }}</strong>
                <span>{{ driver.type }}</span>
              </div>
              <div class="driver-meta">
                <span>端口 {{ driver.defaultPort }}</span>
                <span>{{ driver.testSql }}</span>
                <span>{{ driver.metadataStrategy }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="previewVisible" title="文档预览" width="86%" top="4vh">
      <div class="preview-panel" v-html="previewHtml"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  exportDocument,
  fetchDatasourceDetail,
  fetchDatasourceList,
  fetchDrivers,
  previewDocument,
  removeDatasource,
  saveDatasource,
  testDatasource
} from '../api/dbmeta'
import type { BlobResult } from '../utils/request'
import type { DatasourceDetail, DriverInfo, DocumentPayload, SaveDatasourcePayload } from '../api/dbmeta'

interface FormModel {
  sourceName: string
  remark: string
  enabled: boolean
  dbType: string
  jdbcUrl: string
  host: string
  port: number
  database: string
  schema: string
  username: string
  password: string
  title: string
  format: string
  useCache: boolean
  forceRefresh: boolean
}

const formRef = ref<FormInstance>()
const testing = ref(false)
const saving = ref(false)
const previewing = ref(false)
const exporting = ref(false)
const loadingSources = ref(false)
const previewVisible = ref(false)
const previewHtml = ref('')
const drivers = ref<DriverInfo[]>([])
const datasourceList = ref<DatasourceDetail[]>([])
const editingId = ref<number | null>(null)

const form = reactive<FormModel>({
  sourceName: '',
  remark: '',
  enabled: true,
  dbType: 'MYSQL',
  jdbcUrl: '',
  host: '127.0.0.1',
  port: 3306,
  database: '',
  schema: '',
  username: '',
  password: '',
  title: '',
  format: 'HTML',
  useCache: true,
  forceRefresh: false
})

const rules: FormRules<FormModel> = {
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{
    validator: (_rule, value, callback) => {
      if (value || form.jdbcUrl) {
        callback()
        return
      }
      callback(new Error('请输入主机地址，或填写 JDBC URL'))
    },
    trigger: 'blur'
  }],
  database: [{
    validator: (_rule, value, callback) => {
      if (value || form.jdbcUrl) {
        callback()
        return
      }
      callback(new Error('请输入数据库名，或填写 JDBC URL'))
    },
    trigger: 'blur'
  }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  format: [{ required: true, message: '请选择导出格式', trigger: 'change' }]
}

const activeDriver = computed(() => drivers.value.find(item => item.type === form.dbType) || null)

onMounted(async () => {
  await Promise.all([loadDrivers(), loadDatasourceList()])
})

async function loadDrivers() {
  const result = await fetchDrivers()
  drivers.value = result || []
  if (!activeDriver.value && drivers.value.length) {
    form.dbType = drivers.value[0].type
    form.port = drivers.value[0].defaultPort
  }
}

async function loadDatasourceList() {
  loadingSources.value = true
  try {
    datasourceList.value = await fetchDatasourceList()
  } finally {
    loadingSources.value = false
  }
}

function handleDriverChange(type: string) {
  const driver = drivers.value.find(item => item.type === type)
  if (driver) {
    form.port = driver.defaultPort
  }
}

function buildPayload(): DocumentPayload {
  return {
    dbType: form.dbType,
    jdbcUrl: form.jdbcUrl,
    host: form.host,
    port: form.port,
    database: form.database,
    schema: form.schema,
    username: form.username,
    password: form.password,
    title: form.title,
    format: form.format,
    useCache: form.useCache,
    forceRefresh: form.forceRefresh
  }
}

function saveBlob({ blob, fileName }: BlobResult) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function validateForm() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
}

async function handleTest() {
  await validateForm()
  testing.value = true
  try {
    await testDatasource(buildPayload())
    ElMessage.success('连接测试通过')
  } finally {
    testing.value = false
  }
}

async function handleSave() {
  await validateForm()
  if (!form.sourceName) {
    ElMessage.warning('保存模板前请填写模板名称')
    return
  }
  saving.value = true
  try {
    const payload: SaveDatasourcePayload = {
      id: editingId.value,
      name: form.sourceName,
      remark: form.remark,
      enabled: form.enabled,
      ...buildPayload()
    }
    const result = await saveDatasource(payload)
    editingId.value = result.id
    form.sourceName = result.name
    form.remark = result.remark || ''
    form.enabled = result.enabled ?? true
    form.password = ''
    await loadDatasourceList()
    ElMessage.success('模板已保存，保存前连接测试已通过')
  } finally {
    saving.value = false
  }
}

async function handlePreview() {
  await validateForm()
  previewing.value = true
  try {
    const result = await previewDocument(buildPayload())
    previewHtml.value = result.html || ''
    previewVisible.value = true
  } finally {
    previewing.value = false
  }
}

async function handleExport() {
  await validateForm()
  exporting.value = true
  try {
    const result = await exportDocument(buildPayload())
    saveBlob(result)
    ElMessage.success('文档导出成功')
  } finally {
    exporting.value = false
  }
}

async function applyDatasource(id: number) {
  const detail = await fetchDatasourceDetail(id)
  editingId.value = detail.id
  form.sourceName = detail.name
  form.remark = detail.remark || ''
  form.enabled = detail.enabled ?? true
  form.dbType = detail.dbType
  form.jdbcUrl = detail.jdbcUrl || ''
  form.host = detail.host
  form.port = detail.port
  form.database = detail.database
  form.schema = detail.schema || ''
  form.username = detail.username
  form.password = ''
  ElMessage.success('模板已填充，请重新输入密码后操作')
}

async function handleRemove(id: number) {
  try {
    await ElMessageBox.confirm('删除后模板将被逻辑移除，是否继续？', '删除模板', {
      type: 'warning'
    })
    await removeDatasource(id)
    if (editingId.value === id) {
      resetForm()
    }
    await loadDatasourceList()
    ElMessage.success('模板已删除')
  } catch (error: unknown) {
    if (error !== 'cancel' && error !== 'close') {
      throw error
    }
  }
}

function resetForm() {
  editingId.value = null
  form.sourceName = ''
  form.remark = ''
  form.enabled = true
  form.dbType = drivers.value[0]?.type || 'MYSQL'
  form.jdbcUrl = ''
  form.host = '127.0.0.1'
  form.port = drivers.value[0]?.defaultPort || 3306
  form.database = ''
  form.schema = ''
  form.username = ''
  form.password = ''
  form.title = ''
  form.format = 'HTML'
  form.useCache = true
  form.forceRefresh = false
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.page-shell {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border: 1px solid rgba(26, 76, 103, 0.12);
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(231, 242, 247, 0.92));
  box-shadow: 0 18px 45px rgba(32, 71, 88, 0.08);
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 13px;
  letter-spacing: 0.16em;
  color: #406a7d;
  text-transform: uppercase;
}

.hero-panel h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.15;
  color: #0f2530;
}

.hero-copy {
  max-width: 720px;
  margin: 14px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: #47616f;
}

.hero-stats {
  display: flex;
  gap: 14px;
  align-self: flex-start;
}

.stat-card {
  min-width: 132px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #0f2530;
  color: #f8fbfc;
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(248, 251, 252, 0.7);
}

.stat-card strong {
  font-size: 28px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(360px, 0.9fr);
  gap: 22px;
  margin-top: 24px;
}

.main-card,
.side-card {
  border: 1px solid rgba(23, 55, 69, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
}

.side-stack {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.card-head h2 {
  margin: 0;
  font-size: 20px;
  color: #163340;
}

.card-head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #607988;
}

.compact h2 {
  font-size: 18px;
}

.doc-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.span-2 {
  grid-column: span 2;
}

.option-row,
.driver-strip,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.driver-strip :deep(.el-tag) {
  margin-right: 0;
}

.datasource-list,
.driver-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.datasource-item,
.driver-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f4f8fb;
}

.datasource-info,
.driver-title,
.driver-meta {
  display: flex;
  flex-direction: column;
}

.datasource-info strong,
.driver-title strong {
  color: #173949;
}

.datasource-info span,
.driver-title span,
.driver-meta span {
  margin-top: 4px;
  font-size: 13px;
  color: #5f7885;
}

.datasource-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.preview-panel {
  max-height: 76vh;
  overflow-y: auto;
  padding: 8px;
  border-radius: 14px;
  background: #fff;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page-shell {
    padding: 18px 14px 28px;
  }

  .hero-panel {
    flex-direction: column;
    padding: 22px 20px;
  }

  .hero-stats {
    width: 100%;
  }

  .stat-card {
    flex: 1;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .datasource-item,
  .driver-item {
    flex-direction: column;
  }

  .datasource-actions {
    justify-content: flex-end;
  }
}
</style>
