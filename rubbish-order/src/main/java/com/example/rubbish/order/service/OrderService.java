package com.example.rubbish.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.rubbish.order.entity.Order;
import com.example.rubbish.order.dto.OrderDTO;
import com.example.rubbish.order.vo.OrderVO;

import java.util.List;

public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    String createOrder(OrderDTO dto);

    /**
     * 根据订单号查询
     */
    Order getByOrderNo(String orderNo);

    /**
     * 取消订单
     */
    Boolean cancelOrder(String orderNo);

    /**
     * 根据用户ID查询订单列表
     */
    List<OrderVO> listByUserId(Long userId);
}
