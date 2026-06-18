package com.example.rubbish.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.rubbish.common.exception.BusinessException;
import com.example.rubbish.common.result.Result;
import com.example.rubbish.order.dto.OrderDTO;
import com.example.rubbish.order.entity.Order;
import com.example.rubbish.order.feign.UserFeignClient;
import com.example.rubbish.order.mapper.OrderMapper;
import com.example.rubbish.order.service.OrderService;
import com.example.rubbish.order.vo.OrderVO;
import com.example.rubbish.order.vo.UserVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final UserFeignClient userFeignClient;

    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderDTO dto) {
        log.info("开始创建订单，参数：{}", dto);

        // 验证用户是否存在
        Result<UserVO> userResult = userFeignClient.getById(dto.getUserId());
        if (userResult.getCode() != 200 || userResult.getData() == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查订单号是否已存在
        Order existOrder = this.lambdaQuery()
                .eq(Order::getOrderNo, dto.getOrderNo())
                .one();
        if (existOrder != null) {
            throw new BusinessException("订单号已存在");
        }

        // 计算总金额
        BigDecimal totalAmount = dto.getUnitPrice()
                .multiply(new BigDecimal(dto.getQuantity()));

        // 创建订单
        Order order = new Order();
        BeanUtil.copyProperties(dto, order);
        order.setTotalAmount(totalAmount);
        order.setStatus(0);

        this.save(order);

        log.info("订单创建成功，订单号：{}", order.getOrderNo());

        // 发送订单创建消息到 RabbitMQ
        // rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, RabbitMqConfig.ORDER_CREATED_ROUTING_KEY, order);

        return order.getOrderNo();
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return this.lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(String orderNo) {
        Order order = this.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }

        order.setStatus(4);
        this.updateById(order);

        log.info("订单取消成功，订单号：{}", orderNo);

        return true;
    }

    @Override
    public List<OrderVO> listByUserId(Long userId) {
        log.info("查询用户订单列表，用户ID：{}", userId);

        List<Order> orders = this.lambdaQuery()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime)
                .list();

        return orders.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo);
        return vo;
    }
}
