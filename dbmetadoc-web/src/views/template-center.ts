/**
 * 模板中心页面逻辑。
 *
 * @author mumu
 * @date 2026-03-28
 */

import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchDatasourceList, removeDatasource } from '../api/dbmeta'
import type { DatasourceDetail } from '../api/dbmeta'

function isCancelAction(error: unknown) {
  return error === 'cancel' || error === 'close'
}

function formatDatabaseLabel(item: DatasourceDetail) {
  if (item.database && item.schema) {
    return `${item.database} / ${item.schema}`
  }
  return item.database || item.schema || '-'
}

export function useTemplateCenterPage() {
  const router = useRouter()
  const loading = ref(false)
  const datasourceList = ref<DatasourceDetail[]>([])

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
      await ElMessageBox.confirm('确认删除该模板？', '删除', { type: 'warning' })
      await removeDatasource(id)
      await loadDatasourceList()
      ElMessage.success('已删除')
    } catch (error: unknown) {
      if (!isCancelAction(error)) {
        throw error
      }
    }
  }

  return {
    loading,
    datasourceList,
    loadDatasourceList,
    handleNewExport,
    handleUseTemplate,
    handleRemove,
    formatDatabaseLabel
  }
}
