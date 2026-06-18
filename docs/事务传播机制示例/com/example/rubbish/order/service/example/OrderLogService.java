package com.example.rubbish.order.service.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单日志服务 - 演示 REQUIRES_NEW
 *
 * 适用场景：
 * - 操作日志记录
 * - 发送通知/消息
 * - 无论主业务成功失败，这些操作都要保留
 */
@Service
public class OrderLogService {

    private static final Log log = LogFactory.getLog(OrderLogService.class);

    /**
     * REQUIRES_NEW: 总是新建独立事务
     *
     * 特点：
     * 1. 挂起当前事务（如果有）
     * 2. 创建新事务执行
     * 3. 执行完毕后提交，恢复原事务
     *
     * 结果：
     * - 外部事务失败，这里的日志不会回滚
     * - 这里失败，只回滚这里，不影响外部
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveLogWithRequiresNew(String message) {
        log.info("[REQUIRES_NEW] 保存日志: " + message);
        // INSERT INTO order_log (message, create_time) VALUES (?, NOW())

        // 即使外部事务回滚，这条日志也不会回滚
        // 因为它是在独立的事务中提交的
    }

    /**
     * 对比：使用 REQUIRED（默认）
     *
     * 特点：
     * 1. 有事务就加入，没有就新建
     * 2. 和外部方法在同一个事务中
     *
     * 结果：
     * - 外部事务失败，这里的日志也会回滚
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void saveLogWithRequired(String message) {
        log.info("[REQUIRED] 保存日志: " + message);
        // 和外部方法在同一个事务中，同生共死
    }
}
