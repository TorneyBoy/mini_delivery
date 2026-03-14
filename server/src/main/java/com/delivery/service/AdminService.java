package com.delivery.service;

import com.delivery.common.PageResult;
import com.delivery.dto.request.BranchManagerCreateRequest;
import com.delivery.dto.response.BranchManagerResponse;

import java.util.Map;

/**
 * 总管理服务接口
 */
public interface AdminService {

    /**
     * 创建分管理
     */
    void createBranchManager(BranchManagerCreateRequest request);

    /**
     * 获取分管理列表
     */
    PageResult<BranchManagerResponse> getBranchManagerList(Integer page, Integer size);

    /**
     * 获取分管理详情
     */
    BranchManagerResponse getBranchManagerDetail(Long id);

    /**
     * 禁用/启用分管理
     */
    void toggleBranchManagerStatus(Long id);

    /**
     * 更新分管理
     */
    void updateBranchManager(Long id, BranchManagerCreateRequest request);

    /**
     * 删除分管理
     */
    void deleteBranchManager(Long id);

    /**
     * 获取统计数据
     */
    Map<String, Object> getStatistics();

    /**
     * 获取数据中心详细数据
     * 
     * @param startDate 开始日期 (格式: yyyy-MM-dd)
     * @param endDate   结束日期 (格式: yyyy-MM-dd)
     */
    Map<String, Object> getDataCenter(String startDate, String endDate);
}
