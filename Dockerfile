# syntax=docker/dockerfile:1.7

##
# DBMetaDoc BellSoft 运行镜像
#
# 使用前请先在宿主机完成打包：
# 1. npm run build --prefix dbmetadoc-web
# 2. mvn -q -pl dbmetadoc-app -am package -DskipTests
#
# 然后执行：
# docker build -t dbmetadoc:bellsoft .
#
# 说明：
# - 当前项目前端资源由 dbmetadoc-app 在打包阶段从 dbmetadoc-web/dist 收集。
# - 这里不在容器构建阶段下载 Node，也不在镜像里保留源码和 Maven 缓存。
# - 运行时基座使用 BellSoft Liberica Runtime Container，JRE 17 + glibc，更适合当前
#   中文字体、PDF/Word 生成和 Spring Boot 单体场景。
##
FROM bellsoft/liberica-runtime-container:jre-17-cds-slim-glibc

LABEL org.opencontainers.image.title="DBMetaDoc" \
      org.opencontainers.image.description="基于 BellSoft Liberica Runtime Container 的 DBMetaDoc 单体运行镜像" \
      org.opencontainers.image.vendor="mumu"

ARG APP_JAR=dbmetadoc-app/target/dbmetadoc-app-1.0.0-SNAPSHOT.jar

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    MALLOC_ARENA_MAX=2 \
    APP_HOME=/opt/dbmetadoc \
    APP_NAME=dbmetadoc-app.jar \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:+UseG1GC -XX:+UseStringDeduplication -XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/dbmetadoc/logs -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djava.io.tmpdir=/opt/dbmetadoc/tmp -Duser.home=/opt/dbmetadoc -Duser.timezone=Asia/Shanghai"

WORKDIR ${APP_HOME}

# 预创建运行目录，容器默认以 root 用户启动，便于后续挂载卷和排查问题。
RUN mkdir -p ${APP_HOME}/logs ${APP_HOME}/tmp

# 只复制已经打好的可执行 Jar，减少镜像层体积和构建上下文噪音。
COPY ${APP_JAR} ${APP_HOME}/${APP_NAME}

EXPOSE 8080

# 通过 sh -c 保留 JAVA_OPTS 的可配置性，方便在不同环境按需追加 JVM 参数。
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar ${APP_HOME}/${APP_NAME}"]
