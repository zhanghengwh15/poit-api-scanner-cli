package com.poit.doc.sync;

import com.ly.doc.builder.ApiDataBuilder;
import com.ly.doc.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 本地多模块工程扫描示例；依赖固定机器路径，默认不参与 CI。
 */
@Disabled("需要本机 poit-wine-mes 工程路径，仅本地调试时手动启用")
public class SmartDocTest {

    @Test
    public void testGenerateDoc() {
        ApiConfig config = new ApiConfig();


        // todo  对应的
        // 1. 基础配置
        config.setServerUrl("http://localhost:8080");
        config.setStrict(false); // 关闭严格模式，防止因为缺少注释报错中断
        config.setProjectName("WineMES");
        // 项目根目录
        config.setFramework("spring");
        String basePath = "/Users/zhangheng/poi_tech/poit-wine-mes/";

        // 2. 核心：将所有包含 Java 代码的模块 src/main/java 都加进来！
        config.setSourceCodePaths(
                // Controller 所在模块 (必须)
                SourceCodePath.builder().setPath(basePath + "poit-wine-mes-app/src/main/java"),

                // API 模块：通常存放对外暴露的接口、DTO、VO (极其重要，否则文档没参数)
                SourceCodePath.builder().setPath(basePath + "poit-wine-mes-api/src/main/java"),

                // BIZ 模块：业务逻辑和内部实体
                SourceCodePath.builder().setPath(basePath + "poit-wine-mes-biz/src/main/java"),

                // DAO 模块：数据库实体 (如果有直接返回数据库实体的情况也需要加)
                SourceCodePath.builder().setPath(basePath + "poit-wine-mes-dao/src/main/java")
        );

        // 3. 执行生成
        System.out.println("开始扫描多模块代码并生成文档...");
        long start = System.currentTimeMillis();
        config.setBaseDir(basePath);
        config.setCodePath(basePath);
        // 生成 HTML 格式
        ApiAllData data = ApiDataBuilder.getApiData(config);

        List<ApiDoc> controllers = ApiDocSupport.flattenControllerDocs(data.getApiDocList());
        // 事实证明就是没有注释  swgger 的注释会覆盖掉

        // 4. 遍历提取到的信息，用于存入你的系统
        for (ApiDoc apiDoc : controllers) {
            System.out.println("\n[Controller 名称]: " + apiDoc.getName());
            System.out.println("[Controller 描述]: " + apiDoc.getDesc()); // 会合并 JavaDoc 和 Swagger

            for (ApiMethodDoc method : apiDoc.getList()) {
                System.out.println("\n  -> [接口路径]: " + method.getType() + " " + method.getPath());
                System.out.println("  -> [接口名称]: " + method.getDesc()); // 会优先读取 @Operation 的 summary
                System.out.println("  -> [接口详情]: " + method.getDetail());

                // 打印请求参数
                System.out.println("  -> [请求参数]:");
                for (ApiParam param : method.getRequestParams()) {
                    System.out.println("      - " + param.getField() + " (" + param.getType() + "): " + param.getDesc());
                }

                // 打印复杂的返回类型（这里演示如何跨越 ResponseEntity 解析到内部的 UserDTO）
                System.out.println("  -> [返回参数结构]:");
                for (ApiParam responseParam : method.getResponseParams()) {
                    System.out.println("      - 字段: " + responseParam.getField()
                            + " | 类型: " + responseParam.getType()
                            + " | 描述: " + responseParam.getDesc());
                }
            }
        }

        assertNotNull(controllers);

        // 对应的转换的实体类。
        System.out.println("生成完毕！耗时: " + (System.currentTimeMillis() - start) + "ms");
    }
}