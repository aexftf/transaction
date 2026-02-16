package com.example.rubbish.auth.service;

import com.example.rubbish.auth.dto.LoginDTO;
import com.example.rubbish.auth.vo.LoginVO;

public interface AuthService {

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 刷新Token
     */
    String refreshToken(String token);
}
