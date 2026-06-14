package com.novelweaver.util;

/*
 * Validation Utilities / 校验工具 / 検証ユーティリティ
 *
 * CN 项目 ID、参数格式校验
 * JP プロジェクトID、パラメータフォーマット検証
 * EN Project ID and parameter format validation
 */

import java.util.UUID;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Validate and parse project ID into UUID.
     * Throws IllegalArgumentException with a clear message on invalid format.
     *
     * @param projectId raw project ID string
     * @return parsed UUID
     * @throws IllegalArgumentException if blank or invalid UUID format
     */
    public static UUID requireProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
        try {
            return UUID.fromString(projectId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid projectId format: " + projectId);
        }
    }
}
