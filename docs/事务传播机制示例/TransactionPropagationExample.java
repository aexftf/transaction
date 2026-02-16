package com.example.rubbish.order.service.example;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 事务传播行为示例
 *
 * 演示 REQUIRED、REQUIRES_NEW、NESTED 三种常用传播行为的区别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionPropagationExample {

    private final OrderLogService orderLogService;
    private final OrderItemService orderItemService;

    /**
     * 场景1: REQUIRED（默认）
     *
     * 内部方法和外部方法在同一个事务中
     * 任一方法失败，全部回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void requiredExample() {
        log.info("=== REQUIRED 示例 ===");

        // 创建订单主表
        createOrder();

        // 创建订单明细（使用默认 REQUIRED）
        orderItemService.createItemWithRequired("ITEM-001");

        // 如果这里抛异常，上面的操作全部回滚
        // throw new RuntimeException("模拟失败");
    }

    /**
     * 场景2: REQUIRES_NEW
     *
     * 内部方法总是新建独立事务
     * 即使外部事务失败，内部已提交的操作不会回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void requiresNewExample() {
        log.info("=== REQUIRES_NEW 示例 ===");

        // 创建订单主表
        createOrder();

        // 记录日志（使用 REQUIRES_NEW，独立事务）
        orderLogService.saveLogWithRequiresNew("订单创建成功");

        // 即使这里失败，日志已经提交，不会回滚
        throw new RuntimeException("模拟外部事务失败");
    }

    /**
     * 场景3: NESTED
     *
     * 内部方法是外部事务的子事务（Savepoint）
     * - 内部失败：只回滚内部，外部继续
     * - 外部失败：内部也一起回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void nestedExample(List<String> itemNames) {
        log.info("=== NESTED 示例：批量创建订单明细 ===");

        // 创建订单主表
        createOrder();

        int success = 0;
        int fail = 0;

        // 批量创建订单明细，单个失败不影响整体
        for (String itemName : itemNames) {
            try {
                orderItemService.createItemWithNested(itemName);
                success++;
            } catch (Exception e) {
                log.warn("创建明细失败: {}, 错误: {}", itemName, e.getMessage());
                fail++;
                // catch 住异常，继续处理下一个
            }
        }

        log.info("批量处理完成，成功: {}, 失败: {}", success, fail);

        // 如果这里抛异常，所有已成功的 NESTED 子事务也会回滚
        // throw new RuntimeException("模拟外部事务失败");
    }

    /**
     * 场景4: 混合使用
     *
     * 实际业务中常见：REQUIRES_NEW 用于日志，NESTED 用于批量处理
     */
    @Transactional(rollbackFor = Exception.class)
    public void mixedExample(List<String> itemNames) {
        log.info("=== 混合使用示例 ===");

        // 1. 记录开始日志（REQUIRES_NEW，无论后续成功失败都保留）
        orderLogService.saveLogWithRequiresNew("开始批量创建订单");

        try {
            // 2. 创建订单主表
            createOrder();

            // 3. 批量创建明细（NESTED，单个失败不影响整体）
            for (String itemName : itemNames) {
                try {
                    orderItemService.createItemWithNested(itemName);
                } catch (Exception e) {
                    log.warn("明细创建失败: {}", itemName);
                }
            }

            // 4. 记录成功日志
            orderLogService.saveLogWithRequiresNew("订单创建成功");

        } catch (Exception e) {
            // 5. 记录失败日志（REQUIRES_NEW，独立事务）
            orderLogService.saveLogWithRequiresNew("订单创建失败: " + e.getMessage());
            throw e;
        }
    }

    private void createOrder() {
        log.info("创建订单主表...");
        // 模拟创建订单
    }
}
