package com.motor.frequency.rpc.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.motor.frequency.constants.BloomFilterRedisKeys;
import com.motor.frequency.entity.CodeMsg;
import com.motor.frequency.exception.GlobalException;
import com.motor.frequency.service.IBloomFilterService;
import com.motor.frequency.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RpcFrequencyServiceImpl implements RpcFrequencyService {

    @Autowired
    IBloomFilterService bloomFilterService;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public void setHomeFetchedList(String deviceId, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        List<String> newIds = ids.stream().map(e -> new StringBuilder().append(deviceId).append("_").append(e).toString()).collect(Collectors.toList());
        bloomFilterService.insertByValueList("feed", "home:fetch", newIds, true);
    }

    @Override
    public void setHomeReadList(String deviceId, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        List<String> newIds = ids.stream().map(e -> new StringBuilder().append(deviceId).append("_").append(e).toString()).collect(Collectors.toList());
        bloomFilterService.insertByValueList("feed", "home:read", newIds, false);
    }

    @Override
    public Map<String, Boolean> getHomeFetchedList(String deviceId, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        List<String> newIds = ids.stream().map(e -> new StringBuilder().append(deviceId).append("_").append(e).toString()).collect(Collectors.toList());
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, "feed", "home:fetch");
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        Map<String, Boolean> resultMap = redisUtil.scriptBfContains(keyList, newIds);
        if(!CollectionUtils.isEmpty(resultMap)){
            return resultMap.entrySet().stream().collect(Collectors.toMap(
                    k -> k.getKey().substring(k.getKey().lastIndexOf("_") + 1),
                    Map.Entry::getValue
            ));
        }
        return resultMap;
    }

    @Override
    public Map<String, Boolean> getHomeReadList(String deviceId, List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        List<String> newIds = ids.stream().map(e -> new StringBuilder().append(deviceId).append("_").append(e).toString()).collect(Collectors.toList());
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, "feed", "home:read");
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        Map<String, Boolean> resultMap =  redisUtil.scriptBfContains(keyList, newIds);
        if(!CollectionUtils.isEmpty(resultMap)){
            return resultMap.entrySet().stream().collect(Collectors.toMap(
                    k -> k.getKey().substring(k.getKey().lastIndexOf("_") + 1),
                    Map.Entry::getValue
            ));
        }
        return resultMap;
    }

}
