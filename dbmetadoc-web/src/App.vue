<template>
  <router-view />
</template>

<style>
:root {
  --dbm-bg-page: #f4f5f7;
  --dbm-bg-card: #ffffff;
  --dbm-bg-subtle: #f8fafc;
  --dbm-text-primary: #1f2937;
  --dbm-text-secondary: #606266;
  --dbm-border: #e5e7eb;
  --dbm-border-light: #ebeef5;
  --dbm-radius-card: 14px;
  --dbm-radius-control: 8px;
  --el-color-primary: #2563eb;
  --el-color-success: #16a34a;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-border-color: #dcdfe6;
  --el-border-color-light: #ebeef5;
  --el-fill-color-light: #f8fafc;
  --el-bg-color-page: #f4f5f7;
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
  font-family: "Microsoft YaHei", "DengXian", "PingFang SC", sans-serif;
  background: var(--dbm-bg-page);
  color: #14212d;
}

code,
pre,
.mono-font {
  font-family: "Cascadia Mono", "JetBrains Mono", "LXGW WenKai Mono Screen", "Consolas", monospace;
}

.admin-page {
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px;
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

.admin-toolbar h1,
.admin-card-header h2 {
  margin: 0;
  color: var(--dbm-text-primary);
}

.admin-toolbar h1 {
  font-size: 28px;
  font-weight: 600;
}

.admin-toolbar-actions {
  flex-wrap: wrap;
}

.admin-card {
  border: 1px solid var(--dbm-border);
  border-radius: var(--dbm-radius-card);
  background: var(--dbm-bg-card);
  box-shadow: none;
}

.admin-card .el-card__header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--dbm-border-light);
}

.admin-card .el-card__body {
  padding: 20px;
}

.admin-card-header h2 {
  font-size: 16px;
  font-weight: 600;
}

.admin-count-text,
.admin-muted-text {
  font-size: 13px;
  color: var(--dbm-text-secondary);
}

.admin-table .el-table__header th {
  background: var(--dbm-bg-subtle);
  color: #374151;
}

.admin-table .el-table__cell {
  padding: 12px 0;
}

.admin-preview-surface {
  min-height: 560px;
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--dbm-border-light);
  border-radius: var(--dbm-radius-control);
  background: var(--dbm-bg-card);
}

@media (max-width: 760px) {
  .admin-page {
    padding: 16px 14px 24px;
  }

  .admin-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
