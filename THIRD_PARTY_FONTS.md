# Third-Party Fonts

## Bundled In This Repository

以下字体文件会随项目源码和构建产物一起分发：

- `dbmetadoc-generator/src/main/resources/fonts/SourceHanSansCN-Regular.otf`
- `dbmetadoc-generator/src/main/resources/fonts/SourceHanSansCN-Bold.otf`
- `dbmetadoc-generator/src/main/resources/fonts/NotoSansSC-VF.ttf`
- `dbmetadoc-generator/src/main/resources/fonts/JetBrainsMono-Regular.ttf`
- `dbmetadoc-generator/src/main/resources/fonts/JetBrainsMono-Bold.ttf`
- `dbmetadoc-web/src/assets/fonts/SourceHanSansCN-Regular.otf`
- `dbmetadoc-web/src/assets/fonts/SourceHanSansCN-Bold.otf`
- `dbmetadoc-web/src/assets/fonts/JetBrainsMono-Regular.ttf`
- `dbmetadoc-web/src/assets/fonts/JetBrainsMono-Bold.ttf`

以上字体均按 SIL Open Font License 1.1 使用。许可证副本位于：

- `licenses/fonts/SourceHanSans-OFL-1.1.txt`
- `licenses/fonts/NotoSansSC-OFL-1.1.txt`
- `licenses/fonts/JetBrainsMono-OFL-1.1.txt`

官方来源：

- JetBrains Mono: <https://github.com/JetBrains/JetBrainsMono>
- Source Han Sans: <https://github.com/adobe-fonts/source-han-sans>
- Noto Sans CJK / Noto Sans SC: <https://github.com/notofonts/noto-cjk>

## Local-Only Optional Fonts

以下字体不会被提交到仓库，也不会被打进项目内置字体包，只作为本机可选优先字体：

- Apple: `SF Pro Text` / `SF Pro Display` / `SF Mono` / `PingFang SC`
- Xiaomi: `MiSans`
- Huawei: `HarmonyOS Sans SC`

放置方式：

- 直接安装到系统字体目录
- 或放到 `dbmetadoc-local-fonts/`
- 或通过 `DBMETADOC_FONT_DIRECTORIES` 指定额外目录

官方入口：

- Apple Fonts: <https://developer.apple.com/cn/fonts/>
- MiSans: <https://hyperos.mi.com/font/en/download/>
- Huawei Design Resource: <https://developer.huawei.com/consumer/cn/design/resource-V1/>

## Notice

- 本项目不会把 Apple SF、PingFang、MiSans、HarmonyOS Sans 的字体文件随仓库再分发。
- Web 前端只通过 `@font-face` 注册可再分发字体；品牌字体仅通过系统或本机目录参与回退。
- 导出链路会优先尝试本机品牌字体，缺失时自动回退到内置的开源字体。
