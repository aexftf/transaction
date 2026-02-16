package com.example.rubbish.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.rubbish.common.exception.BusinessException;
import com.example.rubbish.common.result.Result;
import com.example.rubbish.user.dto.OrderDTO;
import com.example.rubbish.user.dto.UserDTO;
import com.example.rubbish.user.entity.User;
import com.example.rubbish.user.feign.OrderFeignClient;
import com.example.rubbish.user.mapper.UserMapper;
import com.example.rubbish.user.service.UserService;
import com.example.rubbish.user.vo.OrderVO;
import com.example.rubbish.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final OrderFeignClient orderFeignClient;

    @Override
    public User getByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public User getByPhone(String phone) {
        return this.lambdaQuery()
                .eq(User::getPhone, phone)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO dto) {
        // 检查用户名是否存在
        User existUser = this.getByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否存在
        if (dto.getPhone() != null) {
            existUser = this.getByPhone(dto.getPhone());
            if (existUser != null) {
                throw new BusinessException("手机号已存在");
            }
        }

        User user = new User();
        BeanUtil.copyProperties(dto, user);

        // 加密密码
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        this.save(user);

        log.info("创建用户成功，用户ID：{}", user.getId());

        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }

        User user = this.getById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查用户名是否被其他用户占用
        User existUser = this.lambdaQuery()
                .eq(User::getUsername, dto.getUsername())
                .ne(User::getId, dto.getId())
                .one();
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        BeanUtil.copyProperties(dto, user);

        // 加密新密码
        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        this.updateById(user);

        log.info("更新用户成功，用户ID：{}", user.getId());

        return true;
    }

    @Override
    public UserVO getUserVOById(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserVOByUsername(String username) {
        User user = this.getByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        // UserVO 不包含密码字段
        return vo;
    }

    @Override
    public List<OrderVO> getUserOrders(Long userId) {
        log.info("远程调用订单服务，查询用户订单，用户ID：{}", userId);

        try {
            Result<List<OrderVO>> result = orderFeignClient.listByUserId(userId);
            if (result.getCode() == 200 && result.getData() != null) {
                log.info("查询用户订单成功，订单数量：{}", result.getData().size());
                return result.getData();
            } else {
                log.warn("查询用户订单失败：{}", result.getMessage());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("远程调用订单服务异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String createUserOrder(Long userId, String productName, Integer quantity, java.math.BigDecimal unitPrice) {
        log.info("远程调用订单服务创建订单，用户ID：{}，产品：{}", userId, productName);

        // 验证用户是否存在
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 构建订单DTO
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setOrderNo(IdUtil.simpleUUID());
        orderDTO.setUserId(userId);
        orderDTO.setProductName(productName);
        orderDTO.setQuantity(quantity);
        orderDTO.setUnitPrice(unitPrice);
        orderDTO.setStatus(0);

        try {
            Result<String> result = orderFeignClient.create(orderDTO);
            if (result.getCode() == 200 && result.getData() != null) {
                log.info("创建订单成功，订单号：{}", result.getData());
                return result.getData();
            } else {
                throw new BusinessException("创建订单失败：" + result.getMessage());
            }
        } catch (Exception e) {
            log.error("远程调用订单服务创建订单异常", e);
            throw new BusinessException("创建订单失败：" + e.getMessage());
        }
    }
}
