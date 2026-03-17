package com.delivery.service.impl;

import com.delivery.config.MeituanConfig;
import com.delivery.dto.response.MeituanProductResponse;
import com.delivery.service.MeituanService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.util.*;

/**
 * 美团开放平台服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeituanServiceImpl implements MeituanService {

    private final MeituanConfig meituanConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MeituanProductResponse> searchByUpcCode(String upcCode) {
        Map<String, String> params = new HashMap<>();
        params.put("upc_codes", upcCode);
        return callMeituanApi("retail/upc/list", params);
    }

    @Override
    public List<MeituanProductResponse> searchByName(String productName) {
        Map<String, String> params = new HashMap<>();
        params.put("upc_name", productName);
        return callMeituanApi("retail/upc/list", params);
    }

    /**
     * 调用美团API
     */
    private List<MeituanProductResponse> callMeituanApi(String apiPath, Map<String, String> businessParams) {
        try {
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("app_key", meituanConfig.getAppKey());
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

            // 添加业务参数
            if (businessParams != null) {
                params.putAll(businessParams);
            }

            // 计算签名
            String sign = generateSignature(params);
            params.put("sign", sign);

            // 构建请求URL
            String url = UriComponentsBuilder.fromHttpUrl(meituanConfig.getBaseUrl())
                    .pathSegment(apiPath.split("/"))
                    .queryParams(params.entrySet().stream()
                            .collect(org.springframework.util.LinkedMultiValueMap::new,
                                    (m, e) -> m.add(e.getKey(), e.getValue()),
                                    org.springframework.util.MultiValueMap::addAll))
                    .toUriString();

            log.info("调用美团API: {}", url);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseResponse(response.getBody());
            }

            log.error("美团API调用失败: {}", response.getStatusCode());
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("调用美团API异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成签名
     * 美团签名规则：将所有参数按key字母升序排序，拼接成key=value格式，最后加上app_secret，进行MD5加密
     */
    private String generateSignature(Map<String, String> params) {
        // 按key字母升序排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (!"sign".equals(key) && params.get(key) != null && !params.get(key).isEmpty()) {
                sb.append(key).append("=").append(params.get(key));
            }
        }
        sb.append(meituanConfig.getAppSecret());

        return md5(sb.toString());
    }

    /**
     * MD5加密
     */
    private String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 解析美团API响应
     */
    private List<MeituanProductResponse> parseResponse(String responseBody) {
        List<MeituanProductResponse> products = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 检查返回状态
            String code = root.path("code").asText();
            if (!"0".equals(code) && !"200".equals(code)) {
                log.error("美团API返回错误: code={}, msg={}", code, root.path("msg").asText());
                return products;
            }

            // 解析商品数据
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    MeituanProductResponse product = new MeituanProductResponse();
                    product.setUpcCode(item.path("upc_code").asText());
                    product.setName(item.path("upc_name").asText());
                    product.setBrand(item.path("brand_name").asText());
                    product.setSpec(item.path("spec").asText());
                    product.setCategory(item.path("category_name").asText());
                    product.setImageUrl(item.path("image_url").asText());
                    product.setUnit(item.path("unit").asText());
                    product.setDescription(item.path("description").asText());
                    products.add(product);
                }
            }
        } catch (Exception e) {
            log.error("解析美团API响应失败", e);
        }
        return products;
    }
}
