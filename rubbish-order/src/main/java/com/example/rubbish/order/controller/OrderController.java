package com.example.rubbish.order.controller;

import com.example.rubbish.common.result.Result;
import com.example.rubbish.order.dto.OrderDTO;
import com.example.rubbish.order.entity.Order;
import com.example.rubbish.order.service.OrderService;
import com.example.rubbish.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<String> create(@Valid @RequestBody OrderDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @GetMapping("/{orderNo}")
    public Result<Order> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Boolean> cancel(@PathVariable String orderNo) {
        return Result.success(orderService.cancelOrder(orderNo));
    }

    @GetMapping("/user/{userId}")
    public Result<List<OrderVO>> listByUserId(@PathVariable Long userId) {
        log.info("查询用户订单列表，用户ID：{}", userId);
        return Result.success(orderService.listByUserId(userId));
    }
}
