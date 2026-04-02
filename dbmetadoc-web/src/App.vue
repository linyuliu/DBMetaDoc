<template>
  <router-view />
</template>

<style>
:root {
  --dbm-font-title:
    "SF Pro Display",
    "PingFang SC",
    "HarmonyOS Sans SC",
    "MiSans",
    "Source Han Sans CN",
    "Noto Sans SC",
    "Microsoft YaHei",
    sans-serif;
  --dbm-font-body:
    "SF Pro Text",
    "PingFang SC",
    "HarmonyOS Sans SC",
    "MiSans",
    "Source Han Sans CN",
    "Noto Sans SC",
    "Microsoft YaHei",
    sans-serif;
  --dbm-font-mono:
    "JetBrains Mono",
    "SF Mono",
    "Cascadia Mono",
    "Consolas",
    monospace;
  --dbm-bg-page: #f3f5f8;
  --dbm-bg-card: #ffffff;
  --dbm-bg-subtle: #f7f9fc;
  --dbm-text-primary: #17212b;
  --dbm-text-secondary: #5d6b7a;
  --dbm-border: #d9dfe8;
  --dbm-border-light: #e8edf4;
  --dbm-border-strong: #cfd7e3;
  --dbm-shadow-card: 0 18px 44px rgba(15, 23, 42, 0.06);
  --dbm-shadow-button: 0 10px 22px rgba(37, 99, 235, 0.16);
  --dbm-radius-card: 18px;
  --dbm-radius-control: 10px;
  --el-color-primary: #2563eb;
  --el-color-success: #0f9f6e;
  --el-color-warning: #b7791f;
  --el-border-radius-base: 10px;
  --el-border-radius-small: 10px;
  --el-border-color: #d9dfe8;
  --el-border-color-light: #e8edf4;
  --el-fill-color-light: #f7f9fc;
  --el-fill-color-blank: #ffffff;
  --el-bg-color-page: #f3f5f8;
  --el-font-family: var(--dbm-font-body);
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
  margin: 0;
}

body {
  font-family: var(--dbm-font-body);
  background: var(--dbm-bg-page);
  color: #14212d;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

body::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.06), transparent 26%),
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.05), transparent 24%);
  z-index: -1;
}

code,
pre,
.mono-font {
  font-family: var(--dbm-font-mono);
}

.admin-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 20px 32px;
}

.admin-toolbar,
.admin-toolbar-actions,
.admin-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-toolbar,
.admin-card-header {
  justify-content: space-between;
}

.admin-toolbar {
  margin-bottom: 16px;
}

.admin-toolbar h1,
.admin-card-header h2 {
  margin: 0;
  color: var(--dbm-text-primary);
  font-family: var(--dbm-font-title);
}

.admin-toolbar h1 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.admin-toolbar-actions {
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.toolbar-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-toolbar-pill,
.admin-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid var(--dbm-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #516173;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.admin-card {
  border: 1px solid var(--dbm-border);
  border-radius: var(--dbm-radius-card);
  background: var(--dbm-bg-card);
  box-shadow: var(--dbm-shadow-card);
  overflow: hidden;
}

.admin-card .el-card__header {
  padding: 14px 18px;
  border-bottom: 1px solid var(--dbm-border-light);
  background: linear-gradient(180deg, #ffffff, #fafcff);
}

.admin-card .el-card__body {
  padding: 18px;
}

.admin-card-header h2 {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.admin-count-text,
.admin-muted-text {
  font-size: 13px;
  color: var(--dbm-text-secondary);
}

.admin-card :is(.el-button) {
  height: 36px;
  padding-inline: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.admin-card :is(.el-button--primary) {
  box-shadow: var(--dbm-shadow-button);
}

.admin-table {
  --el-table-border-color: var(--dbm-border-light);
  --el-table-header-bg-color: var(--dbm-bg-subtle);
  --el-table-row-hover-bg-color: #f8fbff;
}

.admin-table .el-table__header th {
  background: var(--dbm-bg-subtle);
  color: #344255;
}

.admin-table .el-table__header .cell {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.admin-table .el-table__cell {
  padding: 12px 0;
}

.admin-preview-surface {
  min-height: 480px;
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--dbm-border-light);
  border-radius: var(--dbm-radius-control);
  background: var(--dbm-bg-card);
}

@media (max-width: 760px) {
  .admin-page {
    padding: 18px 14px 24px;
  }

  .admin-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-heading {
    flex-wrap: wrap;
  }

  .admin-toolbar-actions {
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
