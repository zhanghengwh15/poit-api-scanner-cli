# poit-api-scanner-cli

基于 [Smart-doc](https://github.com/smart-doc-group/smart-doc) 的**独立命令行扫描器**：指定源码目录（自动递归发现各模块 `src/main/java`），解析 Spring 等框架下的 Controller 与模型，将**接口清单**与**模型字段** **Upsert** 到 MySQL 8（JSON 存字段明细）。无需在业务工程中引入 Maven 插件。

---

## 使用前准备

1. **JDK 21**、**Maven 3.6+**（用于打包本工具；运行 Fat JAR 时目标环境只需 JRE 21+）。
2. **MySQL 8**：在目标库执行建表脚本：

   `src/main/resources/schema.sql`

3. 数据库账号需具备对目标表的 `INSERT` / `UPDATE` 权限。

---

## 构建

在本仓库根目录执行：

```bash
cd poit-api-scanner-cli   # 或你克隆后的本模块根目录
mvn clean package
```

产物为已 Shade 的 Fat JAR，可直接运行：

`target/poit-api-scanner-cli-<version>.jar`

坐标（安装到本地仓库时）：

- `groupId`：`com.poit.doc`
- `artifactId`：`poit-api-scanner-cli`
- `version`：以本仓库 `pom.xml` 为准

### （可选）GraalVM Native Image

若本机已安装 **GraalVM for JDK 21** 且 `native-image` 可用，并用于执行 Maven 的 JDK 为 **17+**（建议与 GraalVM 一致），可构建本地可执行文件：

```bash
mvn clean package -Pnative -DskipTests
```

默认可执行文件名：`poit-api-scanner`（见 `pom.xml` 中 `native` profile）。首次构建可能较慢，且部分依赖需满足 Native Image 的反射等资源要求。

---

## `--scan-dir` 与 `pom.xml`

- **`--scan-dir`** 应指向你要扫描的 **工程根目录**（其下可递归发现各模块的 `src/main/java`）。
- 当**未同时**显式传入 **`--service-name`** 与 **`--artifact-id`** 时，工具会读取 **`--scan-dir/pom.xml`**（仅此路径，不向父目录查找），用其中 **`<project>` 下直接子元素** `<artifactId>` 作为缺省值（与 `<parent>` 内的 `artifactId` 无关）。
- 若此时 **`--scan-dir` 下没有 `pom.xml`**，或无法解析出 `artifactId`，进程会以退出码 **2** 结束，并提示：**无法作为 Maven/Java 工程识别**——请将 `--scan-dir` 指到含 `pom.xml` 的 Maven 模块根目录，或**同时显式指定** `--service-name` 与 `--artifact-id`（二者都给出则**不再要求**目录下存在 `pom.xml`）。

---

## 运行示例

**显式指定服务名与 artifactId（不依赖本地 `pom.xml`）：**

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

**省略 `--service-name` / `--artifact-id`（从 `--scan-dir/pom.xml` 读取 `artifactId`）：**

```bash
java -jar target/poit-api-scanner-cli-1.0.0-SNAPSHOT.jar \
  --scan-dir=/path/to/maven-module-root \
  --db-url='jdbc:mysql://127.0.0.1:3306/your_doc_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai' \
  --db-user=root \
  --db-password=secret \
  --service-version=v1 \
  --env=dev
```

查看全部参数：

```bash
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
