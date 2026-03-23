-- MySQL 8：接口与模型定义（供插件 Upsert 使用）
CREATE TABLE IF NOT EXISTS api_model_definition (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_name    VARCHAR(128)    NOT NULL,
    service_version VARCHAR(64)     NOT NULL,
    env             VARCHAR(32)     NOT NULL,
    full_class_name VARCHAR(512)    NOT NULL,
    fields          JSON            NOT NULL,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model (service_name, service_version, env, full_class_name(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_interface (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_name     VARCHAR(128)    NOT NULL,
    service_version  VARCHAR(64)     NOT NULL,
    env              VARCHAR(32)     NOT NULL,
    controller_class VARCHAR(512)    NULL,
    controller_desc  VARCHAR(512)    NULL,
    method_id        VARCHAR(256)    NOT NULL,
    http_path        VARCHAR(1024)   NOT NULL,
    http_method      VARCHAR(32)     NOT NULL,
    description      TEXT            NULL,
    req_model_ref    VARCHAR(512)    NULL,
    res_model_ref    VARCHAR(512)    NULL,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_iface (service_name, service_version, env, method_id(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
