# poit-api-scanner-cli

基于 [Smart-doc](https://github.com/smart-doc-group/smart-doc) 的**独立命令行扫描器**：指定源码目录（自动递归发现各模块 `src/main/java`），解析 Spring 等框架下的 Controller 与模型，将**接口清单**与**模型字段** **Upsert** 到 MySQL 8（JSON 存字段明细）。无需在业务工程中引入 Maven 插件。

---

## 使用前准备（运行扫描器）

1. **MySQL 8**：在目标库执行建表脚本 `src/main/resources/schema.sql`。
2. 数据库账号需具备对目标表的 `INSERT` / `UPDATE` 权限。
3. **运行方式**：若使用 **原生可执行文件**，目标机**无需**安装 Java；若使用 **Fat JAR**，运行环境需 **JRE 21+**。构建本工具所需环境见下节「构建与打包」。

---

## 构建与打包（第一步～第四步）

本仓库支持两种产物：**GraalVM Native Image 原生可执行文件**（推荐）与 **Fat JAR**（`java -jar`）。

**建议：** 在具备 **GraalVM for JDK 21** 的机器上，完成**第一步、第二步**后，**优先执行第四步**生成自包含二进制（体积小、冷启动快，适合 CI/CD 与短生命周期脚本）。**第三步**用于仅需 JVM 分发、或未安装 GraalVM 时的常规打包。

### 第一步：环境准备

1. 安装 **JDK 21** 或 **GraalVM for JDK 21**（构建**原生二进制**时请使用 GraalVM，且与下文 Maven 所用 JDK 一致）。
2. 安装 **Maven 3.6+**。
3. 构建 Native 时还需：
   - 终端执行 `java -version`，确认输出中含 **GraalVM** 字样；
   - 已安装 **Native Image** 组件，可执行 `native-image --version`（部分发行版需单独安装 `native-image` 组件）。
4. 将运行 Maven 的 **`JAVA_HOME`** 指向上述 GraalVM（或至少 JDK **17+**；`native-maven-plugin` 对 Maven 进程所用 JDK 有版本要求，**推荐全程使用 GraalVM JDK 21** 以避免不一致）。

### 第二步：获取源码并拉取依赖

```bash
git clone <本仓库地址>   # 若尚未克隆
cd poit-api-scanner-cli  # 进入本模块根目录（以你本地路径为准）
mvn dependency:resolve -q
```

首次执行会按 `pom.xml` 从中央仓库下载依赖；网络正常即可，无需额外手工安装依赖包。

### 第三步：构建 Fat JAR（JVM 运行）

在本模块根目录执行：

```bash
mvn clean package
```

产物为已 Shade 的 **Fat JAR**，可直接运行：

`target/poit-api-scanner-cli-<version>.jar`

安装到本地仓库时的 Maven 坐标：

- `groupId`：`com.poit.doc`
- `artifactId`：`poit-api-scanner-cli`
- `version`：以本仓库 `pom.xml` 为准

> 若你只需要 Fat JAR，到本步即可；**不要**加 `-Pnative`（该 profile 会跳过 Shade，主要用于下一步原生构建）。

### 第四步：构建二进制可执行文件（推荐）

在本模块根目录执行：

```bash
mvn -s /Users/zhangheng/jar/settings-alibaba.xml -DskipTests=true clean package -Pnative
```

（若需跳过测试以缩短总耗时，可追加 `-DskipTests`。）

**说明：** 激活 `-Pnative` 后，GraalVM 的 **AOT 编译器（Substrate VM）** 会对你工程中的代码与**编译期可达的**依赖做静态分析，将其中可静态绑定的部分编译为本地机器码，生成**自包含**可执行文件（名称见 `pom.xml` 中 `native` profile 的 `imageName`，一般为 `poit-api-scanner`，通常位于 `target/` 目录下）。该过程一般需要 **约 1～3 分钟**（视 CPU、磁盘与依赖规模而定），**首次**构建往往更久。部分第三方库若大量依赖反射、JNI、资源文件等，可能还需补充 Native Image 配置才能一次编过。

---

## `--scan-dir` 与 `pom.xml`

- **`--scan-dir`** 应指向你要扫描的 **工程根目录**（其下可递归发现各模块的 `src/main/java`）。
- 当**未同时**显式传入 **`--service-name`** 与 **`--artifact-id`** 时，工具会读取 **`--scan-dir/pom.xml`**（仅此路径，不向父目录查找），用其中 **`<project>` 下直接子元素** `<artifactId>` 作为缺省值（与 `<parent>` 内的 `artifactId` 无关）。
- 若此时 **`--scan-dir` 下没有 `pom.xml`**，或无法解析出 `artifactId`，进程会以退出码 **2** 结束，并提示：**无法作为 Maven/Java 工程识别**——请将 `--scan-dir` 指到含 `pom.xml` 的 Maven 模块根目录，或**同时显式指定** `--service-name` 与 `--artifact-id`（二者都给出则**不再要求**目录下存在 `pom.xml`）。

---

## 运行示例

**优先：使用第四步产出的原生可执行文件（无需目标机安装 Java）：**

```bash
./target/poit-api-scanner \
  --scan-dir=/path/to/your/repo-or-module-root \
  --db-url='jdbc:mysql://127.0.0.1:3306/your_doc_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai' \
  --db-user=root \
  --db-password=secret \
  --service-name=user-service \
  --artifact-id=user-service \
  --service-version=v1 \
  --env=dev
```

（Windows 下可执行文件名为 `poit-api-scanner.exe`，路径仍在 `target/`。）

**使用第三步产出的 Fat JAR：**

显式指定服务名与 artifactId（不依赖本地 `pom.xml`）：

```bash
java -jar target/poit-api-scanner-cli-1.0.0-SNAPSHOT.jar \
  --scan-dir=/path/to/your/repo-or-module-root \
  --db-url='jdbc:mysql://127.0.0.1:3306/your_doc_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai' \
  --db-user=root \
  --db-password=secret \
  --service-name=user-service \
  --artifact-id=user-service \
  --service-version=v1 \
  --env=dev
```

省略 `--service-name` / `--artifact-id`（从 `--scan-dir/pom.xml` 读取 `artifactId`）：

```bash
java -jar target/poit-api-scanner-cli-1.0.0-SNAPSHOT.jar \
  --scan-dir=/path/to/maven-module-root \
  --db-url='jdbc:mysql://127.0.0.1:3306/your_doc_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai' \
  --db-user=root \
  --db-password=secret \
  --service-version=v1 \
  --env=dev
```

查看全部参数（JAR 与原生二进制参数相同）：

```bash
./target/poit-api-scanner --help
# 或
java -jar target/poit-api-scanner-cli-1.0.0-SNAPSHOT.jar --help
```

---

## 常用可选参数

| 选项 | 说明 |
|------|------|
| `--framework` | Smart-doc 框架，默认 `spring` |
| `--package-filters` | 只扫描指定包（Smart-doc 语法） |
| `--package-exclude-filters` | 排除包 |
| `--project-name` | 展示用项目名，默认取 `--scan-dir` 最后一级目录名 |
| `--source-path` | 可重复指定，显式源码根；一旦指定则**不再**自动发现 |

---

## 配置项说明（CLI）

| 选项 | 必填 | 说明 |
|------|------|------|
| `--scan-dir` | 是 | 扫描根目录；未指定 `--source-path` 时递归发现 `**/src/main/java` |
| `--db-url` | 是 | MySQL JDBC URL |
| `--db-user` / `--db-password` | 是 | 数据库账号 |
| `--service-version` | 是 | 如 `v1` |
| `--env` | 是 | 如 `dev` / `test` / `prod` |
| `--service-name` | 条件 | 未传时从 `--scan-dir/pom.xml` 的 `artifactId` 读取；若与 `--artifact-id` **均已**显式传入则不要求 `pom.xml` |
| `--artifact-id` | 条件 | 同上；写入库表时使用的 artifactId |
| `--framework` | 否 | 默认 `spring` |
| `--package-filters` / `--package-exclude-filters` | 否 | 与 Smart-doc 一致 |
| `--project-name` | 否 | 见上表 |
| `--source-path` | 否 | 多源码根；不指定则按目录自动发现 |

---

## 数据写入说明（简要）

- **`api_interface`**：每个接口方法一条记录。
- **`api_model_definition`**：按全类名聚合模型，**fields** 列为 JSON。

---

## 常见问题

**Q：提示找不到 `pom.xml` 或无法识别为 Maven/Java 工程？**  
A：将 `--scan-dir` 指到**该 Maven 模块根目录**（该目录下即有 `pom.xml`），或同时传入 `--service-name` 与 `--artifact-id`。

**Q：解析不到 Controller？**  
A：检查 `--package-filters` / `--framework`，并确认源码在 `src/main/java` 或通过 `--source-path` 指到正确根目录。

**Q：表不存在或连接失败？**  
A：先执行 `schema.sql`，核对 JDBC URL 与账号权限。

---

## 相关仓库

- Smart-doc：<https://github.com/smart-doc-group/smart-doc>
