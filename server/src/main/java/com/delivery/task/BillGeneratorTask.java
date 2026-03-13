package com.delivery.task;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.entity.*;
import com.delivery.enums.OrderStatus;
import com.delivery.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账单生成定时任务
 * 每日22:00自动为旗下每个店铺生成当日账单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillGeneratorTask {

    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final BillMapper billMapper;
    private final BillOrderMapper billOrderMapper;
    private final TaskLogMapper taskLogMapper;

    /**
     * 每日22:00执行
     */
    @Scheduled(cron = "0 0 22 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void generateBills() {
        log.info("开始执行账单生成任务...");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            LocalDate today = LocalDate.now();
            int successCount = 0;
            int failCount = 0;

            // 获取所有店铺
            List<Shop> shops = shopMapper.selectList(
                    new LambdaQueryWrapper<Shop>().eq(Shop::getStatus, 1));

            for (Shop shop : shops) {
                try {
                    // 检查是否已生成当日账单
                    Long existCount = billMapper.selectCount(
                            new LambdaQueryWrapper<Bill>()
                                    .eq(Bill::getShopId, shop.getId())
                                    .eq(Bill::getBillDate, today));

                    if (existCount > 0) {
                        log.info("店铺 {} 今日账单已存在，跳过", shop.getId());
                        continue;
                    }

                    // 获取该店铺当日已收货的订单
                    List<Order> orders = orderMapper.selectList(
                            new LambdaQueryWrapper<Order>()
                                    .eq(Order::getShopId, shop.getId())
                                    .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                                    .ge(Order::getReceivedAt, today.atStartOfDay())
                                    .lt(Order::getReceivedAt, today.plusDays(1).atStartOfDay()));

                    if (orders.isEmpty()) {
                        log.info("店铺 {} 今日无已收货订单，跳过", shop.getId());
                        continue;
                    }

                    // 计算总金额
                    BigDecimal totalAmount = orders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 创建账单
                    Bill bill = new Bill();
                    bill.setBillNo("BIL" + IdUtil.getSnowflakeNextIdStr());
                    bill.setShopId(shop.getId());
                    bill.setBillDate(today);
                    bill.setTotalAmount(totalAmount);
                    bill.setStatus(0); // 待支付

                    billMapper.insert(bill);

                    // 关联订单
                    for (Order order : orders) {
                        BillOrder billOrder = new BillOrder();
                        billOrder.setBillId(bill.getId());
                        billOrder.setOrderId(order.getId());
                        billOrderMapper.insert(billOrder);
                    }

                    successCount++;
                    log.info("店铺 {} 账单生成成功，金额: {}", shop.getId(), totalAmount);

                } catch (Exception e) {
                    failCount++;
                    log.error("店铺 {} 账单生成失败: {}", shop.getId(), e.getMessage(), e);
                }
            }

            // 记录任务日志
            saveTaskLog("BILL_GENERATOR", startTime, 1,
                    String.format("成功: %d, 失败: %d", successCount, failCount));

            log.info("账单生成任务完成，成功: {}, 失败: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("账单生成任务执行失败", e);
            saveTaskLog("BILL_GENERATOR", startTime, 0, e.getMessage());
        }
    }

    private void saveTaskLog(String taskType, LocalDateTime executedAt, Integer status, String detail) {
        TaskLog taskLog = new TaskLog();
        taskLog.setTaskType(taskType);
        taskLog.setExecutedAt(executedAt);
        taskLog.setStatus(status);
        taskLog.setDetail(detail);
        taskLogMapper.insert(taskLog);
    }
}
