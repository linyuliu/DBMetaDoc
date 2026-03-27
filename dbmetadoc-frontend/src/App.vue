<template>
  <div>
    <el-container>
      <el-header>
        <div class="nav-bar">
          <span class="title">DBMetaDoc 数据库文档生成工具</span>
          <router-link to="/" class="nav-link">首页</router-link>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: Arial, sans-serif; }

.el-header {
  background-color: #2c3e50;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.nav-bar {
  display: flex;
  align-items: center;
  width: 100%;
}

.title {
  font-size: 20px;
  font-weight: bold;
  flex: 1;
}

.nav-link {
  color: white;
  text-decoration: none;
  margin-left: 20px;
}

.nav-link:hover {
  text-decoration: underline;
}
</style>
