package com.motor.frequency.service;

import java.util.List;
import java.util.Map;

public interface IBloomFilterService {
    /**
     * 查询是否存在布隆过滤器中
     *
     * @param listKey key集合
     * @param id      数据id
     */
    boolean selectByValue(String listKey, String id);

    Map selectByValues(String serviceName, String moduleName, List<String> ids);

    void insertByValueList(String serviceName, String moduleName, List<String> ids, boolean delFlag);
}
