<template>
  <div class="home-container">
    <el-card class="form-card">
      <template #header>
        <h2>生成数据库文档</h2>
      </template>

      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="form.dbType" placeholder="请选择数据库类型" @change="onDbTypeChange">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="KingBase (人大金仓)" value="KINGBASE" />
          </el-select>
        </el-form-item>

        <el-form-item label="主机地址" prop="host">
          <el-input v-model="form.host" placeholder="例如: localhost" />
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>

        <el-form-item label="数据库名" prop="database">
          <el-input v-model="form.database" placeholder="数据库名称" />
        </el-form-item>

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="数据库用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="数据库密码" show-password />
        </el-form-item>

        <el-form-item label="文档标题" prop="title">
          <el-input v-model="form.title" placeholder="例如: 项目数据库设计文档" />
        </el-form-item>

        <el-form-item label="文档格式" prop="format">
          <el-select v-model="form.format" placeholder="请选择文档格式">
            <el-option label="HTML" value="HTML" />
            <el-option label="Markdown" value="MARKDOWN" />
            <el-option label="PDF" value="PDF" />
            <el-option label="Word (DOCX)" value="WORD" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="previewDocument" :loading="previewing">
            预览 (HTML)
          </el-button>
          <el-button type="success" @click="generateDocument" :loading="generating">
            生成并下载
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Preview Dialog -->
    <el-dialog v-model="previewVisible" title="文档预览" width="80%" top="5vh">
      <div class="preview-container" v-html="previewContent"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const previewing = ref(false)
const generating = ref(false)
const previewVisible = ref(false)
const previewContent = ref('')

const form = reactive({
  dbType: 'MYSQL',
  host: 'localhost',
  port: 3306,
  database: '',
  username: '',
  password: '',
  format: 'HTML',
  title: '数据库设计文档'
})

const rules = {
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  format: [{ required: true, message: '请选择文档格式', trigger: 'change' }]
}

function onDbTypeChange(val) {
  if (val === 'MYSQL') form.port = 3306
  else if (val === 'POSTGRESQL') form.port = 5432
  else if (val === 'KINGBASE') form.port = 54321
}

async function previewDocument() {
  try {
    await formRef.value.validate()
    previewing.value = true
    const response = await axios.post('/api/document/preview', form)
    previewContent.value = response.data
    previewVisible.value = true
  } catch (e) {
    if (e.response) {
      ElMessage.error('预览失败: ' + (e.response.data || e.message))
    }
  } finally {
    previewing.value = false
  }
}

async function generateDocument() {
  try {
    await formRef.value.validate()
    generating.value = true
    const response = await axios.post('/api/document/generate', form, {
      responseType: 'blob'
    })

    const contentDisposition = response.headers['content-disposition']
    let filename = 'database-doc'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=["']?([^"'\n;]+)["']?/)
      if (match) filename = match[1]
    }

    const url = URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    ElMessage.success('文档生成成功')
  } catch (e) {
    ElMessage.error('生成失败: ' + e.message)
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.home-container {
  max-width: 800px;
  margin: 30px auto;
  padding: 0 20px;
}

.form-card {
  margin-bottom: 20px;
}

.preview-container {
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #eee;
}
</style>
