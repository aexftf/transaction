package com.example.rubbish.auth.feign;

import com.example.rubbish.auth.dto.UserDTO;
import com.example.rubbish.auth.vo.UserVO;
import com.example.rubbish.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "rubbish-user")
public interface UserFeignClient {

    @GetMapping("/user/{id}")
    Result<UserVO> getById(@PathVariable("id") Long id);

    @GetMapping("/user/username/{username}")
    Result<UserVO> getByUsername(@PathVariable("username") String username);
}
