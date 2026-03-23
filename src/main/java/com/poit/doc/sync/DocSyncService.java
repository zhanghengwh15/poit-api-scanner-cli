package com.poit.doc.sync;

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
        Map<String, String> mergedModels = new LinkedHashMap<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);
            try {
                for (ApiDoc doc : controllerDocs) {
                    String controllerClass = doc.getDocClass();
                    if (controllerClass == null || controllerClass.isEmpty()) {
                        controllerClass = doc.getName();
                    }
                    String controllerDesc = doc.getDesc();
                    List<ApiMethodDoc> methods = doc.getList();
                    if (methods == null) {
                        continue;
                    }
                    for (ApiMethodDoc m : methods) {
                        mergedModels.putAll(ApiDocSupport.extractModelsFromMethod(m));
                        upsertInterface(conn, controllerClass, controllerDesc, m);
                    }
                }
                for (Map.Entry<String, String> e : mergedModels.entrySet()) {
                    upsertModel(conn, e.getKey(), e.getValue());
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private void upsertModel(Connection conn, String fullClassName, String fieldsJson) throws SQLException {
        String sql = "INSERT INTO api_model_definition (service_name, service_version, env, full_class_name, fields) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE fields = VALUES(fields), updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serviceName);
            ps.setString(2, serviceVersion);
            ps.setString(3, env);
            ps.setString(4, fullClassName);
            ps.setString(5, fieldsJson);
            ps.executeUpdate();
        }
    }

    private void upsertInterface(Connection conn, String controllerClass, String controllerDesc, ApiMethodDoc m)
            throws SQLException {
        String methodId = m.getMethodId();
        if (methodId == null || methodId.isEmpty()) {
            methodId = safeId(controllerClass, m.getPath(), m.getType(), m.getName());
        }
        String path = m.getPath() != null ? m.getPath() : m.getUrl();
        String httpMethod = m.getType() != null ? m.getType() : "";
        String desc = m.getDesc();
        String reqRef = ApiDocSupport.resolveReqModelRef(m);
        String resRef = ApiDocSupport.resolveResModelRef(m);

        String sql = "INSERT INTO api_interface (service_name, service_version, env, controller_class, controller_desc, "
                + "method_id, http_path, http_method, description, req_model_ref, res_model_ref) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE controller_class = VALUES(controller_class), "
                + "controller_desc = VALUES(controller_desc), http_path = VALUES(http_path), "
                + "http_method = VALUES(http_method), description = VALUES(description), "
                + "req_model_ref = VALUES(req_model_ref), res_model_ref = VALUES(res_model_ref), "
                + "updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serviceName);
            ps.setString(2, serviceVersion);
            ps.setString(3, env);
            ps.setString(4, controllerClass);
            ps.setString(5, controllerDesc);
            ps.setString(6, methodId);
            ps.setString(7, path != null ? path : "");
            ps.setString(8, httpMethod);
            ps.setString(9, desc);
            ps.setString(10, reqRef);
            ps.setString(11, resRef);
            ps.executeUpdate();
        }
    }

    private static String safeId(String controllerClass, String path, String method, String name) {
        String c = controllerClass != null ? controllerClass : "";
        String p = path != null ? path : "";
        String t = method != null ? method : "";
        String n = name != null ? name : "";
        return (c + "|" + t + "|" + p + "|" + n).replace(' ', '_');
    }
}
