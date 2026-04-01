import { describe, expect, it } from 'vitest'
import { parseJdbcUrl } from './jdbc-url'

describe('parseJdbcUrl', () => {
  it('parses mysql transport variants and keeps the first host node', () => {
    const parsed = parseJdbcUrl('jdbc:mysql:loadbalance://primary-db:3306,replica-db:3307/demo_db?useSSL=false')
    const srvParsed = parseJdbcUrl('jdbc:mysql+srv://cluster.example.com/demo_srv')

    expect(parsed.dbType).toBe('MYSQL')
    expect(parsed.host).toBe('primary-db')
    expect(parsed.port).toBe(3306)
    expect(parsed.database).toBe('demo_db')
    expect(parsed.parameters.useSSL).toBe('false')
    expect(srvParsed.dbType).toBe('MYSQL')
    expect(srvParsed.host).toBe('cluster.example.com')
    expect(srvParsed.database).toBe('demo_srv')
  })

  it('maps mariadb and tidb to MYSQL', () => {
    expect(parseJdbcUrl('jdbc:mariadb://127.0.0.1:3306/maria_db').dbType).toBe('MYSQL')
    expect(parseJdbcUrl('jdbc:tidb://127.0.0.1:4000/tidb_db').dbType).toBe('MYSQL')
  })

  it('unwraps log4jdbc and resolves pg compatible schema parameters case-insensitively', () => {
    const parsed = parseJdbcUrl('jdbc:log4jdbc:edb://primary-pg:5444,standby-pg:5445/app_db;CurrentSchema=reporting')

    expect(parsed.dbType).toBe('POSTGRESQL')
    expect(parsed.host).toBe('primary-pg')
    expect(parsed.port).toBe(5444)
    expect(parsed.database).toBe('app_db')
    expect(parsed.schema).toBe('reporting')
  })

  it('defaults postgresql schema to public when not provided', () => {
    const parsed = parseJdbcUrl('jdbc:postgresql://127.0.0.1:5432/demo_db')

    expect(parsed.schema).toBe('public')
  })

  it('parses oracle service name, sid and description urls', () => {
    const service = parseJdbcUrl('jdbc:oracle:thin:@//10.10.10.8:1522/ORCLPDB1')
    const sid = parseJdbcUrl('jdbc:oracle:thin:@10.10.10.9:1521:ORCL')
    const description = parseJdbcUrl('jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.10.15)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1)));oracle.net.CONNECT_TIMEOUT=3000')
    const aliOracle = parseJdbcUrl('jdbc:alibaba:oracle://10.10.10.8:1522/ORCLPDB1')

    expect(service.host).toBe('10.10.10.8')
    expect(service.sidMode).toBe(false)
    expect(sid.database).toBe('ORCL')
    expect(sid.sidMode).toBe(true)
    expect(description.host).toBe('192.168.10.15')
    expect(description.database).toBe('orclpdb1')
    expect(description.parameters['oracle.net.CONNECT_TIMEOUT']).toBe('3000')
    expect(aliOracle.dbType).toBe('ORACLE')
    expect(aliOracle.database).toBe('ORCLPDB1')
  })

  it('surfaces unsupported vendors with consistent metadata', () => {
    const parsed = parseJdbcUrl('jdbc:sqlserver://sql-host:1433;databaseName=erp')

    expect(parsed.supported).toBe(false)
    expect(parsed.vendorCode).toBe('SQLSERVER')
    expect(parsed.host).toBe('sql-host')
    expect(parsed.database).toBe('erp')
    expect(parsed.unsupportedReason).toContain('SQL Server')
  })

  it('throws for unknown jdbc vendors', () => {
    expect(() => parseJdbcUrl('jdbc:customdb://opaque-host/demo')).toThrow('暂不支持识别该 JDBC URL')
  })
})
