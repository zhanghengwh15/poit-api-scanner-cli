-- MySQL 8：接口、模型定义与语义词典（供插件 Upsert 使用）
-- 设计说明：
-- 1. 解耦存储：Java 类存入 api_model_definition，通过 ref 引用避免循环依赖
-- 2. JSON 字段：支持动态扩展字段属性，MySQL 8 支持 JSON 查询

-- API 接口主表
CREATE TABLE IF NOT EXISTS `api_interface` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '微服务名，如 user-service',
    `service_version` VARCHAR(20) NOT NULL DEFAULT 'v1' COMMENT '接口契约版本，如 v1/v2',
    `env` VARCHAR(20) NOT NULL DEFAULT 'dev' COMMENT '环境 dev/test/staging/prod',
    `project_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '项目/仓库名（展示或统计用）',
    `module_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '模块/Swagger 分组',
    `api_name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '接口名称（Swagger 注释）',
    `path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'URL 路径',
    `method` VARCHAR(20) NOT NULL DEFAULT '' COMMENT 'GET/POST/PUT/DELETE/PATCH',
    `is_deprecated` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否下线 0-否 1-是',
    `req_model_ref` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求体模型全限定名，空表示无 body',
    `res_model_ref` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '响应体模型全限定名',
    `raw_info` JSON NULL COMMENT '其余 Swagger/OpenAPI 元数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `rec_status` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除 1-有效 0-删除',
    `create_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
    `modify_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_service_env_path_method` (`service_name`, `env`, `path`, `method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 接口主表';

-- API 模型类定义表
-- full_name 全局唯一；若不同服务存在同名不同包的类，靠全限定名区分
CREATE TABLE IF NOT EXISTS `api_model_definition` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `full_name` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '类全限定名，如 com.xxx.UserDTO',
    `simple_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '短类名',
    `description` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '模型注释',
    `fields` JSON NOT NULL COMMENT '字段列表 JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `rec_status` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除',
    `create_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
    `modify_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_full_name` (`full_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 模型类定义';

-- API 语义词典表
-- service_name：空=跨服务全局；非空=该服务内通用词条
-- scope：GLOBAL 表示全局，或类全限定名表示类级作用域
CREATE TABLE IF NOT EXISTS `api_dictionary` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '空=跨服务；非空=该服务内通用词条',
    `raw_text` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '原文，如 id、userName',
    `translated_text` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '译文，无则空串',
    `scope` VARCHAR(255) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL 或 类全限定名',
    `is_common` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否公共词条 1-是 0-否',
    `source` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1-AI 2-MANUAL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `rec_status` TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除',
    `create_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建人',
    `modify_by` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '修改人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_svc_raw_scope` (`service_name`, `raw_text`, `scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 语义词典';