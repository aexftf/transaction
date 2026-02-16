package com.example.rubbish.user.feign;

import com.example.rubbish.common.result.Result;
import com.example.rubbish.user.dto.OrderDTO;
import com.example.rubbish.user.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "rubbish-order", fallbackFactory = OrderFeignFallback.class)
public interface OrderFeignClient {

    @GetMapping("/order/{orderNo}")
    Result<OrderVO> getByOrderNo(@PathVariable("orderNo") String orderNo);

    @PostMapping("/order")
    Result<String> create(@RequestBody OrderDTO dto);

    @GetMapping("/order/user/{userId}")
    Result<List<OrderVO>> listByUserId(@PathVariable("userId") Long userId);

    @PostMapping("/order/{orderNo}/cancel")
    Result<Boolean> cancel(@PathVariable("orderNo") String orderNo);
}
