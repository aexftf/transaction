package com.example.rubbish.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.rubbish.user.entity.User;
import com.example.rubbish.user.dto.UserDTO;
import com.example.rubbish.user.vo.OrderVO;
import com.example.rubbish.user.vo.UserVO;

import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    User getByPhone(String phone);

    /**
     * 创建用户
     */
    Long createUser(UserDTO dto);

    /**
     * 更新用户
     */
    Boolean updateUser(UserDTO dto);

    /**
     * 根据ID查询用户VO
     */
    UserVO getUserVOById(Long id);

    /**
     * 根据用户名查询用户VO
     */
    UserVO getUserVOByUsername(String username);

    /**
     * 获取用户订单列表（远程调用订单服务）
     */
    List<OrderVO> getUserOrders(Long userId);

    /**
     * 为用户创建订单（远程调用订单服务）
     */
    String createUserOrder(Long userId, String productName, Integer quantity, java.math.BigDecimal unitPrice);
}
