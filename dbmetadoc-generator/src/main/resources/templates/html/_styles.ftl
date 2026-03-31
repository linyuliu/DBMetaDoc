<style>
:root {
  --font-title: ${theme.titleFontCss};
  --font-body: ${theme.bodyFontCss};
  --font-mono: ${theme.monoFontCss};
  --color-primary: ${theme.primaryColor};
  --color-primary-dark: ${theme.primaryDarkColor};
  --color-text: ${theme.textColor};
  --color-label: ${theme.labelColor};
  --color-muted: ${theme.mutedColor};
  --color-border: ${theme.borderColor};
  --color-border-strong: ${theme.headerBorderColor};
  --color-stripe: ${theme.stripeColor};
  --color-header: ${theme.headerColor};
  --color-soft: ${theme.softColor};
  --font-size-body: ${theme.bodyFontSize};
  --line-height-body: ${theme.lineHeight};
}

* { box-sizing: border-box; }
html, body { margin: 0; padding: 0; }
body {
  font-family: var(--font-body);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
  color: var(--color-text);
  background: #ffffff;
}

@page {
  size: A4 portrait;
  margin: ${theme.pageMargin};
}

@media print {
  .screen-only-block,
  .screen-only-link,
  .screen-only-cell {
    display: none !important;
  }
}

.document {
  width: 100%;
  max-width: 184mm;
  margin: 0 auto;
}

.screen-only-block,
.screen-only-link,
.screen-only-cell {
  display: none;
}

.cover {
  padding: 0 0 5mm;
  border-bottom: 0.4mm solid var(--color-border-strong);
  margin-bottom: 5mm;
}

.cover-label {
  margin: 0 0 2mm;
  font-family: var(--font-mono);
  font-size: 8pt;
  letter-spacing: 0.16em;
  color: var(--color-muted);
  text-transform: uppercase;
}

.cover-title {
  margin: 0;
  font-family: var(--font-title);
  font-size: 19pt;
  line-height: 1.2;
  color: var(--color-primary-dark);
}

.cover-subtitle {
  margin: 2mm 0 0;
  color: var(--color-label);
  font-size: 10pt;
}

.section {
  margin-top: 7mm;
}

.chapter {
  page-break-before: always;
}

.section-head {
  display: block;
}

.section-title,
.sub-title {
  page-break-after: avoid;
}

.section-title {
  margin: 0 0 3mm;
  font-family: var(--font-title);
  font-size: ${theme.sectionTitleSize};
  color: var(--color-primary-dark);
  padding-bottom: 1.6mm;
  border-bottom: 0.28mm solid var(--color-border);
}

.sub-title {
  margin: 4mm 0 2mm;
  font-family: var(--font-title);
  font-size: ${theme.subTitleSize};
  color: var(--color-primary);
}

.section-note {
  margin: 0 0 2.5mm;
  color: var(--color-label);
  font-size: 9pt;
}

.comment-box {
  margin: 0 0 3mm;
  padding: 3mm 3.5mm;
  border-left: 0.8mm solid var(--color-primary);
  background: var(--color-soft);
  color: var(--color-label);
}

.table-caption {
  margin: 0 0 3mm;
  padding: 2mm 2.5mm;
  border: 0.25mm solid var(--color-border);
  background: #fbfcfe;
  color: var(--color-label);
}

.table-caption span + span {
  margin-left: 3mm;
}

.chapter-head {
  position: relative;
}

.table-section-title {
  text-align: center;
  padding-right: 0;
}

.table-comment-line {
  margin: 0 0 3mm;
  color: var(--color-label);
  font-size: 9.6pt;
}

.table-overview-table td:nth-child(2),
.table-overview-table th:nth-child(2),
.table-overview-table td:nth-child(3),
.table-overview-table th:nth-child(3) {
  text-align: center;
}

table {
  width: 100%;
  border-collapse: collapse;
  border-spacing: 0;
  table-layout: fixed;
  margin: 0 0 3mm;
}

thead {
  display: table-header-group;
}

tr, td, th {
  page-break-inside: avoid;
}

th, td {
  border: 0.25mm solid var(--color-border);
  padding: 1.8mm 2.2mm;
  vertical-align: top;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  font-size: 8.8pt;
  line-height: 1.45;
}

th {
  background: var(--color-header);
  font-family: var(--font-title);
  font-weight: 700;
  color: var(--color-text);
  border-bottom: 0.35mm solid var(--color-border-strong);
  text-align: left;
}

.stripe tbody tr:nth-child(even) td {
  background: var(--color-stripe);
}

.kv-table th {
  width: 23mm;
  background: var(--color-soft);
  color: var(--color-label);
}

.compact-table th,
.compact-table td,
.grid-table th,
.grid-table td {
  padding-top: 1.5mm;
  padding-bottom: 1.5mm;
}

.mono {
  font-family: var(--font-mono);
}

.center {
  text-align: center;
}

.empty-box {
  padding: 8mm;
  border: 0.3mm dashed var(--color-border-strong);
  background: var(--color-soft);
  color: var(--color-muted);
  text-align: center;
}

@media screen {
  body {
    background: #eef2f6;
  }

  .document {
    width: 210mm;
    max-width: calc(100vw - 24px);
    min-height: 297mm;
    margin: 0 auto;
    padding: 10mm 11mm 12mm;
    background: #ffffff;
    box-shadow: 0 10px 30px rgba(28, 43, 58, 0.14);
  }

  .cover {
    display: none;
  }

  .screen-only-block {
    display: block;
  }

  .screen-only-link {
    display: inline-block;
  }

  .screen-only-cell {
    display: table-cell;
  }

  .screen-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    margin-bottom: 10px;
    border: 1px solid #d7ddea;
    background: #ffffff;
  }

  .screen-kicker {
    margin: 0 0 2px;
    font-family: var(--font-mono);
    font-size: 11px;
    color: #607287;
  }

  .screen-head h1 {
    margin: 0;
    font-family: var(--font-title);
    font-size: 18px;
    color: #1f2f45;
  }

  .screen-head p {
    margin: 2px 0 0;
    font-size: 12px;
    color: #607287;
  }

  .preview-directory {
    position: sticky;
    top: 8px;
    z-index: 20;
    width: fit-content;
    max-width: 100%;
    margin: 0 0 10px auto;
  }

  .preview-directory summary {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;
    border: 1px solid #d7ddea;
    background: #ffffff;
    color: #1f2f45;
    cursor: pointer;
    list-style: none;
    font-size: 12px;
  }

  .preview-directory summary::-webkit-details-marker {
    display: none;
  }

  .preview-directory summary small {
    color: #607287;
  }

  .preview-directory[open] {
    width: 100%;
  }

  .directory-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 6px;
    margin: 8px 0 0;
    padding: 10px;
    border: 1px solid #d7ddea;
    background: #ffffff;
    list-style: none;
  }

  .directory-grid li {
    margin: 0;
  }

  .directory-grid a {
    display: block;
    padding: 6px 8px;
    color: #1f2f45;
    text-decoration: none;
    border-radius: 2px;
  }

  .directory-grid a:hover {
    background: #eef3f9;
  }

  .directory-grid small {
    display: block;
    margin-top: 2px;
    font-size: 11px;
    line-height: 1.35;
    color: #73859a;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .content-pane {
    flex: 1 1 auto;
    min-width: 0;
    background: #ffffff;
    border: 1px solid #d7ddea;
  }

  .section {
    margin-top: 0;
    padding: 10px 10px 14px;
    border-bottom: 1px solid #e6ebf2;
  }

  .chapter {
    page-break-before: auto;
  }

  .section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .chapter-head {
    position: relative;
    display: block;
  }

  .section-title {
    margin-bottom: 10px;
    font-size: 17px;
    border-bottom: 1px solid #d7ddea;
  }

  .table-section-title {
    margin-bottom: 6px;
    text-align: center;
  }

  .chapter-head .screen-only-link {
    position: absolute;
    right: 0;
    top: 2px;
  }

  .sub-title {
    margin-top: 14px;
    font-size: 14px;
    color: #27415f;
  }

  .screen-only-link {
    color: #4f7ba7;
    font-size: 12px;
    text-decoration: none;
  }

  table {
    margin-bottom: 12px;
  }

  th, td {
    font-size: 12px;
    line-height: 1.35;
    padding: 5px 7px;
  }

  .table-caption {
    margin-bottom: 10px;
  }

  .table-comment-line {
    margin-bottom: 10px;
    text-align: left;
    color: #607287;
  }
}
</style>
