package com.example.rubbish.common.feign;

import com.example.rubbish.common.constant.ResultCode;
import com.example.rubbish.common.exception.BusinessException;
import com.example.rubbish.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FeignException {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handleError(String serviceName, Response response) {
        try {
            if (response.body() != null) {
                String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                log.error("Feign调用失败，服务：{}，响应：{}", serviceName, body);
                throw new BusinessException(ResultCode.RPC_ERROR.getCode(), "服务调用失败：" + serviceName);
            }
        } catch (IOException e) {
            log.error("Feign异常处理失败：{}", e.getMessage());
        }
        throw new BusinessException(ResultCode.RPC_ERROR);
    }
}
