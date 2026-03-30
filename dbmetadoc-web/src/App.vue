<template>
  <router-view />
</template>

<style>
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
  font-family: "Source Han Sans CN", "Microsoft YaHei", "DengXian", "PingFang SC", sans-serif;
  background:
    radial-gradient(circle at top left, rgba(72, 153, 203, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(34, 84, 61, 0.16), transparent 26%),
    linear-gradient(180deg, #f3f8f6 0%, #eef3f8 100%);
  color: #14212d;
}

code,
pre,
.mono-font {
  font-family: "Cascadia Mono", "JetBrains Mono", "LXGW WenKai Mono Screen", "Consolas", monospace;
}
</style>
