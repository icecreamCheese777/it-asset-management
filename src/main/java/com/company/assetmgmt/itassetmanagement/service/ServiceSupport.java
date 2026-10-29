package com.company.assetmgmt.itassetmanagement.service;

import java.util.Objects;

/**
 * Service 层共用的参数校验与异常转换工具。
 */
final class ServiceSupport {

    private ServiceSupport() {
    }

    static String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ServiceException(message);
        }
        return trimmed;
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 把 MyBatis/JDBC 异常转换成用户能看懂的 {@link ServiceException}。
     *
     * @param duplicateMessage 唯一键冲突时的提示；为 null 时使用通用提示
     */
    static ServiceException translate(RuntimeException e, String duplicateMessage, String action) {
        if (e instanceof ServiceException serviceException) {
            return serviceException;
        }

        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (duplicateMessage != null && message != null && message.contains("UNIQUE constraint failed")) {
            return new ServiceException(duplicateMessage, e);
        }

        String detail = Objects.requireNonNullElse(message, "未知错误");
        return new ServiceException(action + "失败：" + detail, e);
    }
}
