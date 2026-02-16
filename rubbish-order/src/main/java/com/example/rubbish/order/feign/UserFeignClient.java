package com.example.rubbish.order.feign;

import com.example.rubbish.common.result.Result;
import com.example.rubbish.order.dto.UserDTO;
import com.example.rubbish.order.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "rubbish-user", fallbackFactory = UserFeignFallback.class)
public interface UserFeignClient {

    @GetMapping("/user/{id}")
    Result<UserVO> getById(@PathVariable("id") Long id);

    @GetMapping("/user/username/{username}")
    Result<UserVO> getByUsername(@PathVariable("username") String username);

    @PostMapping("/user")
    Result<Long> create(@RequestBody UserDTO dto);
}
