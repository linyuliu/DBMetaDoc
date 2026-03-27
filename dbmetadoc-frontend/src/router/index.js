import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import ResultPage from '../views/ResultPage.vue'

const routes = [
  { path: '/', component: HomePage },
  { path: '/result', component: ResultPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
