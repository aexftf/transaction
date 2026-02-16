package com.example.rubbish.order.feign;

import com.example.rubbish.common.result.Result;
import com.example.rubbish.order.dto.UserDTO;
import com.example.rubbish.order.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignFallback implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public Result<UserVO> getById(Long id) {
                log.error("调用用户服务失败，参数：{}", id, cause);
                return Result.error("用户服务暂不可用");
            }

            @Override
            public Result<UserVO> getByUsername(String username) {
                log.error("调用用户服务失败，参数：{}", username, cause);
                return Result.error("用户服务暂不可用");
            }

            @Override
            public Result<Long> create(UserDTO dto) {
                log.error("调用用户服务失败，参数：{}", dto, cause);
                return Result.error("用户服务暂不可用");
            }

        };
    }
}
