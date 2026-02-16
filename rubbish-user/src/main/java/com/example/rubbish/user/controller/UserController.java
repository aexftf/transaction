package com.example.rubbish.user.controller;

import com.example.rubbish.common.annotations.IgnoreAuth;
import com.example.rubbish.common.result.Result;
import com.example.rubbish.user.dto.UserDTO;
import com.example.rubbish.user.service.UserService;
import com.example.rubbish.user.vo.OrderVO;
import com.example.rubbish.user.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Api(tags = "用户管理")
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户")
    public Result<UserVO> getById(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        return Result.success(userService.getUserVOById(id));
    }

    @GetMapping("/username/{username}")
    @ApiOperation("根据用户名查询用户")
    public Result<UserVO> getByUsername(@PathVariable @NotNull(message = "用户名不能为空") String username) {
        return Result.success(userService.getUserVOByUsername(username));
    }

    @PostMapping
    @ApiOperation("创建用户")
    public Result<Long> create(@Valid @RequestBody UserDTO dto) {
        return Result.success(userService.createUser(dto));
    }

    @PutMapping
    @ApiOperation("更新用户")
    public Result<Boolean> update(@Valid @RequestBody UserDTO dto) {
        return Result.success(userService.updateUser(dto));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public Result<Boolean> delete(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        return Result.success(userService.removeById(id));
    }

    @PostMapping("/list")
    @ApiOperation("查询用户列表")
    public Result<List<UserVO>> list(@RequestBody UserDTO dto) {
        return Result.success(userService.list().stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    org.springframework.beans.BeanUtils.copyProperties(user, vo);
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList()));
    }

    // ==================== Feign 远程调用接口 ====================

    @GetMapping("/{userId}/orders")
    @ApiOperation("查询用户订单列表（Feign远程调用）")
    public Result<List<OrderVO>> getUserOrders(@PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        log.info("查询用户订单列表，用户ID：{}", userId);
        return Result.success(userService.getUserOrders(userId));
    }

    @PostMapping("/{userId}/orders")
    @ApiOperation("为用户创建订单（Feign远程调用）")
    public Result<String> createUserOrder(
            @PathVariable @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "产品名称不能为空") String productName,
            @RequestParam @NotNull(message = "数量不能为空") Integer quantity,
            @RequestParam @NotNull(message = "单价不能为空") BigDecimal unitPrice) {
        log.info("为用户创建订单，用户ID：{}，产品：{}，数量：{}，单价：{}", userId, productName, quantity, unitPrice);
        String orderNo = userService.createUserOrder(userId, productName, quantity, unitPrice);
        return Result.success(orderNo);
    }
}
