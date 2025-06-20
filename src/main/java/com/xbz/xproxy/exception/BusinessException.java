package com.xbz.xproxy.exception;

import lombok.Getter;

public class BusinessException extends RuntimeException {
    @Getter
    protected String errMsg;

    public BusinessException(String errMsg) {
        this(errMsg, null);
    }
    public BusinessException() {
        this(null, null);
    }

    public BusinessException(String errMsg, Throwable cause) {
        super(errMsg, cause);
        this.errMsg = errMsg;
    }
}
