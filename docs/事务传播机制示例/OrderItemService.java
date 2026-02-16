package com.example.rubbish.order.service.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单明细服务 - 演示 NESTED 和 REQUIRED
 */
@Slf4j
@Service
public class OrderItemService {

    /**
     * REQUIRED: 默认行为，加入外部事务
     *
     * 特点：
     * 1. 有外部事务就加入
     * 2. 和外部方法在同一个事务中
     *
     * 结果：
     * - 任一失败，全部回滚
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createItemWithRequired(String itemName) {
        log.info("[REQUIRED] 创建明细: {}", itemName);
        // INSERT INTO order_item (item_name) VALUES (?)

        // 和外部方法在同一个事务中，同生共死
    }

    /**
     * NESTED: 嵌套事务（基于 Savepoint）
     *
     * 特点：
     * 1. 在外部事务中创建一个保存点（Savepoint）
     * 2. 失败时回滚到保存点，而不是整个事务
     *
     * 结果：
     * - 内部失败：只回滚内部，外部可以 catch 后继续
     * - 外部失败：内部也一起回滚（因为是子事务）
     *
     * 适用场景：
     * - 批量处理，单个失败不影响整体
     * - 部分失败可以接受的业务
     */
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void createItemWithNested(String itemName) {
        log.info("[NESTED] 创建明细: {}", itemName);
        // INSERT INTO order_item (item_name) VALUES (?)

        // 模拟某些明细创建失败
        if (itemName.contains("error") || itemName.contains("fail")) {
            throw new RuntimeException("明细创建失败: " + itemName);
        }
    }

    /**
     * REQUIRES_NEW: 独立事务
     *
     * 特点：
     * 1. 总是新建独立事务
     * 2. 和外部事务完全隔离
     *
     * 结果：
     * - 内部失败：只回滚内部
     * - 外部失败：内部不回滚（已提交）
     *
     * 与 NESTED 的区别：
     * - NESTED: 外部失败，内部也回滚（父子关系）
     * - REQUIRES_NEW: 外部失败，内部不回滚（完全独立）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createItemWithRequiresNew(String itemName) {
        log.info("[REQUIRES_NEW] 创建明细: {}", itemName);
        // INSERT INTO order_item (item_name) VALUES (?)

        // 完全独立的事务，不受外部影响
    }
}
