/**
 * JDBC URL 前端解析工具。
 * 规则表与 parser kind 保持和后端同一套思路，方便前后端长期对齐。
 *
 * @author mumu
 * @date 2026-04-01
 */

const JDBC_PROTOCOL = 'jdbc:'
const JDBC_WRAPPER_PREFIXES = ['jdbc:log4jdbc:'] as const
const PG_SCHEMA_KEYS = ['currentSchema', 'current_schema']
const DM_SCHEMA_KEYS = ['schema']
const DEFAULT_SCHEMAS = {
  POSTGRESQL: 'public'
} as const

const ORACLE_SERVICE_PATTERN =
  /^\/\/(?<host>[^:/?#,;]+)(?::(?<port>\d+))?\/(?<database>[^?;]+)(?<rest>[?;].*)?$/i

const ORACLE_SID_PATTERN =
  /^(?<host>[^:/?#,;]+)(?::(?<port>\d+))?:(?<database>[^?;]+)(?<rest>[?;].*)?$/i

const ORACLE_DESCRIPTION_HOST_PATTERN = /\(HOST\s*=\s*(?<value>[^)]+)\)/i
const ORACLE_DESCRIPTION_PORT_PATTERN = /\(PORT\s*=\s*(?<value>\d+)\)/i
const ORACLE_DESCRIPTION_SERVICE_PATTERN = /\(SERVICE_NAME\s*=\s*(?<value>[^)]+)\)/i
const ORACLE_DESCRIPTION_SID_PATTERN = /\(SID\s*=\s*(?<value>[^)]+)\)/i

export type JdbcDbType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'KINGBASE' | 'DAMENG'
export type JdbcVendorCode =
  | 'MYSQL'
  | 'MARIADB'
  | 'TIDB'
  | 'POSTGRESQL'
  | 'EDB'
  | 'ORACLE'
  | 'ALIBABA_ORACLE'
  | 'KINGBASE'
  | 'DAMENG'
  | 'SQLSERVER'
  | 'CLICKHOUSE'
  | 'DB2'
  | 'HIVE'
  | 'PHOENIX'
  | 'KYLIN'
  | 'GBASE'
  | 'XUGU'
  | 'OCEANBASE'

type ParserKind = 'URI' | 'ORACLE' | 'SQLSERVER' | 'UNSUPPORTED'

interface JdbcVendorRule {
  vendorCode: JdbcVendorCode
  prefixes: string[]
  parserKind: ParserKind
  mappedDbType?: JdbcDbType
  supported: boolean
  schemaKeys?: string[]
  unsupportedReason?: string
}

interface JdbcRuleMatch {
  rule: JdbcVendorRule
  matchedPrefix: string
}

interface ParsedPath {
  path: string
  parameterFragment: string
}

interface HostPort {
  host?: string
  port?: number
}

export interface JdbcUrlParseResult {
  dbType?: JdbcDbType
  vendorCode: JdbcVendorCode
  matchedPrefix: string
  mappedDbType?: JdbcDbType
  supported: boolean
  unsupportedReason?: string
  host?: string
  port?: number
  database?: string
  schema?: string
  serviceNameOrSid?: string
  sidMode?: boolean
  parameters: Record<string, string>
}

function supportedRule(
  vendorCode: JdbcVendorCode,
  prefixes: string[],
  parserKind: ParserKind,
  mappedDbType: JdbcDbType,
  schemaKeys?: string[]
): JdbcVendorRule {
  return {
    vendorCode,
    prefixes,
    parserKind,
    mappedDbType,
    supported: true,
    schemaKeys
  }
}

function unsupportedRule(
  vendorCode: JdbcVendorCode,
  prefixes: string[],
  parserKind: ParserKind,
  unsupportedReason: string
): JdbcVendorRule {
  return {
    vendorCode,
    prefixes,
    parserKind,
    supported: false,
    unsupportedReason
  }
}

const JDBC_VENDOR_RULES: JdbcVendorRule[] = [
  supportedRule('ALIBABA_ORACLE', ['jdbc:alibaba:oracle:'], 'ORACLE', 'ORACLE'),
  supportedRule('ORACLE', ['jdbc:oracle:thin:@'], 'ORACLE', 'ORACLE'),
  supportedRule('POSTGRESQL', ['jdbc:postgresql:'], 'URI', 'POSTGRESQL', PG_SCHEMA_KEYS),
  supportedRule('KINGBASE', ['jdbc:kingbase8:', 'jdbc:kingbase:'], 'URI', 'KINGBASE', PG_SCHEMA_KEYS),
  supportedRule('MARIADB', ['jdbc:mariadb:'], 'URI', 'MYSQL'),
  supportedRule('MYSQL', ['jdbc:mysql+srv:loadbalance:', 'jdbc:mysql+srv:replication:', 'jdbc:mysql+srv:', 'jdbc:mysql:'], 'URI', 'MYSQL'),
  supportedRule('TIDB', ['jdbc:tidb:'], 'URI', 'MYSQL'),
  supportedRule('DAMENG', ['jdbc:dm:'], 'URI', 'DAMENG', DM_SCHEMA_KEYS),
  supportedRule('EDB', ['jdbc:edb:'], 'URI', 'POSTGRESQL', PG_SCHEMA_KEYS),
  unsupportedRule('SQLSERVER', ['jdbc:sqlserver:'], 'SQLSERVER', '已识别为 SQL Server JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('CLICKHOUSE', ['jdbc:clickhouse:'], 'URI', '已识别为 ClickHouse JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('DB2', ['jdbc:db2:'], 'URI', '已识别为 DB2 JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('HIVE', ['jdbc:hive2:', 'jdbc:hive:'], 'URI', '已识别为 Hive JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('PHOENIX', ['jdbc:phoenix:thin:', 'jdbc:phoenix://'], 'UNSUPPORTED', '已识别为 Phoenix JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('KYLIN', ['jdbc:kylin:'], 'URI', '已识别为 Kylin JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('GBASE', ['jdbc:gbase:'], 'URI', '已识别为 GBase JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('XUGU', ['jdbc:xugu:'], 'URI', '已识别为虚谷 JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器'),
  unsupportedRule('OCEANBASE', ['jdbc:oceanbase:'], 'URI', '已识别为 OceanBase JDBC URL，当前系统暂未内置该数据库驱动和元数据提取器')
]

export function parseJdbcUrl(jdbcUrl: string): JdbcUrlParseResult {
  const normalizedUrl = jdbcUrl.trim()
  if (!normalizedUrl.toLowerCase().startsWith(JDBC_PROTOCOL)) {
    throw new Error('JDBC URL 必须以 jdbc: 开头')
  }

  const unwrappedUrl = unwrapJdbcUrl(normalizedUrl)
  const ruleMatch = matchJdbcRule(unwrappedUrl)
  if (!ruleMatch) {
    throw new Error('暂不支持识别该 JDBC URL')
  }

  const parsed = parseByRule(ruleMatch.rule, unwrappedUrl, ruleMatch.matchedPrefix)
  return {
    ...parsed,
    dbType: ruleMatch.rule.mappedDbType,
    vendorCode: ruleMatch.rule.vendorCode,
    matchedPrefix: ruleMatch.matchedPrefix,
    mappedDbType: ruleMatch.rule.mappedDbType,
    supported: ruleMatch.rule.supported,
    unsupportedReason: ruleMatch.rule.supported ? undefined : ruleMatch.rule.unsupportedReason
  }
}

/**
 * parser kind 是前端的模式匹配入口。
 * 新增 vendor 时优先新增规则，再决定落到哪一种 parser。
 */
function parseByRule(rule: JdbcVendorRule, jdbcUrl: string, matchedPrefix: string) {
  switch (rule.parserKind) {
    case 'URI':
      return parseUriStyle(rule, jdbcUrl, matchedPrefix)
    case 'ORACLE':
      return parseOracleUrl(jdbcUrl)
    case 'SQLSERVER':
      return parseSqlServerUrl(jdbcUrl, matchedPrefix)
    case 'UNSUPPORTED':
    default:
      return createEmptyParsedResult()
  }
}

function parseUriStyle(rule: JdbcVendorRule, jdbcUrl: string, matchedPrefix: string) {
  const body = normalizeUriBody(jdbcUrl.slice(matchedPrefix.length))
  if (!body.startsWith('//')) {
    return createEmptyParsedResult()
  }

  const hostAndPath = body.slice(2)
  const slashIndex = hostAndPath.indexOf('/')
  const authority = slashIndex >= 0 ? hostAndPath.slice(0, slashIndex) : hostAndPath
  const pathAndParameters = slashIndex >= 0 ? hostAndPath.slice(slashIndex + 1) : ''
  const parsedPath = splitPathAndParameters(pathAndParameters)
  const hostPort = parseAuthority(authority)
  const parameters = parseMixedParameters(parsedPath.parameterFragment)
  const database = decodeValue(trimToUndefined(parsedPath.path) || '')
  const schema = pickParameter(parameters, rule.schemaKeys || []) || defaultSchema(rule)
  return {
    host: hostPort.host,
    port: hostPort.port,
    database: trimToUndefined(database),
    schema,
    serviceNameOrSid: trimToUndefined(database),
    sidMode: false,
    parameters
  }
}

function parseOracleUrl(jdbcUrl: string) {
  const normalized = normalizeOracleJdbcUrl(jdbcUrl)
  const body = normalized.replace(/^jdbc:oracle:thin:@/i, '')
  if (body.startsWith('(')) {
    return parseOracleDescriptionUrl(body)
  }

  const serviceMatch = body.match(ORACLE_SERVICE_PATTERN)
  if (serviceMatch?.groups) {
    return {
      host: trimToUndefined(serviceMatch.groups.host),
      port: parsePort(serviceMatch.groups.port),
      database: trimToUndefined(serviceMatch.groups.database),
      schema: undefined,
      serviceNameOrSid: trimToUndefined(serviceMatch.groups.database),
      sidMode: false,
      parameters: parseMixedParameters(serviceMatch.groups.rest)
    }
  }

  const sidMatch = body.match(ORACLE_SID_PATTERN)
  if (sidMatch?.groups) {
    return {
      host: trimToUndefined(sidMatch.groups.host),
      port: parsePort(sidMatch.groups.port),
      database: trimToUndefined(sidMatch.groups.database),
      schema: undefined,
      serviceNameOrSid: trimToUndefined(sidMatch.groups.database),
      sidMode: true,
      parameters: parseMixedParameters(sidMatch.groups.rest)
    }
  }

  throw new Error('Oracle JDBC URL 仅支持 service name、SID 或 DESCRIPTION 三种常见形式')
}

function parseOracleDescriptionUrl(body: string) {
  const descriptionEnd = findBalancedDescriptionEnd(body)
  const description = descriptionEnd >= 0 ? body.slice(0, descriptionEnd + 1) : body
  const rest = descriptionEnd >= 0 ? body.slice(descriptionEnd + 1) : ''
  const serviceName = findPatternValue(ORACLE_DESCRIPTION_SERVICE_PATTERN, description)
  const sid = findPatternValue(ORACLE_DESCRIPTION_SID_PATTERN, description)
  const database = trimToUndefined(serviceName || sid)
  return {
    host: findPatternValue(ORACLE_DESCRIPTION_HOST_PATTERN, description),
    port: parsePort(findPatternValue(ORACLE_DESCRIPTION_PORT_PATTERN, description)),
    database,
    schema: undefined,
    serviceNameOrSid: database,
    sidMode: !serviceName && Boolean(sid),
    parameters: parseMixedParameters(rest)
  }
}

function parseSqlServerUrl(jdbcUrl: string, matchedPrefix: string) {
  const body = jdbcUrl.slice(matchedPrefix.length).replace(/^\/\//, '')
  const segments = body.split(';')
  const hostPort = parseAuthority(segments[0] || '')
  const parameters = parseKeyValueSegments(segments.slice(1))
  const database = pickParameter(parameters, ['databaseName', 'database'])
  return {
    host: hostPort.host,
    port: hostPort.port,
    database,
    schema: pickParameter(parameters, ['schema']),
    serviceNameOrSid: database,
    sidMode: false,
    parameters
  }
}

function matchJdbcRule(jdbcUrl: string): JdbcRuleMatch | undefined {
  let bestMatch: JdbcRuleMatch | undefined
  for (const rule of JDBC_VENDOR_RULES) {
    for (const prefix of rule.prefixes) {
      if (jdbcUrl.toLowerCase().startsWith(prefix.toLowerCase())) {
        if (!bestMatch || prefix.length > bestMatch.matchedPrefix.length) {
          bestMatch = { rule, matchedPrefix: prefix }
        }
      }
    }
  }
  return bestMatch
}

function unwrapJdbcUrl(jdbcUrl: string) {
  let current = jdbcUrl
  let changed = true
  while (changed) {
    changed = false
    for (const prefix of JDBC_WRAPPER_PREFIXES) {
      if (current.toLowerCase().startsWith(prefix)) {
        current = `${JDBC_PROTOCOL}${current.slice(prefix.length)}`
        changed = true
        break
      }
    }
  }
  return current
}

function normalizeOracleJdbcUrl(jdbcUrl: string) {
  return jdbcUrl.replace(/^jdbc:alibaba:oracle:/i, 'jdbc:oracle:thin:@')
}

function normalizeUriBody(body: string) {
  if (body.startsWith('//')) {
    return body
  }
  const transportSeparator = body.indexOf('://')
  return transportSeparator >= 0 ? body.slice(transportSeparator + 1) : body
}

function splitPathAndParameters(pathAndParameters: string): ParsedPath {
  const queryIndex = pathAndParameters.indexOf('?')
  const semicolonIndex = pathAndParameters.indexOf(';')
  const splitIndex =
    queryIndex >= 0 && semicolonIndex >= 0
      ? Math.min(queryIndex, semicolonIndex)
      : queryIndex >= 0
        ? queryIndex
        : semicolonIndex
  if (splitIndex < 0) {
    return { path: pathAndParameters, parameterFragment: '' }
  }
  return {
    path: pathAndParameters.slice(0, splitIndex),
    parameterFragment: pathAndParameters.slice(splitIndex)
  }
}

function parseAuthority(authority: string): HostPort {
  const node = firstAuthorityNode(authority)
  const normalized = trimToUndefined(node)
  if (!normalized) {
    return { host: undefined, port: undefined }
  }
  if (normalized.startsWith('[')) {
    const end = normalized.indexOf(']')
    const host = end > 0 ? normalized.slice(1, end) : normalized
    const port = end > 0 && normalized[end + 1] === ':' ? parsePort(normalized.slice(end + 2)) : undefined
    return { host: trimToUndefined(host), port }
  }
  const lastColon = normalized.lastIndexOf(':')
  if (lastColon > 0 && normalized.indexOf(':') === lastColon) {
    return {
      host: trimToUndefined(normalized.slice(0, lastColon)),
      port: parsePort(normalized.slice(lastColon + 1))
    }
  }
  return {
    host: trimToUndefined(normalized),
    port: undefined
  }
}

function firstAuthorityNode(authority: string) {
  let bracketDepth = 0
  for (let index = 0; index < authority.length; index += 1) {
    const current = authority[index]
    if (current === '[') {
      bracketDepth += 1
    } else if (current === ']') {
      bracketDepth = Math.max(0, bracketDepth - 1)
    } else if (current === ',' && bracketDepth === 0) {
      return authority.slice(0, index)
    }
  }
  return authority
}

function findBalancedDescriptionEnd(body: string) {
  let depth = 0
  for (let index = 0; index < body.length; index += 1) {
    const current = body[index]
    if (current === '(') {
      depth += 1
    } else if (current === ')') {
      depth -= 1
      if (depth === 0) {
        return index
      }
    }
  }
  return -1
}

function parseMixedParameters(fragment?: string) {
  if (!fragment) {
    return {}
  }
  let normalized = fragment.trim()
  if (normalized.startsWith('?') || normalized.startsWith(';')) {
    normalized = normalized.slice(1)
  }
  if (!normalized) {
    return {}
  }
  return parseKeyValueSegments(normalized.split(/[&;]/))
}

function parseKeyValueSegments(segments: string[]) {
  return segments.reduce<Record<string, string>>((parameters, segment) => {
    const normalized = segment.trim()
    if (!normalized) {
      return parameters
    }
    const separatorIndex = normalized.indexOf('=')
    if (separatorIndex < 0) {
      parameters[decodeValue(normalized)] = ''
      return parameters
    }
    const key = decodeValue(normalized.slice(0, separatorIndex))
    const value = decodeValue(normalized.slice(separatorIndex + 1))
    parameters[key] = value
    return parameters
  }, {})
}

function pickParameter(parameters: Record<string, string>, keys: string[]) {
  for (const key of keys) {
    const directValue = parameters[key]
    if (directValue?.trim()) {
      return directValue.trim()
    }
    const caseInsensitiveKey = Object.keys(parameters).find(item => item.toLowerCase() === key.toLowerCase())
    if (caseInsensitiveKey && parameters[caseInsensitiveKey]?.trim()) {
      return parameters[caseInsensitiveKey].trim()
    }
  }
  return undefined
}

function findPatternValue(pattern: RegExp, text: string) {
  return trimToUndefined(text.match(pattern)?.groups?.value)
}

function defaultSchema(rule: JdbcVendorRule) {
  return rule.mappedDbType ? DEFAULT_SCHEMAS[rule.mappedDbType as keyof typeof DEFAULT_SCHEMAS] : undefined
}

function parsePort(port?: string) {
  if (!port) {
    return undefined
  }
  const parsed = Number(port)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

function trimToUndefined(value?: string | null) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function decodeValue(value: string) {
  return decodeURIComponent(value || '')
}

function createEmptyParsedResult() {
  return {
    host: undefined,
    port: undefined,
    database: undefined,
    schema: undefined,
    serviceNameOrSid: undefined,
    sidMode: false,
    parameters: {}
  }
}
