/**
 * 模板中心页面逻辑。
 *
 * @author mumu
 * @date 2026-03-28
 */

import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchDatasourceList, removeDatasource } from '../api/dbmeta'
import type { DatasourceDetail } from '../api/dbmeta'

function isCancelAction(error: unknown) {
  return error === 'cancel' || error === 'close'
}

function formatDatabaseLabel(item: DatasourceDetail) {
  if (!item.database) {
    return '未填写数据库'
  }
  return item.schema ? `${item.database} / ${item.schema}` : item.database
}

export function useTemplateCenterPage() {
  const router = useRouter()
  const loading = ref(false)
  const datasourceList = ref<DatasourceDetail[]>([])

  const recentTemplates = computed(() => datasourceList.value.slice(0, 4))

  onMounted(() => {
    void loadDatasourceList()
  })

  async function loadDatasourceList() {
    loading.value = true
    try {
      datasourceList.value = await fetchDatasourceList()
    } finally {
      loading.value = false
    }
  }

  function handleNewExport() {
    void router.push('/export')
  }

  function handleUseTemplate(id: number) {
    void router.push({ path: '/export', query: { datasourceId: String(id) } })
  }

  async function handleRemove(id: number) {
    try {
      await ElMessageBox.confirm('删除后模板将被逻辑移除，是否继续？', '删除模板', { type: 'warning' })
      await removeDatasource(id)
      await loadDatasourceList()
      ElMessage.success('模板已删除')
    } catch (error: unknown) {
      if (!isCancelAction(error)) {
        throw error
      }
    }
  }

  return {
    loading,
    datasourceList,
    recentTemplates,
    loadDatasourceList,
    handleNewExport,
    handleUseTemplate,
    handleRemove,
    formatDatabaseLabel
  }
}
