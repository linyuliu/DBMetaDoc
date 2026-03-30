<template>
  <div class="template-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="hero-kicker">DBMetaDoc</p>
        <h1>模板中心</h1>
        <p>
          这里只保留模板管理和进入导出的入口。连接、选表、预览、导出都放到单独向导里，页面会更轻，也更方便你继续改。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="handleNewExport">新建导出</el-button>
        <el-button size="large" @click="loadDatasourceList" :loading="loading">刷新模板</el-button>
      </div>
    </section>

    <section class="recent-section">
      <div class="section-head">
        <div>
          <h2>最近模板</h2>
          <p>优先展示最近维护或使用过的数据源模板。</p>
        </div>
        <span class="section-count">{{ datasourceList.length }} 个模板</span>
      </div>

      <el-empty v-if="!recentTemplates.length && !loading" description="还没有可用模板" />
      <div v-else class="recent-grid">
        <article v-for="item in recentTemplates" :key="item.id" class="recent-card">
          <div class="recent-head">
            <div>
              <h3>{{ item.name }}</h3>
              <p>{{ item.dbType }} · {{ item.host || '未配置主机' }}<template v-if="item.port">:{{ item.port }}</template></p>
            </div>
            <el-tag size="small" type="success" effect="light" v-if="item.passwordSaved">已存密码</el-tag>
          </div>
          <p class="recent-db">{{ formatDatabaseLabel(item) }}</p>
          <p class="recent-remark">{{ item.remark || '未填写备注' }}</p>
          <div class="recent-foot">
            <span>更新于 {{ item.updatedAt || item.createdAt || '未知时间' }}</span>
            <el-button text type="primary" @click="handleUseTemplate(item.id)">进入导出</el-button>
          </div>
        </article>
      </div>
    </section>

    <el-card class="list-card" shadow="never">
      <template #header>
        <div class="section-head">
          <div>
            <h2>全部模板</h2>
            <p>支持继续编辑、直接导出，密码只返回是否已保存。</p>
          </div>
        </div>
      </template>

      <el-table :data="datasourceList" v-loading="loading" empty-text="暂无模板" class="template-table">
        <el-table-column prop="name" label="模板名称" min-width="180" />
        <el-table-column label="连接位置" min-width="220">
          <template #default="{ row }">
            {{ row.host || '-' }}<template v-if="row.port">:{{ row.port }}</template>
          </template>
        </el-table-column>
        <el-table-column label="库 / Schema" min-width="220">
          <template #default="{ row }">
            {{ formatDatabaseLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column label="密码" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.passwordSaved" size="small" type="success" effect="light">已保存</el-tag>
            <span v-else class="muted-text">未保存</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ row.updatedAt || row.createdAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleUseTemplate(row.id)">导出</el-button>
            <el-button text type="danger" @click="handleRemove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useTemplateCenterPage } from './template-center'

const {
  loading,
  datasourceList,
  recentTemplates,
  loadDatasourceList,
  handleNewExport,
  handleUseTemplate,
  handleRemove,
  formatDatabaseLabel
} = useTemplateCenterPage()
</script>

<style scoped>
.template-page {
  max-width: 1360px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border: 1px solid rgba(18, 60, 73, 0.08);
  border-radius: 28px;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.95), rgba(237, 245, 249, 0.92));
  box-shadow: 0 20px 48px rgba(34, 66, 79, 0.08);
}

.hero-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: #40606d;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  font-size: 34px;
  color: #102732;
}

.hero-copy p:last-child {
  max-width: 720px;
  margin: 14px 0 0;
  line-height: 1.8;
  color: #536a77;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.recent-section {
  margin-top: 24px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
  color: #163340;
}

.section-head p {
  margin: 6px 0 0;
  color: #607987;
}

.section-count {
  color: #607987;
  font-size: 13px;
}

.recent-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.recent-card,
.list-card {
  border: 1px solid rgba(19, 58, 76, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
}

.recent-card {
  padding: 18px 18px 16px;
}

.recent-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.recent-head h3 {
  margin: 0;
  font-size: 18px;
  color: #13303b;
}

.recent-head p,
.recent-db,
.recent-remark,
.recent-foot span,
.muted-text {
  color: #647d8b;
}

.recent-head p,
.recent-db,
.recent-remark {
  margin: 6px 0 0;
  line-height: 1.7;
}

.recent-db {
  font-family: "Cascadia Mono", "JetBrains Mono", "LXGW WenKai Mono Screen", "Consolas", monospace;
}

.recent-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.list-card {
  margin-top: 24px;
}

.template-table :deep(.cell) {
  line-height: 1.6;
}

@media (max-width: 1120px) {
  .recent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .template-page {
    padding: 18px 14px 28px;
  }

  .hero-card {
    flex-direction: column;
    padding: 22px 20px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .recent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
