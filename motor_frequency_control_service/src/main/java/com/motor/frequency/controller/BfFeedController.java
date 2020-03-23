package com.motor.frequency.controller;

import com.motor.frequency.constants.BloomFilterRedisKeys;
import com.motor.frequency.entity.CodeMsg;
import com.motor.frequency.exception.GlobalException;
import com.motor.frequency.service.IBloomFilterService;
import com.motor.frequency.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @program: motor_frequency_control
 * @description: 测试控制器
 * @author: OneJane
 * @create: 2020-03-16 13:56
 **/
@RestController
@RequestMapping("bf/feed")
@Slf4j
public class BfFeedController {

    @Autowired
    IBloomFilterService bloomFilterService;

    @Autowired
    RedisUtil redisUtil;



    @PostMapping(value = "/homeReadList")
    public void setHomeReadList(List<String> ids) {
        if(CollectionUtils.isEmpty(ids)){
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        bloomFilterService.insertByValueList("feed", "home:read", ids, false);
    }

    @PostMapping(value = "/homeFetchedList")
    public void setHomeFetchedList(List<String> ids) {
        if(CollectionUtils.isEmpty(ids)){
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        bloomFilterService.insertByValueList("feed", "home:fetch", ids, true);
    }

    @GetMapping(value = "/homeFetchedList")
    public Map<String, Boolean> getHomeFetchedList(List<String> ids) {
        if(CollectionUtils.isEmpty(ids)){
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, "feed", "home:fetch");
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        return redisUtil.scriptBfContains(keyList, ids);
    }

    @GetMapping(value = "/homeReadList")
    public Map<String, Boolean> getHomeReadList(List<String> ids) {
        if(CollectionUtils.isEmpty(ids)){
            throw new GlobalException(CodeMsg.BIND_ERROR.fillArgs(ids));
        }
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, "feed", "home:read");
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        return redisUtil.scriptBfContains(keyList, ids);
    }



}
