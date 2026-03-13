package com.delivery.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.entity.*;
import com.delivery.enums.OrderStatus;
import com.delivery.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单自动分配定时任务
 * 每日凌晨2:00自动分配未分配的当日订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAssignerTask {

    private final OrderMapper orderMapper;
    private final DriverMapper driverMapper;
    private final ShopMapper shopMapper;
    private final TaskLogMapper taskLogMapper;

    /**
     * 每日凌晨2:00执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoAssignOrders() {
        log.info("开始执行订单自动分配任务...");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            LocalDate today = LocalDate.now();

            // 获取当日仍未分配的订单
            List<Order> unassignedOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getStatus, OrderStatus.PENDING_ASSIGNMENT.getCode())
                            .eq(Order::getDeliveryDate, today));

            if (unassignedOrders.isEmpty()) {
                log.info("没有需要自动分配的订单");
                saveTaskLog("ORDER_ASSIGNER", startTime, 1, "无待分配订单");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            // 按店铺分组
            Map<Long, List<Order>> ordersByShop = unassignedOrders.stream()
                    .collect(Collectors.groupingBy(Order::getShopId));

            for (Map.Entry<Long, List<Order>> entry : ordersByShop.entrySet()) {
                Long shopId = entry.getKey();
                List<Order> shopOrders = entry.getValue();

                try {
                    // 获取店铺所属的分管理
                    Shop shop = shopMapper.selectById(shopId);
                    if (shop == null) {
                        log.warn("店铺 {} 不存在，跳过", shopId);
                        failCount += shopOrders.size();
                        continue;
                    }

                    // 获取该分管理下的所有司机
                    List<Driver> drivers = driverMapper.selectList(
                            new LambdaQueryWrapper<Driver>()
                                    .eq(Driver::getBranchManagerId, shop.getBranchManagerId())
                                    .eq(Driver::getStatus, 1));

                    if (drivers.isEmpty()) {
                        log.warn("分管理 {} 下没有可用司机，跳过店铺 {}", shop.getBranchManagerId(), shopId);
                        failCount += shopOrders.size();
                        continue;
                    }

                    // 查找该店铺前一天送货的司机
                    Driver assignedDriver = findYesterdayDriver(shopId, drivers);

                    // 如果没有历史司机，随机选择一个
                    if (assignedDriver == null) {
                        assignedDriver = drivers.get(0);
                    }

                    // 分配订单
                    for (Order order : shopOrders) {
                        order.setDriverId(assignedDriver.getId());
                        order.setStatus(OrderStatus.PENDING_PICKUP.getCode());
                        orderMapper.updateById(order);
                        successCount++;
                    }

                    log.info("店铺 {} 的 {} 个订单已分配给司机 {}", shopId, shopOrders.size(), assignedDriver.getName());

                } catch (Exception e) {
                    failCount += shopOrders.size();
                    log.error("店铺 {} 订单分配失败: {}", shopId, e.getMessage(), e);
                }
            }

            // 记录任务日志
            saveTaskLog("ORDER_ASSIGNER", startTime, 1,
                    String.format("成功: %d, 失败: %d", successCount, failCount));

            log.info("订单自动分配任务完成，成功: {}, 失败: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("订单自动分配任务执行失败", e);
            saveTaskLog("ORDER_ASSIGNER", startTime, 0, e.getMessage());
        }
    }

    /**
     * 查找该店铺前一天送货的司机
     */
    private Driver findYesterdayDriver(Long shopId, List<Driver> availableDrivers) {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 查找前一天该店铺的已完成订单
        List<Order> yesterdayOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getShopId, shopId)
                        .eq(Order::getStatus, OrderStatus.COMPLETED.getCode())
                        .ge(Order::getReceivedAt, yesterday.atStartOfDay())
                        .lt(Order::getReceivedAt, LocalDate.now().atStartOfDay())
                        .isNotNull(Order::getDriverId)
                        .orderByDesc(Order::getReceivedAt)
                        .last("LIMIT 1"));

        if (!yesterdayOrders.isEmpty()) {
            Long driverId = yesterdayOrders.get(0).getDriverId();
            // 检查该司机是否仍在可用司机列表中
            return availableDrivers.stream()
                    .filter(d -> d.getId().equals(driverId))
                    .findFirst()
                    .orElse(null);
        }

        return null;
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
