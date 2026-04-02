import { describe, expect, it, vi } from 'vitest'
import {
  BOOLEAN_DISPLAY_SYMBOL,
  BOOLEAN_DISPLAY_TEXT,
  buildConnectionValidationKey,
  buildDocumentPayloadFromForm,
  buildPreviewDependencyKey,
  continueToContentFlow,
  createDefaultForm
} from './export-wizard'

describe('export wizard payload helpers', () => {
  it('defaults boolean display style to symbol and includes it in document payload', () => {
    const form = createDefaultForm()
    form.datasourceId = 7
    form.dbType = 'MYSQL'
    form.host = '127.0.0.1'
    form.port = 3306
    form.database = 'dbmeta'
    form.username = 'root'
    form.password = 'secret'
    form.format = 'PDF'
    form.selectedTableKeys = ['biz.order_main']
    form.exportSections = ['COLUMN_BASIC', 'INDEXES']

    const payload = buildDocumentPayloadFromForm(form, 'template', true)

    expect(form.booleanDisplayStyle).toBe(BOOLEAN_DISPLAY_SYMBOL)
    expect(payload.booleanDisplayStyle).toBe(BOOLEAN_DISPLAY_SYMBOL)
    expect(payload.datasourceId).toBe(7)
    expect(payload.selectedTableKeys).toEqual(['biz.order_main'])
  })

  it('changes preview dependency key when boolean display style changes', () => {
    const form = createDefaultForm()
    const before = buildPreviewDependencyKey(form)

    form.booleanDisplayStyle = BOOLEAN_DISPLAY_TEXT
    const after = buildPreviewDependencyKey(form)

    expect(after).not.toBe(before)
  })

  it('normalizes manual payload display style to text when explicitly selected', () => {
    const form = createDefaultForm()
    form.booleanDisplayStyle = BOOLEAN_DISPLAY_TEXT
    form.dbType = 'POSTGRESQL'
    form.host = '10.0.0.8'
    form.port = 5432
    form.database = 'analytics'
    form.username = 'report'
    form.password = 'secret'
    form.selectedTableKeys = ['public.orders']
    form.exportSections = ['COLUMN_BASIC']

    const payload = buildDocumentPayloadFromForm(form, 'manual', false)

    expect(payload.datasourceId).toBeNull()
    expect(payload.booleanDisplayStyle).toBe(BOOLEAN_DISPLAY_TEXT)
    expect(payload.useStoredPassword).toBe(false)
  })

  it('builds different validation keys when connection fields change', () => {
    const form = createDefaultForm()
    form.datasourceId = 3
    form.dbType = 'MYSQL'
    form.host = '127.0.0.1'
    form.port = 3306
    form.database = 'dbmeta'
    form.username = 'root'
    form.password = 'secret'

    const before = buildConnectionValidationKey(form, 'template', false, false)
    form.database = 'dbmeta_archive'
    const after = buildConnectionValidationKey(form, 'template', false, false)

    expect(after).not.toBe(before)
  })

  it('keeps validation key stable when stored password is reused', () => {
    const form = createDefaultForm()
    form.datasourceId = 12
    form.dbType = 'MYSQL'
    form.host = '10.1.1.8'
    form.port = 3306
    form.database = 'dbmeta'
    form.username = 'root'

    const before = buildConnectionValidationKey(form, 'template', true, true)
    form.password = 'typed-password-should-be-ignored'
    const after = buildConnectionValidationKey(form, 'template', true, true)

    expect(after).toBe(before)
  })

  it('skips explicit test when the current connection is already validated', async () => {
    const validateSourceForm = vi.fn().mockResolvedValue(undefined)
    const testConnection = vi.fn().mockResolvedValue(undefined)
    const loadCatalog = vi.fn().mockResolvedValue(undefined)

    await continueToContentFlow({
      validateSourceForm,
      hasValidatedConnection: true,
      testConnection,
      loadCatalog
    })

    expect(validateSourceForm).toHaveBeenCalledTimes(1)
    expect(testConnection).not.toHaveBeenCalled()
    expect(loadCatalog).toHaveBeenCalledTimes(1)
  })

  it('tests first when the current connection has not been validated', async () => {
    const validateSourceForm = vi.fn().mockResolvedValue(undefined)
    const testConnection = vi.fn().mockResolvedValue(undefined)
    const loadCatalog = vi.fn().mockResolvedValue(undefined)

    await continueToContentFlow({
      validateSourceForm,
      hasValidatedConnection: false,
      testConnection,
      loadCatalog
    })

    expect(validateSourceForm).toHaveBeenCalledTimes(1)
    expect(testConnection).toHaveBeenCalledTimes(1)
    expect(loadCatalog).toHaveBeenCalledTimes(1)
  })
})
