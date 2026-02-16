package com.example.rubbish.auth.service.impl;

import com.example.rubbish.auth.dto.LoginDTO;
import com.example.rubbish.auth.feign.UserFeignClient;
import com.example.rubbish.auth.service.AuthService;
import com.example.rubbish.auth.vo.LoginVO;
import com.example.rubbish.auth.vo.UserVO;
import com.example.rubbish.common.exception.BusinessException;
import com.example.rubbish.common.result.Result;
import com.example.rubbish.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserFeignClient userFeignClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "auth:token:";
    private static final long TOKEN_EXPIRE = 24;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 调用用户服务验证用户
        Result<UserVO> userResult = userFeignClient.getByUsername(dto.getUsername());
        if (userResult.getCode() != 200 || userResult.getData() == null) {
            throw new BusinessException("用户不存在");
        }

        UserVO user = userResult.getData();

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 存储Token到Redis
        String redisKey = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, user.getId(), TOKEN_EXPIRE, TimeUnit.HOURS);

        log.info("用户登录成功，用户名：{}", dto.getUsername());

        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void logout(String token) {
        String redisKey = TOKEN_PREFIX + token;
        redisTemplate.delete(redisKey);
        log.info("用户登出成功");
    }

    @Override
    public String refreshToken(String token) {
        String redisKey = TOKEN_PREFIX + token;
        Long userId = (Long) redisTemplate.opsForValue().get(redisKey);

        if (userId == null) {
            throw new BusinessException("Token已过期，请重新登录");
        }

        // 获取用户信息
        Result<UserVO> userResult = userFeignClient.getById(userId);
        if (userResult.getCode() != 200 || userResult.getData() == null) {
            throw new BusinessException("用户不存在");
        }

        UserVO user = userResult.getData();

        // 生成新Token
        String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 删除旧Token，存储新Token
        redisTemplate.delete(redisKey);
        String newRedisKey = TOKEN_PREFIX + newToken;
        redisTemplate.opsForValue().set(newRedisKey, user.getId(), TOKEN_EXPIRE, TimeUnit.HOURS);

        return newToken;
    }
}
