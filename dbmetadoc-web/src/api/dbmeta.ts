import { get, post, postBlob, postFormData } from '../utils/request'
import type { BlobResult } from '../utils/request'

export interface DriverInfo {
  type: string
  label: string
  defaultPort: number
  driverClass: string
  testSql: string
  domestic: boolean
  mysqlLike: boolean
  pgLike: boolean
  oracleLike: boolean
  supportsDatabase: boolean
  supportsSchema: boolean
  supportsJdbcUrl: boolean
  metadataStrategy: string
}

export interface DatasourceDetail {
  id: number
  name: string
  dbType: string
  jdbcUrl?: string | null
  host?: string
  port?: number
  database?: string
  schema?: string | null
  username?: string
  driverClass: string
  testSql: string
  remark?: string | null
  enabled?: boolean
  passwordSaved?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface ConnectionPayload {
  datasourceId?: number | null
  dbType?: string
  jdbcUrl?: string
  host?: string
  port?: number
  database?: string
  schema?: string
  username?: string
  password?: string
  useStoredPassword?: boolean
  useCache?: boolean
  forceRefresh?: boolean
}

export interface DocumentPayload extends ConnectionPayload {
  title?: string
  format: string
  selectedTableKeys?: string[]
  exportSections?: string[]
  fontPreset?: string
}

export interface PreviewResponse {
  title: string
  html: string
}

export interface SaveDatasourcePayload extends ConnectionPayload {
  id?: number | null
  name: string
  driverClass?: string
  remark?: string
  enabled?: boolean
  rememberPassword?: boolean
}

export interface TableOption {
  key: string
  name: string
  schema?: string | null
  comment?: string | null
  columnCount: number
}

export interface DocumentCatalogResponse {
  databaseName?: string
  schemaName?: string | null
  tableCount: number
  tables: TableOption[]
}

export interface FontPreset {
  code: string
  label: string
  titleFont: string
  bodyFont: string
  monoFont: string
}

export interface ExportSectionOption {
  code: string
  label: string
  description: string
}

export interface DocumentOptionsResponse {
  defaultFontPreset: string
  fontPresets: FontPreset[]
  exportSections: ExportSectionOption[]
  defaultExportSections: string[]
}

export const fetchDrivers = () => get<DriverInfo[]>('/api/drivers')
export const fetchDatasourceList = () => get<DatasourceDetail[]>('/api/datasource/list')
export const fetchDatasourceDetail = (id: number) => get<DatasourceDetail>('/api/datasource/detail', { id })
export const testDatasource = (payload: ConnectionPayload) => post<void>('/api/datasource/test', payload)
export const saveDatasource = (payload: SaveDatasourcePayload) => post<DatasourceDetail>('/api/datasource/save', payload)
export const removeDatasource = (id: number) => post<void>('/api/datasource/remove', { id })
export const fetchDocumentOptions = () => get<DocumentOptionsResponse>('/api/document/options')
export const fetchDocumentCatalog = (payload: ConnectionPayload) => post<DocumentCatalogResponse>('/api/document/catalog', payload)
export const previewDocument = (payload: DocumentPayload) => post<PreviewResponse>('/api/document/preview', payload)
export const exportDocument = (payload: DocumentPayload): Promise<BlobResult> => postBlob('/api/document/export', payload)

// 预留上传/混合表单能力，当前业务暂未接入后端 @RequestPart 接口
export const postMultipart = <T>(url: string, payload: FormData | Record<string, unknown>) => postFormData<T>(url, payload)
