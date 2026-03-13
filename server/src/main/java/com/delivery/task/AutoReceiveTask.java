package com.delivery.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.delivery.entity.Order;
import com.delivery.entity.TaskLog;
import com.delivery.enums.OrderStatus;
import com.delivery.mapper.OrderMapper;
import com.delivery.mapper.TaskLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自动收货定时任务
 * 每日21:00自动将当日应达订单置为已收货
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReceiveTask {

    private final OrderMapper orderMapper;
    private final TaskLogMapper taskLogMapper;

    /**
     * 每日21:00执行
     */
    @Scheduled(cron = "0 0 21 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoReceive() {
        log.info("开始执行自动收货任务...");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            LocalDate today = LocalDate.now();

            // 获取当日应达但未确认收货的订单（待送货状态）
            // 根据需求，收货日期为当天的订单
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                    .eq(Order::getStatus, OrderStatus.PENDING_DELIVERY.getCode())
                    .eq(Order::getDeliveryDate, today);

            // 如果当天22:00前未点击，系统自动标记为已完成
            // 这里是21:00执行，所以会自动将待送货的当日订单标记为已完成
            // 注意：根据需求文档，送货完成也是22:00前未点击自动标记
            // 这里我们按照21:00自动收货来实现

            // 获取所有符合条件的订单
            java.util.List<Order> orders = orderMapper.selectList(wrapper);

            int count = 0;
            for (Order order : orders) {
                order.setStatus(OrderStatus.COMPLETED.getCode());
                order.setReceivedAt(LocalDateTime.now());
                orderMapper.updateById(order);
                count++;
            }

            // 记录任务日志
            saveTaskLog("AUTO_RECEIVE", startTime, 1,
                    String.format("自动收货订单数: %d", count));

            log.info("自动收货任务完成，处理订单数: {}", count);

        } catch (Exception e) {
            log.error("自动收货任务执行失败", e);
            saveTaskLog("AUTO_RECEIVE", startTime, 0, e.getMessage());
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
