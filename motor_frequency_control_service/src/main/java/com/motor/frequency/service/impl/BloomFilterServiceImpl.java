package com.motor.frequency.service.impl;

import com.motor.frequency.config.BloomFilterConfig;
import com.motor.frequency.constants.BloomFilterRedisKeys;
import com.motor.frequency.service.IBloomFilterService;
import com.motor.frequency.util.RedisLock;
import com.motor.frequency.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: motor_frequency_control
 * @description: 布隆过滤器服务
 * @author: OneJane
 * @create: 2020-03-17 14:18
 **/
@Service
@Slf4j
public class BloomFilterServiceImpl implements IBloomFilterService {

    @Resource
    private RedisUtil redisUtil;

    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    @Autowired
    RedisTemplate redisTemplate;


    @Override
    public boolean selectByValue(String listKey, String id) {
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        Map map = redisUtil.scriptBfContains(keyList, new ArrayList<String>() {{
            add(id);
        }});
        if (map.values().contains(true)) {
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Boolean> selectByValues(String serviceName, String moduleName, List<String> ids) {
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, serviceName, moduleName);
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);
        // 获取所有key
        return redisUtil.scriptBfContains(keyList, ids);
    }

    @Override
    public void insertByValueList(String serviceName, String moduleName, List<String> ids, boolean delFlag) {
        String lockKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_LOCK_KEY, serviceName, moduleName);
        // 分片集合
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, serviceName, moduleName);
        // 计数器 最后一个分片的数量
        String countKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_COUNT_KEY, serviceName, moduleName);
        String keyPrefix = String.format(BloomFilterRedisKeys.BLOOM_FILTER_PREFIX_KEY, serviceName, moduleName);
        RedisLock redisLock = new RedisLock(redisTemplate, lockKey, 500);


        // 从bf中获取不存在的id
        List<String> keyList = redisUtil.lrange(listKey, 0, -1);

        String key = "";

        if (CollectionUtils.isEmpty(keyList)) {
            try {
                if (redisLock.lock()) {
                    // 布隆过滤器中没有数据  ids数量必远小于分片中的数据量
                    key = String.format("%s:%s", keyPrefix, 1);
                    redisUtil.lrightPush(listKey, key); // 分片集合
                    // 新建分片
                    redisUtil.scriptBfCreate(key);
                    redisUtil.scriptBfAdd(key, ids); // 布隆过滤器
                    redisUtil.incr(countKey, ids.size()); // 计数器
                } else {
                    log.info("未获取到锁");
                }
            } catch (Exception e) {
                log.info(e.getMessage(), e);
            } finally {
                redisLock.unlock();
            }
        } else {
            Map<String, Boolean> map = redisUtil.scriptBfContains(keyList, ids);
// 有分片数据
            Map<String, Boolean> inexistenceMap = map.entrySet().stream().filter(v -> v.getValue().equals(false))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Integer size = inexistenceMap.keySet().size();
            if (size > 0) {
                // 多条数据不存在于布隆过滤器中
                // 获取最后一个分片名及数量

                Integer count = 0;
                if (redisUtil.getValue(countKey) != null) {
                    count = Integer.valueOf(redisUtil.getValue(countKey));
                }

                if (count + size > bloomFilterConfig.getSize()) {
                    try {
                        if (redisLock.lock()) {
                            // 嵌套count检查防止多线程读到脏count和新起分片
                            count = Integer.valueOf(redisUtil.getValue(countKey));
                            if (count + size > bloomFilterConfig.getSize()) {
                                // 获取分片数量
                                Long shardSize = redisUtil.lsize(listKey);

                                // 删除原有计数器
                                redisUtil.deleteKey(countKey);

                                // 新建分片
                                String lastKey = redisUtil.index(listKey, -1);
                                // 分片后缀
                                Integer keySuffix = Integer.valueOf(lastKey.substring(lastKey.lastIndexOf(":") + 1));
                                key = String.format("%s:%s", keyPrefix, keySuffix + 1);
                                redisUtil.scriptBfCreate(key);

                                redisUtil.lrightPush(listKey, key); // list
                                redisUtil.scriptBfAdd(key, new ArrayList<>(inexistenceMap.keySet())); // bf
                                redisUtil.incr(countKey, size);
                                if (delFlag && shardSize >= 20) {
                                    // 分片数据大于4千万则删除最前分片
                                    String leftKey = redisUtil.lleftPop(listKey);
                                    redisUtil.deleteKey(leftKey);
                                }
                            } else {
                                String lastKey = redisUtil.index(listKey, -1);
                                redisUtil.scriptBfAdd(lastKey, new ArrayList<>(inexistenceMap.keySet())); // bf
                                redisUtil.incr(countKey, size);
                            }
                        } else {
                            log.info("未获取到锁");

                        }
                    } catch (Exception e) {
                        log.info(e.getMessage(), e);
                    } finally {
                        redisLock.unlock();
                    }
                } else {
                    String lastKey = redisUtil.index(listKey, -1);
                    redisUtil.scriptBfAdd(lastKey, new ArrayList<>(inexistenceMap.keySet())); // bf
                    redisUtil.incr(countKey, size);
                }

            }
        }


    }


}
