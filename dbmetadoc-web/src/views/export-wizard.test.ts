import { describe, expect, it } from 'vitest'
import {
  BOOLEAN_DISPLAY_SYMBOL,
  BOOLEAN_DISPLAY_TEXT,
  buildDocumentPayloadFromForm,
  buildPreviewDependencyKey,
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
})
