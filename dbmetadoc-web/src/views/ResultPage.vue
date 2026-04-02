<template>
  <div class="admin-page result-page">
    <div class="admin-toolbar">
      <h1>预览</h1>
      <router-link to="/">
        <el-button>返回</el-button>
      </router-link>
    </div>

    <el-card class="admin-card preview-card" shadow="never">
      <div v-if="htmlContent" class="admin-preview-surface html-preview" v-html="htmlContent"></div>
      <el-empty v-else description="暂无" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const htmlContent = ref('')
const route = useRoute()

onMounted(() => {
  const routeContent = Array.isArray(route.query.content) ? route.query.content[0] : route.query.content
  htmlContent.value = routeContent || sessionStorage.getItem('previewContent') || ''
})
</script>

<style scoped>
.preview-card {
  margin-top: 16px;
}

.html-preview {
  max-height: 80vh;
}
</style>
