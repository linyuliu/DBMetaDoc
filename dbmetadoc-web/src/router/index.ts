import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import TemplateCenterPage from '../views/TemplateCenterPage.vue'
import ExportWizardPage from '../views/ExportWizardPage.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', component: TemplateCenterPage },
  { path: '/export', component: ExportWizardPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
