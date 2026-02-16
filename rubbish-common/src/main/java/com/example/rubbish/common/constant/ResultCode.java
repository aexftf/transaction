package com.example.rubbish.common.constant;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    PARAM_ERROR(400, "参数错误"),
    PARAM_MISS(401, "参数缺失"),
    PARAM_TYPE_ERROR(402, "参数类型错误"),
    PARAM_BIND_ERROR(403, "参数绑定错误"),
    PARAM_VALID_ERROR(404, "参数校验错误"),

    BUSINESS_ERROR(1000, "业务错误"),
    SYSTEM_ERROR(1001, "系统错误"),
    NETWORK_ERROR(1002, "网络错误"),
    DATABASE_ERROR(1003, "数据库错误"),

    USER_NOT_EXIST(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "密码错误"),
    USER_ACCOUNT_LOCKED(2003, "账户已锁定"),
    USER_ACCOUNT_EXPIRED(2004, "账户已过期"),

    TOKEN_ERROR(3001, "Token错误"),
    TOKEN_EXPIRED(3002, "Token已过期"),
    TOKEN_INVALID(3003, "Token无效"),

    RPC_ERROR(4001, "远程调用失败"),
    RATE_LIMIT_ERROR(4002, "请求过于频繁"),
    DEGRADATION_ERROR(4003, "服务降级"),

    TRANSACTION_ERROR(5001, "事务异常"),
    MESSAGE_SEND_ERROR(5002, "消息发送失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
