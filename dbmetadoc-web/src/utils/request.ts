/**
 * 前端请求工具。
 *
 * @author mumu
 * @date 2026-03-28
 */

import axios, { AxiosError } from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'

const REQUEST_TIMEOUT_MS = 30_000
const SUCCESS_CODE = 200
const DEFAULT_DOWNLOAD_FILE_NAME = 'download.bin'
const MULTIPART_CONTENT_TYPE = 'multipart/form-data'
const JSON_CONTENT_TYPE = 'application/json'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface BlobResult {
  blob: Blob
  fileName: string
  contentType: string
}

const http: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: REQUEST_TIMEOUT_MS
})

http.interceptors.response.use(
  response => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

function isWrappedResponse<T>(payload: ApiResponse<T> | T): payload is ApiResponse<T> {
  return Boolean(payload && typeof payload === 'object' && 'code' in payload)
}

function unwrapResponse<T>(payload: ApiResponse<T> | T): T {
  if (!isWrappedResponse(payload)) {
    return payload as T
  }
  if (payload.code !== SUCCESS_CODE) {
    throw new Error(payload.message || '请求失败')
  }
  return payload.data as T
}

function extractFilename(contentDisposition?: string) {
  if (!contentDisposition) {
    return DEFAULT_DOWNLOAD_FILE_NAME
  }
  const match = contentDisposition.match(/filename\*?=(?:UTF-8''|")?([^\";]+)/i)
  return match ? decodeURIComponent(match[1].replace(/"/g, '')) : DEFAULT_DOWNLOAD_FILE_NAME
}

async function parseBlobError(blob: Blob) {
  const text = await blob.text()
  try {
    const json = JSON.parse(text)
    throw new Error(json.message || '请求失败')
  } catch (error) {
    if (error instanceof SyntaxError) {
      throw new Error(text || '请求失败')
    }
    throw error
  }
}

function buildMultipartConfig() {
  return {
    headers: {
      'Content-Type': MULTIPART_CONTENT_TYPE
    }
  }
}

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const { data } = await http.get(url, { params })
  return unwrapResponse<T>(data)
}

export async function post<T>(url: string, body?: unknown): Promise<T> {
  const { data } = await http.post(url, body)
  return unwrapResponse<T>(data)
}

export async function postBlob(url: string, body?: unknown): Promise<BlobResult> {
  const response: AxiosResponse<Blob> = await http.post(url, body, { responseType: 'blob' })
  const contentType = response.headers['content-type'] || ''
  if (contentType.includes(JSON_CONTENT_TYPE)) {
    await parseBlobError(response.data)
  }
  return {
    blob: response.data,
    fileName: extractFilename(response.headers['content-disposition']),
    contentType
  }
}

export async function postFormData<T>(url: string, payload: FormData | Record<string, unknown>): Promise<T> {
  const formData = payload instanceof FormData ? payload : buildFormData(payload)
  const { data } = await http.post(url, formData, buildMultipartConfig())
  return unwrapResponse<T>(data)
}

function appendFormValue(formData: FormData, key: string, value: unknown) {
  if (value instanceof Blob) {
    formData.append(key, value)
    return
  }
  formData.append(key, String(value))
}

export function buildFormData(payload: Record<string, unknown> = {}): FormData {
  const formData = new FormData()
  Object.entries(payload).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }
    if (Array.isArray(value)) {
      value.forEach(item => appendFormValue(formData, key, item))
      return
    }
    appendFormValue(formData, key, value)
  })
  return formData
}
