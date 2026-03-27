<template>
  <div class="result-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>文档预览结果</h2>
          <router-link to="/">
            <el-button>返回首页</el-button>
          </router-link>
        </div>
      </template>

      <div v-if="htmlContent" class="html-preview" v-html="htmlContent"></div>
      <el-empty v-else description="暂无预览内容，请返回首页生成文档" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const htmlContent = ref('')
const route = useRoute()

onMounted(() => {
  const routeContent = Array.isArray(route.query.content) ? route.query.content[0] : route.query.content
  htmlContent.value = routeContent || sessionStorage.getItem('previewContent') || ''
})
</script>

<style scoped>
.result-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.html-preview {
  max-height: 80vh;
  overflow-y: auto;
  padding: 10px;
}
</style>
