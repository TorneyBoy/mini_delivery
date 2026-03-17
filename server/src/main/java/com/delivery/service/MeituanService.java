package com.delivery.service;

import com.delivery.dto.response.MeituanProductResponse;

import java.util.List;

/**
 * 美团开放平台服务接口
 */
public interface MeituanService {

    /**
     * 通过条形码查询商品信息
     * 
     * @param upcCode 条形码
     * @return 商品信息列表
     */
    List<MeituanProductResponse> searchByUpcCode(String upcCode);

    /**
     * 通过商品名称搜索商品
     * 
     * @param productName 商品名称
     * @return 商品信息列表
     */
    List<MeituanProductResponse> searchByName(String productName);
}
