package com.example.rubbish.docs;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Spring AOP 切面编程完整示例
 *
 * 依赖：spring-boot-starter-aop（pom.xml 中已添加到 rubbish-common）
 *
 * 目录：
 * 一、AOP 五种通知类型
 * 二、两种切入方式对比（execution vs @annotation）
 * 三、实战案例
 *     1. 接口耗时监控（execution）
 *     2. 操作日志记录（@annotation）
 *     3. 分布式锁（@annotation + Redis）
 *     4. 防重复提交（@annotation + Redis）
 *     5. 权限校验（@annotation）
 *     6. 自动重试（@annotation）
 */

// ============================================================
// 一、AOP 五种通知类型
// ============================================================

/**
 * 通知类型说明：
 *
 * @Before        - 方法执行前运行，无法阻止目标方法执行（除非抛异常）
 * @After         - 方法执行后运行（不管成功还是异常都会执行，类似 finally）
 * @AfterReturning- 方法正常返回后运行，可以拿到返回值
 * @AfterThrowing - 方法抛异常后运行，可以拿到异常对象
 * @Around        - 环绕通知，最强大，能控制是否执行目标方法、修改参数和返回值
 *
 * 执行顺序：
 * @Around(前半段) → @Before → 目标方法 → @AfterReturning/@AfterThrowing → @After → @Around(后半段)
 *
 * 日常使用频率：
 * @Around > @Before > @AfterReturning > @AfterThrowing > @After
 */
@Slf4j
@Aspect
@Component
class NotificationTypeExample {

    // --- @Before：前置通知 ---
    @Before("execution(* com.example.rubbish..controller.*.*(..))")
    public void beforeExample(JoinPoint point) {
        log.info("[Before] 即将执行：{}", point.getSignature().toShortString());
    }

    // --- @After：后置通知（类似 finally，无论成功失败都执行）---
    @After("execution(* com.example.rubbish..controller.*.*(..))")
    public void afterExample(JoinPoint point) {
        log.info("[After] 执行完毕：{}", point.getSignature().toShortString());
    }

    // --- @AfterReturning：返回通知（可以拿到返回值）---
    @AfterReturning(value = "execution(* com.example.rubbish..controller.*.*(..))",
                    returning = "result")
    public void afterReturningExample(JoinPoint point, Object result) {
        log.info("[AfterReturning] {}，返回值：{}", point.getSignature().toShortString(), result);
    }

    // --- @AfterThrowing：异常通知（可以拿到异常对象）---
    @AfterThrowing(value = "execution(* com.example.rubbish..controller.*.*(..))",
                   throwing = "ex")
    public void afterThrowingExample(JoinPoint point, Throwable ex) {
        log.error("[AfterThrowing] {} 抛出异常：{}", point.getSignature().toShortString(), ex.getMessage());
    }

    // --- @Around：环绕通知（最常用，能做所有事）---
    @Around("execution(* com.example.rubbish..controller.*.*(..))")
    public Object aroundExample(ProceedingJoinPoint point) throws Throwable {
        log.info("[Around-前] 开始执行：{}", point.getSignature().toShortString());
        long start = System.currentTimeMillis();

        Object result = point.proceed();  // 调用目标方法，不调就不执行了

        log.info("[Around-后] 执行完成，耗时：{}ms", System.currentTimeMillis() - start);
        return result;  // 必须返回，不然调用方拿不到结果
    }
}


// ============================================================
// 二、两种切入方式对比
// ============================================================

/**
 * 1. execution - 按方法签名匹配（粗粒度，批量拦截）
 *
 *    语法：execution(修饰符? 返回值 包名.类名.方法名(参数) 异常?)
 *
 *    常用写法：
 *    execution(* com.example.rubbish..controller.*.*(..))   所有 controller 的所有方法
 *    execution(* com.example.rubbish..service.*.*(..))      所有 service 的所有方法
 *    execution(public * com.example.rubbish..*.get*(..))    所有 public 的 get 开头方法
 *    execution(* com.example.rubbish..*.*(String, ..))      第一个参数是 String 的方法
 *
 *    通配符：
 *    *   匹配任意字符（一层）
 *    ..  匹配任意层级包 或 任意个数参数
 *
 * 2. @annotation - 按注解匹配（细粒度，精确控制）
 *
 *    写法：@annotation(com.example.xxx.MyAnnotation)
 *    只拦截加了指定注解的方法，可以通过注解传参
 *
 * 3. 组合使用
 *
 *    &&  同时满足
 *    ||  满足其一
 *    !   取反
 *
 *    例：execution(* com.example..controller.*.*(..)) && @annotation(com.example..OperationLog)
 *    表示：controller 包下且加了 @OperationLog 注解的方法
 */


// ============================================================
// 三、实战案例
// ============================================================


// ======================== 案例1：接口耗时监控 ========================

/**
 * 最基础的 AOP 用法
 * 用 execution 拦截所有 controller 方法，超过阈值打警告日志
 * 不需要任何注解，全自动生效
 */
@Slf4j
@Aspect
@Component
class PerformanceAspect {

    private static final long SLOW_THRESHOLD_MS = 1000;

    @Around("execution(* com.example.rubbish..controller.*.*(..))")
    public Object monitor(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = point.proceed();

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > SLOW_THRESHOLD_MS) {
            log.warn("慢接口告警：{}，耗时：{}ms", point.getSignature().toShortString(), elapsed);
        }

        return result;
    }
}


// ======================== 案例2：操作日志记录 ========================

/**
 * 自定义注解 + AOP，记录操作日志
 * 适合审计场景：记录谁在什么时候对什么模块做了什么操作
 */

// --- 注解定义 ---
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface OperationLog {
    String module() default "";       // 操作模块
    String description() default "";  // 操作描述
}

// --- 切面实现 ---
@Slf4j
@Aspect
@Component
class OperationLogAspect {

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();

        // 获取请求信息
        String requestUri = "";
        String requestMethod = "";
        String ip = "";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            requestUri = request.getRequestURI();
            requestMethod = request.getMethod();
            ip = getIpAddress(request);
        }

        // 获取参数（过滤不可序列化的类型）
        String params = getParams(signature, point.getArgs());

        log.info("====== 操作开始 ======");
        log.info("模块：{}", operationLog.module());
        log.info("描述：{}", operationLog.description());
        log.info("方法：{}.{}", className, methodName);
        log.info("请求：{} {}", requestMethod, requestUri);
        log.info("IP：{}", ip);
        log.info("参数：{}", params);

        Object result;
        try {
            result = point.proceed();
            log.info("结果：成功，耗时：{}ms", System.currentTimeMillis() - startTime);
        } catch (Throwable e) {
            log.error("结果：失败（{}），耗时：{}ms", e.getMessage(), System.currentTimeMillis() - startTime);
            throw e;
        } finally {
            log.info("====== 操作结束 ======");
        }

        return result;
    }

    private String getParams(MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        if (paramNames == null || paramNames.length == 0) {
            return "";
        }
        Map<String, Object> paramMap = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            if (arg instanceof HttpServletRequest
                    || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile) {
                continue;
            }
            paramMap.put(paramNames[i], arg);
        }
        try {
            return JSON.toJSONString(paramMap);
        } catch (Exception e) {
            return paramMap.toString();
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

// --- 使用示例 ---
// @PostMapping
// @OperationLog(module = "订单管理", description = "创建订单")
// public Result<String> create(@Valid @RequestBody OrderDTO dto) { ... }
//
// 日志输出：
// ====== 操作开始 ======
// 模块：订单管理
// 描述：创建订单
// 方法：OrderController.create
// 请求：POST /order
// IP：192.168.1.100
// 参数：{"dto":{"userId":1,"orderNo":"ORD001"}}
// 结果：成功，耗时：56ms
// ====== 操作结束 ======


// ======================== 案例3：分布式锁 ========================

/**
 * 防并发场景：同一个订单同时被多个请求操作
 * 用 Redis SETNX 实现简易分布式锁
 *
 * 使用：@DistributedLock(key = "order:#orderNo")
 * key 中的 #参数名 会被替换为实际参数值
 */

// --- 注解定义 ---
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface DistributedLock {
    String key();                // 锁的key，支持 #参数名 占位
    long waitTime() default 3;  // 等待时间，秒
    long leaseTime() default 10;// 持有时间，秒
}

// --- 切面实现 ---
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
class DistributedLockAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint point, DistributedLock distributedLock) throws Throwable {
        String key = "lock:" + parseKey(distributedLock.key(), point);

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", distributedLock.leaseTime(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new RuntimeException("操作频繁，请稍后重试");
        }

        try {
            return point.proceed();
        } finally {
            redisTemplate.delete(key);
        }
    }

    /**
     * 解析 key 中的 #参数名 占位符
     * 例：key = "order:#orderNo"，参数 orderNo = "ORD001"
     * 结果："order:ORD001"
     */
    private String parseKey(String expression, ProceedingJoinPoint point) {
        MethodSignature sig = (MethodSignature) point.getSignature();
        String[] names = sig.getParameterNames();
        Object[] args = point.getArgs();
        String key = expression;
        for (int i = 0; i < names.length; i++) {
            key = key.replace("#" + names[i], String.valueOf(args[i]));
        }
        return key;
    }
}

// --- 使用示例 ---
// @PostMapping("/{orderNo}/cancel")
// @DistributedLock(key = "order:#orderNo", leaseTime = 15)
// public Result<Boolean> cancel(@PathVariable String orderNo) {
//     return Result.success(orderService.cancelOrder(orderNo));
// }
//
// 效果：同一个 orderNo 的取消请求，同一时刻只能有一个在执行
// 其他请求会收到"操作频繁，请稍后重试"


// ======================== 案例4：防重复提交 ========================

/**
 * 防止用户短时间内多次点击提交按钮
 * 用 用户ID + 请求URI + 参数摘要 作为唯一标识
 * 在指定时间窗口内相同请求直接拒绝
 */

// --- 注解定义 ---
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Idempotent {
    int expireTime() default 5;  // 幂等时间窗口，秒
}

// --- 切面实现 ---
@Aspect
@Component
@RequiredArgsConstructor
class IdempotentAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        String userId = request.getHeader("X-User-Id");
        String argsDigest = DigestUtils.md5DigestAsHex(JSON.toJSONBytes(point.getArgs()));
        String key = "idempotent:" + userId + ":" + request.getRequestURI() + ":" + argsDigest;

        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", idempotent.expireTime(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(absent)) {
            throw new RuntimeException("请勿重复提交");
        }

        return point.proceed();
    }
}

// --- 使用示例 ---
// @PostMapping
// @Idempotent(expireTime = 10)
// public Result<String> create(@Valid @RequestBody OrderDTO dto) {
//     return Result.success(orderService.createOrder(dto));
// }
//
// 效果：同一用户10秒内用相同参数提交订单，第二次会被拒绝


// ======================== 案例5：权限校验 ========================

/**
 * 在方法级别做角色校验
 * 从请求头读取用户角色，和注解中配置的允许角色对比
 */

// --- 注解定义 ---
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface RequireRole {
    String[] value();  // 允许的角色列表
}

// --- 切面实现 ---
@Aspect
@Component
class RequireRoleAspect {

    @Before("@annotation(requireRole)")
    public void check(JoinPoint point, RequireRole requireRole) {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        String userRole = request.getHeader("X-User-Role");
        String[] allowed = requireRole.value();

        boolean hasRole = Arrays.asList(allowed).contains(userRole);
        if (!hasRole) {
            throw new RuntimeException("权限不足，需要角色：" + Arrays.toString(allowed));
        }
    }
}

// --- 使用示例 ---
// @DeleteMapping("/{id}")
// @RequireRole({"admin", "manager"})
// public Result<Boolean> delete(@PathVariable Long id) {
//     return Result.success(userService.removeById(id));
// }
//
// 效果：只有请求头 X-User-Role 为 admin 或 manager 的用户才能调用


// ======================== 案例6：自动重试 ========================

/**
 * 调用外部接口时自动重试
 * 适合调用第三方 API、RPC 等不稳定的场景
 */

// --- 注解定义 ---
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Retry {
    int maxAttempts() default 3;  // 最大重试次数
    long delay() default 1000;   // 每次重试间隔，毫秒
}

// --- 切面实现 ---
@Slf4j
@Aspect
@Component
class RetryAspect {

    @Around("@annotation(retry)")
    public Object around(ProceedingJoinPoint point, Retry retry) throws Throwable {
        Throwable lastException = null;

        for (int i = 1; i <= retry.maxAttempts(); i++) {
            try {
                return point.proceed();
            } catch (Throwable e) {
                lastException = e;
                log.warn("第{}次调用失败：{}，方法：{}",
                        i, e.getMessage(), point.getSignature().toShortString());
                if (i < retry.maxAttempts()) {
                    Thread.sleep(retry.delay());
                }
            }
        }

        log.error("重试{}次后仍然失败：{}", retry.maxAttempts(), point.getSignature().toShortString());
        throw lastException;
    }
}

// --- 使用示例 ---
// @Retry(maxAttempts = 3, delay = 2000)
// public UserVO callThirdPartyApi(Long userId) {
//     return restTemplate.getForObject("http://xxx/user/" + userId, UserVO.class);
// }
//
// 效果：调用失败后间隔2秒自动重试，最多3次，全部失败后抛出最后一次异常


// ============================================================
// 总结
// ============================================================

/**
 * 切入方式选择：
 * ┌─────────────┬──────────┬──────────────────────┐
 * │ 场景        │ 切入方式  │ 原因                  │
 * ├─────────────┼──────────┼──────────────────────┤
 * │ 整层统一处理 │ execution│ 不需要逐个标记         │
 * │ 精确控制+传参│@annotation│ 可以通过注解传业务参数  │
 * │ 两者结合    │ 组合表达式 │ 缩小范围 + 精确控制    │
 * └─────────────┴──────────┴──────────────────────┘
 *
 * 通知类型选择：
 * ┌─────────────────┬──────────────────────────────┐
 * │ 通知类型         │ 适用场景                      │
 * ├─────────────────┼──────────────────────────────┤
 * │ @Around          │ 耗时监控、分布式锁、重试、幂等  │
 * │ @Before          │ 权限校验、参数校验              │
 * │ @AfterReturning  │ 响应日志、结果缓存              │
 * │ @AfterThrowing   │ 异常报警、错误统计              │
 * │ @After           │ 资源清理（少用）                │
 * └─────────────────┴──────────────────────────────┘
 *
 * 注意事项：
 * 1. 同一个类内部方法互调不会触发 AOP（因为没走代理），需要注入自身或用 AopContext.currentProxy()
 * 2. private 方法无法被代理，AOP 不生效
 * 3. 多个切面的执行顺序用 @Order(数字) 控制，数字越小优先级越高
 * 4. @Around 中必须调用 point.proceed() 且返回结果，否则目标方法不执行或调用方拿不到返回值
 */
