package com.example.rubbish.auth.controller;

import com.example.rubbish.auth.dto.LoginDTO;
import com.example.rubbish.auth.service.AuthService;
import com.example.rubbish.auth.vo.LoginVO;
import com.example.rubbish.common.annotations.IgnoreAuth;
import com.example.rubbish.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @IgnoreAuth
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    @IgnoreAuth
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return Result.success();
    }

    @PostMapping("/refresh")
    @IgnoreAuth
    public Result<String> refreshToken(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        String newToken = authService.refreshToken(token);
        return Result.success(newToken);
    }
}
