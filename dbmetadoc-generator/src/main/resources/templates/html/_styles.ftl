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

.document {
  width: 100%;
  max-width: 184mm;
  margin: 0 auto;
}

.cover {
  padding: 8mm 0 10mm;
  border-bottom: 0.4mm solid var(--color-border-strong);
  page-break-after: always;
}

.cover-label {
  margin: 0 0 3mm;
  font-family: var(--font-mono);
  font-size: 8pt;
  letter-spacing: 0.16em;
  color: var(--color-muted);
  text-transform: uppercase;
}

.cover-title {
  margin: 0;
  font-family: var(--font-title);
  font-size: ${theme.coverTitleSize};
  line-height: 1.25;
  color: var(--color-primary-dark);
}

.cover-subtitle {
  margin: 3mm 0 0;
  color: var(--color-label);
  font-size: 10pt;
}

.cover-meta {
  margin-top: 7mm;
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.cover-meta td {
  border: 0.25mm solid var(--color-border);
  padding: 2.2mm 2.6mm;
  vertical-align: top;
}

.cover-meta .label {
  width: 28mm;
  background: var(--color-soft);
  color: var(--color-label);
  font-weight: 700;
}

.section {
  margin-top: 7mm;
}

.chapter {
  page-break-before: always;
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
  padding: 2mm 2.5mm;
  vertical-align: top;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  font-size: 9pt;
  line-height: 1.55;
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
</style>
