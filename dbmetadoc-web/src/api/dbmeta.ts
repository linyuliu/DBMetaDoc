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
  host: string
  port: number
  database: string
  schema?: string
  username: string
  driverClass: string
  testSql: string
  remark?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface DocumentPayload {
  dbType: string
  jdbcUrl?: string
  host: string
  port: number
  database: string
  schema?: string
  username: string
  password: string
  title?: string
  format: string
  useCache: boolean
  forceRefresh: boolean
}

export interface PreviewResponse {
  title: string
  html: string
}

export interface SaveDatasourcePayload extends DocumentPayload {
  id?: number | null
  name: string
  remark?: string
  enabled?: boolean
}

export const fetchDrivers = () => get<DriverInfo[]>('/api/drivers')
export const fetchDatasourceList = () => get<DatasourceDetail[]>('/api/datasource/list')
export const fetchDatasourceDetail = (id: number) => get<DatasourceDetail>('/api/datasource/detail', { id })
export const testDatasource = (payload: DocumentPayload) => post<void>('/api/datasource/test', payload)
export const saveDatasource = (payload: SaveDatasourcePayload) => post<DatasourceDetail>('/api/datasource/save', payload)
export const removeDatasource = (id: number) => post<void>('/api/datasource/remove', { id })
export const previewDocument = (payload: DocumentPayload) => post<PreviewResponse>('/api/document/preview', payload)
export const exportDocument = (payload: DocumentPayload): Promise<BlobResult> => postBlob('/api/document/export', payload)

// 预留上传/混合表单能力，当前业务暂未接入后端 @RequestPart 接口
export const postMultipart = <T>(url: string, payload: FormData | Record<string, unknown>) => postFormData<T>(url, payload)
