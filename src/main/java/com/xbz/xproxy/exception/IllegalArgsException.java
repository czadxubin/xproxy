package com.xbz.xproxy.exception;

import java.util.List;

/**
 * 不合法的参数异常
 */
public class IllegalArgsException extends BusinessException {
    private final List<String> illegalArgs;

    public IllegalArgsException(List<String> illegalArgs) {
        this.illegalArgs = illegalArgs;
        StringBuilder sb = new StringBuilder();
        sb.append("不合法的参数异常，异常情况如下：\n");
        for (String illegalArg : illegalArgs) {
            sb.append(illegalArg);
        }
        this.errMsg = sb.toString();
    }
}
