package com.poit.doc.sync;

import com.google.gson.Gson;
import com.ly.doc.model.ApiDoc;
import com.ly.doc.model.ApiMethodDoc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将打平后的接口与模型定义 Upsert 到 MySQL（JSON 字段存字段明细）。
 */
public class DocSyncService {

    private static final Gson GSON = new Gson();

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String serviceName;
    private final String serviceVersion;
    private final String env;

    public DocSyncService(String jdbcUrl, String user, String password, String serviceName, String serviceVersion,
            String env) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.env = env;
    }

    public void sync(List<ApiDoc> controllerDocs) throws SQLException {
        Map<String, ModelInfo> mergedModels = new LinkedHashMap<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);
            try {
                for (ApiDoc doc : controllerDocs) {
                    String moduleName = doc.getDesc();
                    List<ApiMethodDoc> methods = doc.getList();
                    if (methods == null) {
                        continue;
                    }
                    for (ApiMethodDoc m : methods) {
                        mergedModels.putAll(ApiDocSupport.extractModelsFromMethod(m));
                        upsertInterface(conn, moduleName, m);
                    }
                }
                for (Map.Entry<String, ModelInfo> e : mergedModels.entrySet()) {
                    upsertModel(conn, e.getKey(), e.getValue());
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private void upsertModel(Connection conn, String fullName, ModelInfo info) throws SQLException {
        String sql = "INSERT INTO api_model_definition (full_name, simple_name, description, fields) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE simple_name = VALUES(simple_name), description = VALUES(description), "
                + "fields = VALUES(fields), modify_time = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, info.getSimpleName());
            ps.setString(3, info.getDescription());
            ps.setString(4, info.getFieldsJson());
            ps.executeUpdate();
        }
    }

    private void upsertInterface(Connection conn, String moduleName, ApiMethodDoc m) throws SQLException {
        String path = m.getPath() != null ? m.getPath() : m.getUrl();
        String method = m.getType() != null ? m.getType() : "";
        String apiName = m.getName();
        String desc = m.getDesc();
        String reqRef = ApiDocSupport.resolveReqModelRef(m);
        String resRef = ApiDocSupport.resolveResModelRef(m);

        // raw_info: 存储额外的接口元数据
        Map<String, Object> rawInfo = new LinkedHashMap<>();
        rawInfo.put("methodId", m.getMethodId());
        rawInfo.put("contentType", m.getContentType());
        rawInfo.put("headers", m.getHeaders());
        rawInfo.put("pathParams", m.getPathParams());
        rawInfo.put("queryParams", m.getQueryParams());
        rawInfo.put("requestParams", m.getRequestParams());
        rawInfo.put("responseParams", m.getResponseParams());
        String rawInfoJson = GSON.toJson(rawInfo);

        String sql = "INSERT INTO api_interface (service_name, service_version, env, module_name, api_name, "
                + "path, method, req_model_ref, res_model_ref, raw_info) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE service_version = VALUES(service_version), module_name = VALUES(module_name), "
                + "api_name = VALUES(api_name), req_model_ref = VALUES(req_model_ref), "
                + "res_model_ref = VALUES(res_model_ref), raw_info = VALUES(raw_info), "
                + "modify_time = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serviceName);
            ps.setString(2, serviceVersion);
            ps.setString(3, env);
            ps.setString(4, moduleName != null ? moduleName : "");
            ps.setString(5, apiName != null ? apiName : "");
            ps.setString(6, path != null ? path : "");
            ps.setString(7, method);
            ps.setString(8, reqRef != null ? reqRef : "");
            ps.setString(9, resRef != null ? resRef : "");
            ps.setString(10, rawInfoJson);
            ps.executeUpdate();
        }
    }
}