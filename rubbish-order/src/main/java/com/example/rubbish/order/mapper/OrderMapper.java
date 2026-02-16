package com.example.rubbish.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rubbish.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
