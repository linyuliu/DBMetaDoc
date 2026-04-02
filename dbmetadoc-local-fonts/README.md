# 本机可选字体目录

这个目录用于放置不能随仓库再分发、但允许你在本机导出时优先命中的字体文件。

默认情况下，应用会自动扫描：

- `./dbmetadoc-local-fonts`
- 系统字体目录
- `DBMETADOC_FONT_DIRECTORIES` 指定的额外目录（多个目录用逗号分隔）

建议放入的文件名示例：

- `SF-Pro-Display-Regular.otf`
- `SF-Pro-Text-Regular.otf`
- `SF-Mono-Regular.otf`
- `PingFangSC-Regular.otf`
- `MiSans-Regular.ttf`
- `HarmonyOS_Sans_SC_Regular.ttf`

