package com.example.rubbish.user.feign;

import com.example.rubbish.common.result.Result;
import com.example.rubbish.user.dto.OrderDTO;
import com.example.rubbish.user.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class OrderFeignFallback implements FallbackFactory<OrderFeignClient> {

    @Override
    public OrderFeignClient create(Throwable cause) {
        return new OrderFeignClient() {
            @Override
            public Result<OrderVO> getByOrderNo(String orderNo) {
                log.error("调用订单服务失败，订单号：{}，错误：{}", orderNo, cause.getMessage());
                return Result.error("订单服务暂不可用");
            }

            @Override
            public Result<String> create(OrderDTO dto) {
                log.error("调用订单服务创建订单失败，错误：{}", cause.getMessage());
                return Result.error("订单服务暂不可用");
            }

            @Override
            public Result<List<OrderVO>> listByUserId(Long userId) {
                log.error("调用订单服务查询用户订单失败，用户ID：{}，错误：{}", userId, cause.getMessage());
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Boolean> cancel(String orderNo) {
                log.error("调用订单服务取消订单失败，订单号：{}，错误：{}", orderNo, cause.getMessage());
                return Result.error("订单服务暂不可用");
            }
        };
    }
}
