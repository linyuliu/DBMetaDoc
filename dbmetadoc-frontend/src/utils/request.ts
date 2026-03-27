import axios from 'axios'
import type { AxiosResponse } from 'axios'

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

const http = axios.create({
  baseURL: '/',
  timeout: 30000
})

function unwrapResponse<T>(payload: ApiResponse<T> | T): T {
  if (payload && typeof payload === 'object' && 'code' in payload) {
    if (payload.code === 200) {
      return payload.data as T
    }
    throw new Error(payload.message || '请求失败')
  }
  return payload as T
}

function extractFilename(contentDisposition?: string) {
  if (!contentDisposition) {
    return 'download.bin'
  }
  const match = contentDisposition.match(/filename\*?=(?:UTF-8''|")?([^\";]+)/i)
  return match ? decodeURIComponent(match[1].replace(/"/g, '')) : 'download.bin'
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
  if (contentType.includes('application/json')) {
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
  const { data } = await http.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
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
