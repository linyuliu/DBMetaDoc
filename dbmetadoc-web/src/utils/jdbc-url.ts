/**
 * JDBC URL 前端解析工具，用于将常见数据库连接串回填到表单。
 *
 * @author mumu
 * @date 2026-03-28
 */

const JDBC_PROTOCOL = 'jdbc:'
const DEFAULT_SCHEMAS = {
  POSTGRESQL: 'public'
} as const

const ORACLE_SERVICE_PATTERN =
  /^jdbc:oracle:thin:@\/\/(?<host>[^:/?#,]+)(?::(?<port>\d+))?\/(?<database>[^?]+)(?:\?(?<query>.*))?$/i

const ORACLE_SID_PATTERN =
  /^jdbc:oracle:thin:@(?<host>[^:/?#,]+)(?::(?<port>\d+))?:(?<database>[^?]+)(?:\?(?<query>.*))?$/i

export type JdbcDbType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'KINGBASE' | 'DAMENG'

export interface JdbcUrlParseResult {
  dbType: JdbcDbType
  host?: string
  port?: number
  database?: string
  schema?: string
  serviceNameOrSid?: string
  sidMode?: boolean
  parameters: Record<string, string>
}

export function parseJdbcUrl(jdbcUrl: string): JdbcUrlParseResult {
  const normalizedUrl = jdbcUrl.trim()
  if (!normalizedUrl.startsWith(JDBC_PROTOCOL)) {
    throw new Error('JDBC URL 必须以 jdbc: 开头')
  }

  if (normalizedUrl.startsWith('jdbc:mysql:')) {
    return parseUriStyle('MYSQL', normalizedUrl, 'jdbc:mysql:', [])
  }
  if (normalizedUrl.startsWith('jdbc:postgresql:')) {
    return parseUriStyle('POSTGRESQL', normalizedUrl, 'jdbc:postgresql:', ['currentSchema', 'current_schema'])
  }
  if (normalizedUrl.startsWith('jdbc:kingbase8:')) {
    return parseUriStyle('KINGBASE', normalizedUrl, 'jdbc:kingbase8:', ['currentSchema', 'current_schema'])
  }
  if (normalizedUrl.startsWith('jdbc:dm:')) {
    return parseUriStyle('DAMENG', normalizedUrl, 'jdbc:dm:', ['schema'])
  }
  if (normalizedUrl.startsWith('jdbc:oracle:thin:@')) {
    return parseOracleUrl(normalizedUrl)
  }

  throw new Error('暂不支持识别该 JDBC URL')
}

function parseUriStyle(
  dbType: JdbcDbType,
  jdbcUrl: string,
  prefix: string,
  schemaKeys: string[]
): JdbcUrlParseResult {
  const body = jdbcUrl.slice(prefix.length)
  const url = new URL(`jdbc-parser:${body}`)
  const parameters = parseQuery(url.searchParams)
  const schema = pickParameter(parameters, schemaKeys) || DEFAULT_SCHEMAS[dbType as keyof typeof DEFAULT_SCHEMAS]
  return {
    dbType,
    host: firstHost(url.hostname),
    port: parsePort(url.port),
    database: normalizePath(url.pathname),
    schema,
    serviceNameOrSid: normalizePath(url.pathname),
    parameters
  }
}

function parseOracleUrl(jdbcUrl: string): JdbcUrlParseResult {
  const serviceMatch = jdbcUrl.match(ORACLE_SERVICE_PATTERN)
  if (serviceMatch?.groups) {
    return {
      dbType: 'ORACLE',
      host: serviceMatch.groups.host,
      port: parsePort(serviceMatch.groups.port),
      database: serviceMatch.groups.database?.trim(),
      serviceNameOrSid: serviceMatch.groups.database?.trim(),
      sidMode: false,
      parameters: parseRawQuery(serviceMatch.groups.query)
    }
  }

  const sidMatch = jdbcUrl.match(ORACLE_SID_PATTERN)
  if (sidMatch?.groups) {
    return {
      dbType: 'ORACLE',
      host: sidMatch.groups.host,
      port: parsePort(sidMatch.groups.port),
      database: sidMatch.groups.database?.trim(),
      serviceNameOrSid: sidMatch.groups.database?.trim(),
      sidMode: true,
      parameters: parseRawQuery(sidMatch.groups.query)
    }
  }

  throw new Error('Oracle JDBC URL 仅支持 service name 或 SID 两种常见形式')
}

function parseQuery(searchParams: URLSearchParams): Record<string, string> {
  const parameters: Record<string, string> = {}
  searchParams.forEach((value, key) => {
    parameters[key] = value
  })
  return parameters
}

function parseRawQuery(query?: string): Record<string, string> {
  if (!query) {
    return {}
  }
  return parseQuery(new URLSearchParams(query))
}

function pickParameter(parameters: Record<string, string>, keys: string[]): string | undefined {
  for (const key of keys) {
    const exact = parameters[key]
    if (exact) {
      return exact.trim()
    }
    const caseInsensitiveKey = Object.keys(parameters).find(item => item.toLowerCase() === key.toLowerCase())
    if (caseInsensitiveKey && parameters[caseInsensitiveKey]) {
      return parameters[caseInsensitiveKey].trim()
    }
  }
  return undefined
}

function normalizePath(pathname: string): string | undefined {
  const trimmed = pathname.replace(/^\/+/, '').trim()
  return trimmed || undefined
}

function parsePort(port?: string): number | undefined {
  if (!port) {
    return undefined
  }
  const parsed = Number(port)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

function firstHost(host?: string): string | undefined {
  if (!host) {
    return undefined
  }
  return host.split(',')[0]?.trim() || undefined
}
