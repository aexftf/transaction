package com.example.rubbish.order.service.example;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seata 四种分布式事务模式代码示例
 *
 * 重点：项目用的是 AT 模式，其他模式了解即可
 */

// ============================================================
// 模式一：AT 模式（项目在用）
// ============================================================

/**
 * AT 模式 = Automatic Transaction（自动事务）
 *
 * 特点：
 * 1. 只需要加 @GlobalTransactional 注解
 * 2. 不需要写额外代码
 * 3. 通过 undo_log 表自动回滚
 */
@Slf4j
@Service
class AT_Mode_Example {

    /**
     * AT 模式示例 - 就这么简单
     *
     * 原理：
     * 1. 执行前：Seata 自动记录数据快照到 undo_log
     * 2. 执行业务 SQL
     * 3. 成功：删除 undo_log
     * 4. 失败：根据 undo_log 自动生成反向 SQL 回滚
     */
    @GlobalTransactional(name = "at-example", rollbackFor = Exception.class)
    public void atExample(Long userId, String productName, Integer quantity) {
        log.info("=== AT 模式示例 ===");

        // 直接写业务代码，不用管回滚
        // 扣余额
        deductBalance(userId, 100);
        // 创建订单
        createOrder(userId, productName, quantity);

        // 如果这里抛异常，Seata 会自动根据 undo_log 回滚上面的操作
    }

    private void deductBalance(Long userId, Integer amount) {
        // UPDATE account SET balance = balance - 100 WHERE user_id = ?
        // Seata 会自动记录这条记录修改前的值到 undo_log
    }

    private void createOrder(Long userId, String productName, Integer quantity) {
        // INSERT INTO orders (user_id, product_name, quantity) VALUES (?, ?, ?)
        // Seata 会自动记录这条记录到 undo_log
    }
}


// ============================================================
// 模式二：TCC 模式（Try-Confirm-Cancel）
// ============================================================

/**
 * TCC 接口定义
 *
 * 需要定义三个方法：
 * 1. Try：预留资源（不真正执行）
 * 2. Confirm：确认执行（真正执行）
 * 3. Cancel：取消执行（回滚）
 */
@LocalTCC
interface AccountTCCService {

    /**
     * Try 阶段：预留资源
     *
     * @TwoPhaseBusinessAction 注解指定：
     *   - commitMethod = "confirm"：成功时调用的方法
     *   - rollbackMethod = "cancel"：失败时调用的方法
     */
    @TwoPhaseBusinessAction(name = "deductBalance", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(
            @BusinessActionContextParameter(paramName = "userId") Long userId,
            @BusinessActionContextParameter(paramName = "amount") Integer amount
    );

    /**
     * Confirm 阶段：真正扣除
     */
    boolean confirm(BusinessActionContext context);

    /**
     * Cancel 阶段：释放预留
     */
    boolean cancel(BusinessActionContext context);
}

/**
 * TCC 实现
 */
@Slf4j
@Service
class AccountTCCServiceImpl implements AccountTCCService {

    /**
     * Try：冻结余额，不真正扣除
     *
     * 比如用户有 1000 元，要扣 100 元
     * Try 阶段：冻结 100 元，可用余额变 900，冻结余额变 100
     */
    @Override
    public boolean tryDeduct(Long userId, Integer amount) {
        log.info("=== TCC Try 阶段：冻结余额 ===");
        // UPDATE account
        // SET balance = balance - 100, frozen = frozen + 100
        // WHERE user_id = ? AND balance >= 100
        return true;
    }

    /**
     * Confirm：真正扣除冻结的余额
     *
     * 事务成功提交时调用
     */
    @Override
    public boolean confirm(BusinessActionContext context) {
        log.info("=== TCC Confirm 阶段：确认扣除 ===");
        Long userId = context.getActionContext("userId", Long.class);
        Integer amount = context.getActionContext("amount", Integer.class);

        // UPDATE account SET frozen = frozen - 100 WHERE user_id = ?
        return true;
    }

    /**
     * Cancel：释放冻结的余额
     *
     * 事务失败回滚时调用
     */
    @Override
    public boolean cancel(BusinessActionContext context) {
        log.info("=== TCC Cancel 阶段：释放冻结 ===");
        Long userId = context.getActionContext("userId", Long.class);
        Integer amount = context.getActionContext("amount", Integer.class);

        // UPDATE account
        // SET balance = balance + 100, frozen = frozen - 100
        // WHERE user_id = ?
        return true;
    }
}

/**
 * TCC 模式使用示例
 */
@Slf4j
@Service
class TCC_Mode_Example {

    private final AccountTCCService accountTCCService;

    TCC_Mode_Example(AccountTCCService accountTCCService) {
        this.accountTCCService = accountTCCService;
    }

    /**
     * TCC 模式使用
     *
     * 同样用 @GlobalTransactional，但内部调用的是 TCC 接口
     * Seata 会自动调用 Try → Confirm 或 Try → Cancel
     */
    @GlobalTransactional(name = "tcc-example", rollbackFor = Exception.class)
    public void tccExample(Long userId, String productName, Integer amount) {
        log.info("=== TCC 模式示例 ===");

        // 调用 Try 方法（冻结余额）
        accountTCCService.tryDeduct(userId, amount);

        // 创建订单
        createOrder(userId, productName);

        // 成功：Seata 自动调用 Confirm
        // 失败：Seata 自动调用 Cancel
    }

    private void createOrder(Long userId, String productName) {
        // INSERT INTO orders ...
    }
}


// ============================================================
// 模式三：SAGA 模式（状态机编排）
// ============================================================

/**
 * SAGA 模式 = 长事务编排
 *
 * 特点：
 * 1. 适合流程长的业务
 * 2. 每个步骤要配一个补偿动作
 * 3. 通过状态机定义流程
 *
 * 以下伪代码展示 SAGA 的思想
 */
@Slf4j
@Service
class SAGA_Mode_Example {

    /**
     * SAGA 模式示例：订机票流程
     *
     * 流程：扣款 → 出票 → 发短信 → 发邮件
     * 每个步骤都有对应的补偿动作
     */
    public void sagaExample(Long userId, String flightNo) {
        log.info("=== SAGA 模式示例 ===");

        try {
            // 步骤1：扣款
            deductPayment(userId, 1000);
            log.info("步骤1完成：扣款");

            // 步骤2：出票
            issueTicket(userId, flightNo);
            log.info("步骤2完成：出票");

            // 步骤3：发短信
            sendSms(userId, "出票成功");
            log.info("步骤3完成：发短信");

            // 步骤4：发邮件
            sendEmail(userId, "您的机票已出票");
            log.info("步骤4完成：发邮件");

        } catch (Exception e) {
            log.error("流程失败，开始补偿：{}", e.getMessage());
            // 按相反顺序执行补偿动作
            compensate(userId, flightNo);
        }
    }

    // ========== 正向动作 ==========

    private void deductPayment(Long userId, Integer amount) {
        // 扣款逻辑
    }

    private void issueTicket(Long userId, String flightNo) {
        // 出票逻辑
        throw new RuntimeException("出票失败"); // 模拟失败
    }

    private void sendSms(Long userId, String message) {
        // 发短信逻辑
    }

    private void sendEmail(Long userId, String message) {
        // 发邮件逻辑
    }

    // ========== 补偿动作 ==========

    private void compensate(Long userId, String flightNo) {
        log.info("开始补偿流程...");

        // 补偿步骤2：退票
        refundTicket(userId, flightNo);

        // 补偿步骤1：退款
        refundPayment(userId, 1000);
    }

    private void refundTicket(Long userId, String flightNo) {
        log.info("补偿：退票");
        // DELETE FROM ticket WHERE user_id = ? AND flight_no = ?
    }

    private void refundPayment(Long userId, Integer amount) {
        log.info("补偿：退款");
        // UPDATE account SET balance = balance + ? WHERE user_id = ?
    }

    /**
     * SAGA 状态机定义（实际使用 json 文件）
     *
     * {
     *   "Name": "bookFlight",
     *   "Steps": [
     *     {
     *       "Service": "paymentService",
     *       "Method": "deduct",
     *       "Compensate": "refund"
     *     },
     *     {
     *       "Service": "ticketService",
     *       "Method": "issue",
     *       "Compensate": "cancel"
     *     },
     *     {
     *       "Service": "notifyService",
     *       "Method": "sendSms",
     *       "Compensate": "none"
     *     }
     *   ]
     * }
     */
}


// ============================================================
// 模式四：XA 模式（数据库两阶段提交）
// ============================================================

/**
 * XA 模式 = 数据库标准两阶段提交协议
 *
 * 特点：
 * 1. 依赖数据库本身的 XA 协议支持
 * 2. 强一致性，但性能差
 * 3. 事务期间锁定资源，并发性能低
 *
 * 代码和 AT 模式一样，只是配置不同
 */
@Slf4j
@Service
class XA_Mode_Example {

    /**
     * XA 模式示例
     *
     * 代码写法和 AT 一样，区别在于：
     * 1. 数据源配置要使用 XADataSource
     * 2. Seata 配置 data-source-proxy-mode: XA
     */
    @GlobalTransactional(name = "xa-example", rollbackFor = Exception.class)
    public void xaExample(Long userId, String productName, Integer amount) {
        log.info("=== XA 模式示例 ===");

        // 这里的代码和 AT 模式完全一样
        // 区别在底层实现：
        // AT：记录 undo_log，事后补偿
        // XA：数据库层面两阶段锁定

        deductBalance(userId, amount);
        createOrder(userId, productName);
    }

    private void deductBalance(Long userId, Integer amount) {
        // UPDATE account SET balance = balance - ? WHERE user_id = ?
        // XA 模式下，这条 SQL 会锁定记录直到全局事务提交
    }

    private void createOrder(Long userId, String productName) {
        // INSERT INTO orders ...
    }
}

/**
 * XA 模式数据源配置示例
 *
 * # application.yml
 * spring:
 *   datasource:
 *     type: com.alibaba.druid.pool.xa.DruidXADataSource
 *     driver-class-name: com.mysql.cj.jdbc.Driver
 *     url: jdbc:mysql://localhost:3306/rubbish_order
 *
 * seata:
 *   data-source-proxy-mode: XA  # 关键配置
 */


// ============================================================
// 四种模式对比总结
// ============================================================

/**
 * ┌──────────┬──────────────────┬────────────┬────────────┐
 * │   模式   │       特点       │   性能     │  适用场景  │
 * ├──────────┼──────────────────┼────────────┼────────────┤
 * │   AT     │ 自动回滚，零侵入 │    高      │ 大多数场景 │
 * │   TCC    │ 手写三段代码     │    中      │ 强一致要求 │
 * │  SAGA    │ 长事务，配补偿   │    中      │ 长流程业务 │
 * │   XA     │ 数据库锁定       │    低      │ 很少用     │
 * └──────────┴──────────────────┴────────────┴────────────┘
 *
 * 项目推荐：AT 模式（默认，最简单）
 *
 * 面试回答：
 * 我们项目用的是 AT 模式，因为它是无侵入的，只需要加个注解，
 * Seata 会自动通过 undo_log 实现回滚。TCC 需要手写三段代码，
 * 适合对一致性要求特别高的场景。SAGA 适合长流程业务。XA 性能差，
 * 因为事务期间会锁定资源。
 */
