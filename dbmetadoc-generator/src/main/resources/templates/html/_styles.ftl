<style>
${theme.fontFaceCss!""}
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
  font-family: ${theme.bodyFontCss};
  font-size: ${theme.bodyFontSize};
  line-height: ${theme.lineHeight};
  color: ${theme.textColor};
  background: #ffffff;
}

@page {
  size: A4 portrait;
  margin: ${theme.pageMargin};
}

@media print {
  .screen-only-block,
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
.screen-only-cell {
  display: none;
}

.cover {
  padding: 0 0 5mm;
  border-bottom: 0.4mm solid ${theme.headerBorderColor};
  margin-bottom: 5mm;
}

.cover-label {
  margin: 0 0 2mm;
  font-family: ${theme.bodyFontCss};
  font-size: 8pt;
  letter-spacing: 0.16em;
  color: ${theme.mutedColor};
  text-transform: uppercase;
}

.cover-title {
  margin: 0;
  font-family: ${theme.titleFontCss};
  font-size: 19pt;
  line-height: 1.2;
  color: ${theme.primaryDarkColor};
}

.cover-subtitle {
  margin: 2mm 0 0;
  color: ${theme.labelColor};
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
  font-family: ${theme.titleFontCss};
  font-size: ${theme.sectionTitleSize};
  color: ${theme.primaryDarkColor};
  padding-bottom: 1.6mm;
  border-bottom: 0.28mm solid ${theme.borderColor};
}

.sub-title {
  margin: 4mm 0 2mm;
  font-family: ${theme.titleFontCss};
  font-size: ${theme.subTitleSize};
  color: ${theme.primaryColor};
}

.section-note {
  margin: 0 0 2.5mm;
  color: ${theme.labelColor};
  font-size: 9pt;
}

.comment-box {
  margin: 0 0 3mm;
  padding: 3mm 3.5mm;
  border-left: 0.8mm solid ${theme.primaryColor};
  background: ${theme.softColor};
  color: ${theme.labelColor};
}

.table-caption {
  margin: 0 0 3mm;
  padding: 2mm 2.5mm;
  border: 0.25mm solid ${theme.borderColor};
  background: #fbfcfe;
  color: ${theme.labelColor};
}

.table-caption span + span {
  margin-left: 3mm;
}

.chapter-head {
  display: block;
}

.table-section-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.8mm;
}

.chapter-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 8mm;
  padding: 0.7mm 2.1mm;
  border-radius: 999px;
  background: ${theme.primaryColor};
  color: #ffffff;
  font-family: ${theme.titleFontCss};
  font-size: 0.88em;
  font-weight: 700;
  line-height: 1;
}

.chapter-dot,
.chapter-title-text {
  color: ${theme.primaryDarkColor};
  font-weight: 700;
}

.table-comment-line {
  margin: 0 0 3mm;
  color: ${theme.labelColor};
  font-size: 9.6pt;
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
  border: 0.25mm solid ${theme.borderColor};
  padding: 1.8mm 2.2mm;
  vertical-align: top;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: normal;
  font-size: 9.2pt;
  line-height: 1.5;
  font-family: ${theme.bodyFontCss};
  font-weight: 600;
}

th {
  background: ${theme.headerColor};
  font-weight: 700;
  color: ${theme.textColor};
  border-bottom: 0.35mm solid ${theme.headerBorderColor};
  text-align: left;
}

.stripe tbody tr:nth-child(even) td {
  background: ${theme.stripeColor};
}

.kv-table th {
  width: 23mm;
  background: ${theme.softColor};
  color: ${theme.labelColor};
}

.compact-table th,
.compact-table td,
.grid-table th,
.grid-table td {
  padding-top: 1.5mm;
  padding-bottom: 1.5mm;
}

.center {
  text-align: center;
}

.cell-strong {
  font-weight: 700;
}

.order-cell {
  color: ${theme.primaryDarkColor};
  letter-spacing: 0.03em;
}

.bool-cell {
  font-family: ${theme.titleFontCss};
  font-size: 0.96em;
  letter-spacing: 0.08em;
}

.row-lines-1 td {
  height: 8.4mm;
}

.row-lines-2 td {
  height: 12.2mm;
}

.row-lines-3 td {
  height: 15.8mm;
}

.row-lines-4 td {
  height: 19.6mm;
}

.empty-box {
  padding: 8mm;
  border: 0.3mm dashed ${theme.headerBorderColor};
  background: ${theme.softColor};
  color: ${theme.mutedColor};
  text-align: center;
}

@media screen {
  body {
    background: #f3f4f6;
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
    font-family: var(--font-body);
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
  }

  .chapter-head {
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

  .sub-title {
    margin-top: 14px;
    font-size: 14px;
    color: #27415f;
  }

  table {
    margin-bottom: 12px;
  }

  th, td {
    font-size: 13px;
    line-height: 1.45;
    padding: 5px 7px;
  }

  .row-lines-1 td {
    height: 34px;
  }

  .row-lines-2 td {
    height: 50px;
  }

  .row-lines-3 td {
    height: 66px;
  }

  .row-lines-4 td {
    height: 82px;
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
