/**
 * 导出向导页面逻辑。
 *
 * @author mumu
 * @date 2026-03-28
 */

import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  exportDocument,
  fetchDatasourceDetail,
  fetchDatasourceList,
  fetchDocumentCatalog,
  fetchDocumentOptions,
  fetchDrivers,
  previewDocument,
  saveDatasource,
  testDatasource
} from '../api/dbmeta'
import type { BlobResult } from '../utils/request'
import type {
  ConnectionPayload,
  DatasourceDetail,
  DocumentPayload,
  DocumentCatalogResponse,
  DocumentOptionsResponse,
  DriverInfo,
  SaveDatasourcePayload
} from '../api/dbmeta'
import { parseJdbcUrl } from '../utils/jdbc-url'

const DEFAULT_HOST = '127.0.0.1'
const DEFAULT_DB_TYPE = 'MYSQL'
const DEFAULT_PORT = 3306
const DEFAULT_FORMAT = 'HTML'
const SOURCE_MODE_TEMPLATE = 'template'
const SOURCE_MODE_MANUAL = 'manual'
export const BOOLEAN_DISPLAY_SYMBOL = 'SYMBOL'
export const BOOLEAN_DISPLAY_TEXT = 'TEXT'

export const documentFormats = [
  { label: 'HTML', value: 'HTML' },
  { label: 'Markdown', value: 'MARKDOWN' },
  { label: 'PDF', value: 'PDF' },
  { label: 'Word', value: 'WORD' },
  { label: 'Excel', value: 'EXCEL' }
] as const
export const booleanDisplayOptions = [
  { label: '√ / ×', value: BOOLEAN_DISPLAY_SYMBOL },
  { label: '是 / 否', value: BOOLEAN_DISPLAY_TEXT }
] as const

type SourceMode = typeof SOURCE_MODE_TEMPLATE | typeof SOURCE_MODE_MANUAL
type BooleanDisplayStyle = typeof BOOLEAN_DISPLAY_SYMBOL | typeof BOOLEAN_DISPLAY_TEXT

interface JdbcFeedback {
  tone: 'success' | 'warning'
  title: string
  detail: string
}

interface WizardFormModel {
  datasourceId: number | null
  templateName: string
  remark: string
  enabled: boolean
  rememberPassword: boolean
  useStoredPassword: boolean
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
  fontPreset: string
  booleanDisplayStyle: BooleanDisplayStyle
  useCache: boolean
  forceRefresh: boolean
  selectedTableKeys: string[]
  exportSections: string[]
}

export function normalizeBooleanDisplayStyle(value?: string): BooleanDisplayStyle {
  return value?.toUpperCase() === BOOLEAN_DISPLAY_TEXT ? BOOLEAN_DISPLAY_TEXT : BOOLEAN_DISPLAY_SYMBOL
}

export function createDefaultForm(): WizardFormModel {
  return {
    datasourceId: null,
    templateName: '',
    remark: '',
    enabled: true,
    rememberPassword: false,
    useStoredPassword: false,
    dbType: DEFAULT_DB_TYPE,
    jdbcUrl: '',
    host: DEFAULT_HOST,
    port: DEFAULT_PORT,
    database: '',
    schema: '',
    username: '',
    password: '',
    title: '',
    format: DEFAULT_FORMAT,
    fontPreset: '',
    booleanDisplayStyle: BOOLEAN_DISPLAY_SYMBOL,
    useCache: true,
    forceRefresh: false,
    selectedTableKeys: [],
    exportSections: []
  }
}

function normalizeText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : undefined
}

function hasExplicitSchemaParameter(parameters: Record<string, string>) {
  const schemaKeys = ['schema', 'currentschema', 'current_schema']
  return Object.keys(parameters).some(key => schemaKeys.includes(key.toLowerCase()))
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

function parseRouteDatasourceId(value: unknown) {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (!rawValue) {
    return null
  }
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

export function buildPreviewDependencyKey(form: Pick<WizardFormModel,
  'selectedTableKeys' | 'exportSections' | 'fontPreset' | 'title' | 'format' | 'booleanDisplayStyle'>) {
  return [
    form.selectedTableKeys.join('|'),
    form.exportSections.join('|'),
    form.fontPreset,
    form.title,
    form.format,
    normalizeBooleanDisplayStyle(form.booleanDisplayStyle)
  ].join('||')
}

export function buildConnectionPayloadFromForm(
  form: Pick<WizardFormModel,
  'datasourceId' | 'dbType' | 'jdbcUrl' | 'host' | 'port' | 'database' | 'schema' | 'username' | 'password' | 'useCache' | 'forceRefresh'>,
  sourceMode: SourceMode,
  canUseStoredPassword: boolean,
  useStoredPassword: boolean
): ConnectionPayload {
  return {
    datasourceId: sourceMode === SOURCE_MODE_TEMPLATE ? form.datasourceId : null,
    dbType: form.dbType,
    jdbcUrl: normalizeText(form.jdbcUrl),
    host: normalizeText(form.host),
    port: form.port,
    database: normalizeText(form.database),
    schema: normalizeText(form.schema),
    username: normalizeText(form.username),
    password: normalizeText(form.password),
    useStoredPassword: canUseStoredPassword ? useStoredPassword : false,
    useCache: form.useCache,
    forceRefresh: form.forceRefresh
  }
}

export function buildDocumentPayloadFromForm(
  form: WizardFormModel,
  sourceMode: SourceMode,
  canUseStoredPassword: boolean
): DocumentPayload {
  return {
    ...buildConnectionPayloadFromForm(form, sourceMode, canUseStoredPassword, form.useStoredPassword),
    title: normalizeText(form.title),
    format: form.format,
    selectedTableKeys: [...form.selectedTableKeys],
    exportSections: [...form.exportSections],
    fontPreset: form.fontPreset,
    booleanDisplayStyle: normalizeBooleanDisplayStyle(form.booleanDisplayStyle)
  }
}

export function useExportWizardPage() {
  const route = useRoute()
  const router = useRouter()
  const sourceFormRef = ref<FormInstance>()
  const pageLoading = ref(false)
  const testing = ref(false)
  const saving = ref(false)
  const catalogLoading = ref(false)
  const previewing = ref(false)
  const exporting = ref(false)
  const activeStep = ref(0)
  const sourceMode = ref<SourceMode>(SOURCE_MODE_TEMPLATE)
  const previewHtml = ref('')
  const jdbcFeedback = ref<JdbcFeedback | null>(null)
  const drivers = ref<DriverInfo[]>([])
  const datasourceList = ref<DatasourceDetail[]>([])
  const documentOptions = ref<DocumentOptionsResponse | null>(null)
  const catalog = ref<DocumentCatalogResponse | null>(null)
  const previewEverLoaded = ref(false)
  let previewTimer: number | undefined

  const form = reactive<WizardFormModel>(createDefaultForm())

  const rules: FormRules<WizardFormModel> = {
    dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
    host: [{
      validator: (_rule, value, callback) => {
        if (normalizeText(value) || normalizeText(form.jdbcUrl)) {
          callback()
          return
        }
        callback(new Error('请输入主机地址，或填写 JDBC URL'))
      },
      trigger: 'blur'
    }],
    database: [{
      validator: (_rule, value, callback) => {
        if (normalizeText(value) || normalizeText(form.jdbcUrl)) {
          callback()
          return
        }
        callback(new Error('请输入数据库名，或填写 JDBC URL'))
      },
      trigger: 'blur'
    }],
    username: [{
      validator: (_rule, value, callback) => {
        if (normalizeText(value)) {
          callback()
          return
        }
        callback(new Error('请输入用户名'))
      },
      trigger: 'blur'
    }],
    password: [{
      validator: (_rule, value, callback) => {
        if (form.useStoredPassword || normalizeText(value)) {
          callback()
          return
        }
        callback(new Error('请输入密码，或启用已保存密码'))
      },
      trigger: 'blur'
    }]
  }

  const activeDriver = computed(() => drivers.value.find(item => item.type === form.dbType) || null)
  const currentTemplate = computed(() => datasourceList.value.find(item => item.id === form.datasourceId) || null)
  const canUseStoredPassword = computed(() => sourceMode.value === SOURCE_MODE_TEMPLATE && Boolean(currentTemplate.value?.passwordSaved))
  const availableTables = computed(() => catalog.value?.tables || [])
  const selectedTableCount = computed(() => form.selectedTableKeys.length)

  onMounted(() => {
    void initialize()
  })

  watch(
    () => route.query.datasourceId,
    value => {
      const datasourceId = parseRouteDatasourceId(value)
      if (datasourceId && datasourceId !== form.datasourceId) {
        void applyDatasource(datasourceId, false)
      }
    }
  )

  watch(
    () => buildPreviewDependencyKey(form),
    () => {
      if (activeStep.value === 2 && previewEverLoaded.value) {
        schedulePreview()
      }
    }
  )

  watch(canUseStoredPassword, value => {
    if (!value) {
      form.useStoredPassword = false
    }
  })

  async function initialize() {
    pageLoading.value = true
    try {
      const [driverResult, datasourceResult, optionResult] = await Promise.all([
        fetchDrivers(),
        fetchDatasourceList(),
        fetchDocumentOptions()
      ])
      drivers.value = driverResult
      datasourceList.value = datasourceResult
      documentOptions.value = optionResult
      applyDriverDefaults(activeDriver.value || drivers.value[0] || null)
      form.fontPreset = optionResult.defaultFontPreset || ''
      form.exportSections = [...optionResult.defaultExportSections]
      const datasourceId = parseRouteDatasourceId(route.query.datasourceId)
      if (datasourceId) {
        await applyDatasource(datasourceId, false)
      } else {
        sourceMode.value = SOURCE_MODE_MANUAL
      }
    } finally {
      pageLoading.value = false
    }
  }

  function applyDriverDefaults(driver: DriverInfo | null) {
    if (!driver) {
      return
    }
    form.dbType = driver.type
    form.port = driver.defaultPort
  }

  function handleDriverChange(type: string) {
    clearJdbcFeedback()
    const driver = drivers.value.find(item => item.type === type) || null
    applyDriverDefaults(driver)
  }

  function handleSourceModeChange(mode: SourceMode) {
    sourceMode.value = mode
    clearJdbcFeedback()
    if (mode === SOURCE_MODE_MANUAL) {
      resetForManualEntry()
      void router.replace({ path: '/export' })
    }
  }

  function handleTemplateSelection(value: number | string | undefined) {
    if (value === undefined || value === null || value === '') {
      resetForManualEntry()
      return
    }
    void applyDatasource(Number(value))
  }

  async function applyDatasource(datasourceId: number, showMessage = true) {
    const detail = await fetchDatasourceDetail(datasourceId)
    sourceMode.value = SOURCE_MODE_TEMPLATE
    form.datasourceId = detail.id
    form.templateName = detail.name
    form.remark = detail.remark || ''
    form.enabled = detail.enabled ?? true
    form.rememberPassword = Boolean(detail.passwordSaved)
    form.useStoredPassword = Boolean(detail.passwordSaved)
    form.dbType = detail.dbType
    form.jdbcUrl = detail.jdbcUrl || ''
    form.host = detail.host || DEFAULT_HOST
    form.port = detail.port || resolveDefaultPort(detail.dbType)
    form.database = detail.database || ''
    form.schema = detail.schema || ''
    form.username = detail.username || ''
    form.password = ''
    clearJdbcFeedback()
    clearDocumentState()
    await router.replace({ path: '/export', query: { datasourceId: String(detail.id) } })
    if (showMessage) {
      ElMessage.success('模板已载入，可继续测试连接或直接加载表清单')
    }
  }

  function resetForManualEntry() {
    const nextForm = createDefaultForm()
    nextForm.dbType = activeDriver.value?.type || DEFAULT_DB_TYPE
    nextForm.port = activeDriver.value?.defaultPort || DEFAULT_PORT
    nextForm.fontPreset = documentOptions.value?.defaultFontPreset || ''
    nextForm.exportSections = [...(documentOptions.value?.defaultExportSections || [])]
    Object.assign(form, nextForm)
    clearJdbcFeedback()
    sourceFormRef.value?.clearValidate()
    clearDocumentState()
  }

  function clearDocumentState() {
    catalog.value = null
    previewHtml.value = ''
    previewEverLoaded.value = false
    form.selectedTableKeys = []
  }

  function resolveDefaultPort(dbType: string) {
    return drivers.value.find(item => item.type === dbType)?.defaultPort || DEFAULT_PORT
  }

  function handleParseJdbcUrl() {
    if (!normalizeText(form.jdbcUrl)) {
      ElMessage.warning('请先粘贴 JDBC URL')
      return
    }
    try {
      const parsed = parseJdbcUrl(form.jdbcUrl)
      const parsedDbType = parsed.dbType
      if (!parsedDbType) {
        throw new Error(parsed.unsupportedReason || '当前 JDBC URL 暂不支持自动识别数据库类型')
      }
      if (parsedDbType !== form.dbType) {
        handleDriverChange(parsedDbType)
      }
      form.dbType = parsedDbType
      if (parsed.host) {
        form.host = parsed.host
      }
      if (parsed.port) {
        form.port = parsed.port
      }
      if (parsed.database) {
        form.database = parsed.database
      }
      if (parsed.schema !== undefined && (!normalizeText(form.schema) || hasExplicitSchemaParameter(parsed.parameters))) {
        form.schema = parsed.schema
      }
      jdbcFeedback.value = {
        tone: 'success',
        title: `已识别为 ${resolveDriverLabel(parsedDbType)}`,
        detail: buildJdbcFeedbackDetail(parsed.host, parsed.port, parsed.database, parsed.schema)
      }
      ElMessage.success('JDBC URL 已解析并回填')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'JDBC URL 解析失败'
      jdbcFeedback.value = {
        tone: 'warning',
        title: 'JDBC 解析未完成',
        detail: message
      }
      ElMessage.error(message)
    }
  }

  async function validateSourceForm() {
    await sourceFormRef.value?.validate()
  }

  function buildConnectionPayload(): ConnectionPayload {
    return buildConnectionPayloadFromForm(form, sourceMode.value, canUseStoredPassword.value, form.useStoredPassword)
  }

  function buildDocumentPayload(): DocumentPayload {
    return buildDocumentPayloadFromForm(form, sourceMode.value, canUseStoredPassword.value)
  }

  async function handleTestConnection(showMessage = true) {
    await validateSourceForm()
    testing.value = true
    try {
      await testDatasource(buildConnectionPayload())
      if (showMessage) {
        ElMessage.success('连接测试通过')
      }
    } finally {
      testing.value = false
    }
  }

  async function loadCatalog(showMessage = false) {
    await validateSourceForm()
    catalogLoading.value = true
    try {
      const result = await fetchDocumentCatalog(buildConnectionPayload())
      catalog.value = result
      syncSelectedTables(result)
      previewHtml.value = ''
      previewEverLoaded.value = false
      if (showMessage) {
        ElMessage.success(`已加载 ${result.tableCount} 张表到表清单`)
      }
      return result
    } finally {
      catalogLoading.value = false
    }
  }

  function syncSelectedTables(result: DocumentCatalogResponse) {
    const availableKeys = new Set(result.tables.map(item => item.key))
    const currentSelected = form.selectedTableKeys.filter(key => availableKeys.has(key))
    form.selectedTableKeys = currentSelected.length ? currentSelected : result.tables.map(item => item.key)
  }

  async function handleContinueToContent() {
    await handleTestConnection(false)
    await loadCatalog()
    activeStep.value = 1
    ElMessage.success('表清单已准备完成，请选择导出范围')
  }

  async function handleSaveTemplate() {
    await validateSourceForm()
    const templateName = normalizeText(form.templateName)
    if (!templateName) {
      ElMessage.warning('保存模板前请填写模板名称')
      return
    }
    saving.value = true
    try {
      const payload: SaveDatasourcePayload = {
        ...buildConnectionPayload(),
        id: sourceMode.value === SOURCE_MODE_TEMPLATE ? form.datasourceId : null,
        name: templateName,
        remark: normalizeText(form.remark),
        enabled: form.enabled,
        rememberPassword: form.rememberPassword
      }
      const result = await saveDatasource(payload)
      datasourceList.value = await fetchDatasourceList()
      sourceMode.value = SOURCE_MODE_TEMPLATE
      form.datasourceId = result.id
      form.templateName = result.name
      form.rememberPassword = Boolean(result.passwordSaved)
      form.useStoredPassword = Boolean(result.passwordSaved)
      form.password = ''
      await router.replace({ path: '/export', query: { datasourceId: String(result.id) } })
      ElMessage.success('模板已保存，保存前连接测试已在服务端完成')
    } finally {
      saving.value = false
    }
  }

  function handleSelectAllTables() {
    form.selectedTableKeys = availableTables.value.map(item => item.key)
  }

  function handleClearTables() {
    form.selectedTableKeys = []
  }

  function handleResetSections() {
    form.exportSections = [...(documentOptions.value?.defaultExportSections || [])]
  }

  function handleSelectAllSections() {
    form.exportSections = [...(documentOptions.value?.exportSections.map(item => item.code) || [])]
  }

  async function handleContinueToPreview() {
    if (!form.selectedTableKeys.length) {
      ElMessage.warning('请至少选择一张表')
      return
    }
    activeStep.value = 2
    await handlePreview(true)
  }

  async function handlePreview(showMessage = false) {
    if (!form.selectedTableKeys.length) {
      ElMessage.warning('请至少选择一张表')
      return
    }
    previewing.value = true
    try {
      const result = await previewDocument(buildDocumentPayload())
      previewHtml.value = result.html
      previewEverLoaded.value = true
      if (showMessage) {
        ElMessage.success('预览已刷新')
      }
    } finally {
      previewing.value = false
    }
  }

  async function handleExport() {
    if (!form.selectedTableKeys.length) {
      ElMessage.warning('请至少选择一张表')
      return
    }
    exporting.value = true
    try {
      const result = await exportDocument(buildDocumentPayload())
      saveBlob(result)
      ElMessage.success('文档导出成功')
    } finally {
      exporting.value = false
    }
  }

  function schedulePreview() {
    if (previewTimer) {
      window.clearTimeout(previewTimer)
    }
    previewTimer = window.setTimeout(() => {
      void handlePreview()
    }, 350)
  }

  function handleBackToTemplates() {
    void router.push('/')
  }

  function resolveDriverLabel(dbType: string) {
    return drivers.value.find(item => item.type === dbType)?.label || dbType || '未选择'
  }

  function buildJdbcFeedbackDetail(host?: string, port?: number, database?: string, schema?: string) {
    const segments = [
      host ? `主机 ${host}${port ? `:${port}` : ''}` : '',
      database ? `数据库 ${database}` : '',
      schema ? `Schema ${schema}` : ''
    ].filter(Boolean)
    return segments.length ? segments.join(' · ') : '已按 JDBC URL 回填可识别字段'
  }

  function clearJdbcFeedback() {
    jdbcFeedback.value = null
  }

  return {
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
    availableTables,
    previewHtml,
    jdbcFeedback,
    canUseStoredPassword,
    selectedTableCount,
    documentFormats,
    booleanDisplayOptions,
    handleDriverChange,
    handleSourceModeChange,
    handleTemplateSelection,
    handleParseJdbcUrl,
    handleTestConnection,
    handleContinueToContent,
    handleSaveTemplate,
    applyDatasource,
    resetForManualEntry,
    handleSelectAllTables,
    handleClearTables,
    handleResetSections,
    handleSelectAllSections,
    loadCatalog,
    handleContinueToPreview,
    handlePreview,
    handleExport,
    handleBackToTemplates
  }
}
