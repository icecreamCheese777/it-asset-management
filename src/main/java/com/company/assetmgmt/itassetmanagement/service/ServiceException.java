package com.company.assetmgmt.itassetmanagement.service;

/**
 * 业务异常：message 是可以直接展示给用户的中文提示。
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
