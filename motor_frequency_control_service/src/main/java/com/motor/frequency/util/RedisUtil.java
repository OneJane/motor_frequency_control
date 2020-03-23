package com.motor.frequency.util;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import com.motor.frequency.config.BloomFilterConfig;
import com.motor.frequency.config.BloomFilterHelper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @program: motor_frequency_control
 * @description: redis工具类
 * @author: OneJane
 * @create: 2020-03-16 13:56
 **/
@Slf4j
@Component
public class RedisUtil {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    // 两个月过期
    private static final long TIME_OUT = 60;
    //bit数组长度
    private long numBits;
    //hash函数数量
    private int numHashFunctions;

    final Random random = new Random();

    /**
     * 默认过期时长，单位：秒
     */
    public static final long DEFAULT_EXPIRE = 60 * 60 * 24;

    private static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 99;

    public static final int LOCK_EXPIRE = 300; // ms

    /**
     * 不设置过期时长
     */
    public static final long NOT_EXPIRE = -1;

    public boolean existsKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 重名名key，如果newKey已经存在，则newKey的原值被覆盖
     *
     * @param oldKey
     * @param newKey
     */
    public void renameKey(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * newKey不存在时才重命名
     *
     * @param oldKey
     * @param newKey
     * @return 修改成功返回true
     */
    public boolean renameKeyNotExist(String oldKey, String newKey) {
        return redisTemplate.renameIfAbsent(oldKey, newKey);
    }

    /**
     * 删除key
     *
     * @param key
     */
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 删除多个key
     *
     * @param keys
     */
    public void deleteKey(String... keys) {
        Set<String> kSet = Stream.of(keys).map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 删除Key的集合
     *
     * @param keys
     */
    public void deleteKey(Collection<String> keys) {
        Set<String> kSet = keys.stream().map(k -> k).collect(Collectors.toSet());
        redisTemplate.delete(kSet);
    }

    /**
     * 设置key的生命周期
     *
     * @param key
     * @param time
     * @param timeUnit
     */
    public void expireKey(String key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

    /**
     * 指定key在指定的日期过期
     *
     * @param key
     * @param date
     */
    public void expireKeyAt(String key, Date date) {
        redisTemplate.expireAt(key, date);
    }

    /**
     * 查询key的生命周期
     *
     * @param key
     * @param timeUnit
     * @return
     */
    public long getKeyExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * 将key设置为永久有效
     *
     * @param key
     */
    public void persistKey(String key) {
        redisTemplate.persist(key);
    }

    public boolean isExistBloom(String key) {
        long[] indexs = getIndexs(key);
        List list = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Nullable
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                redisConnection.openPipeline();
                for (long index : indexs) {
                    redisConnection.getBit(key.getBytes(), index);
                }
                redisConnection.close();
                return null;
            }
        });
        return !list.contains(false);
    }


    private long[] getIndexs(String key) {
        long hash1 = hash(key);
        long hash2 = hash1 >>> 16;
        long[] result = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = hash1 + i * hash2;
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            result[i] = combinedHash % numBits;
        }
        return result;
    }

    /**
     * 获取一个hash值
     */
    private long hash(String key) {
        Charset charset = Charset.forName("UTF-8");
        return Hashing.murmur3_128().hashObject(key, Funnels.stringFunnel(charset)).asLong();
    }

    /**
     * scan 实现
     *
     * @param pattern  表达式
     * @param consumer 对迭代到的key进行操作
     */
    public void scan(String pattern, Consumer<byte[]> consumer) {
        redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().count(Long.MAX_VALUE).match(pattern).build())) {
                cursor.forEachRemaining(consumer);
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 获取符合条件的key
     *
     * @param pattern 表达式
     * @return
     */
    public List<String> keys(String pattern) {
        List<String> keys = new ArrayList<>();
        this.scan(pattern, item -> {
            //符合条件的key
            String key = new String(item, StandardCharsets.UTF_8);
            keys.add(key);
        });
        return keys;
    }

    /**
     * 存储数据或修改数据
     *
     * @param modelMap
     * @param mapName
     */
    public void setMapKey(String mapName, Map<String, Object> modelMap) {
        HashOperations<String, String, Object> hps = redisTemplate.opsForHash();
        hps.putAll(mapName, modelMap);
    }


    /**
     * 获取数据Map
     *
     * @param mapName
     * @return
     */
    public Map<String, Object> getMapValue(String mapName) {
        HashOperations<String, String, Object> hps = redisTemplate.opsForHash();
        return hps.entries(mapName);
    }

    /**
     * 增加数据
     *
     * @param key
     * @param num
     * @return
     */
    public Long incr(String key, Integer num) {
        return redisTemplate.opsForValue().increment(key, num);
    }

    /**
     * 获取数据
     *
     * @param key
     * @return
     */
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setValue(String key, String v) {
        redisTemplate.opsForValue().set(key, v);
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key
     * @return
     */
    public Set<String> rangeSet(String key) {
        return redisTemplate.opsForSet().members(key);
    }


    /**
     * set移除元素
     *
     * @param key
     * @param values
     * @return
     */
    public Long removeSet(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }


    public void batch(List<String> keyList) {
        Map<String, String> map = new HashMap();

        List<Object> list = redisTemplate.executePipelined(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection redisConnection) throws DataAccessException {
                StringRedisConnection connection = (StringRedisConnection) redisConnection;
                for (String key : keyList) {
                    map.put(key, connection.get(key));
                }
                return null;
            }
        });
        map.forEach((k, v) -> {
            System.out.println("k=" + k + ",v=" + v);
        });

    }

    /**
     * @param key
     * @param value
     * @return
     */
    public Long addSet(String key, String value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * HashEntries
     *
     * @param key 键 不能为null
     * @return 值
     */
    public Map<Object, Object> hEntries(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 右插一条数据
     *
     * @param key
     * @param value
     * @return
     */
    public Long lrightPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 通过索引获取列表中的元素
     *
     * @param key
     * @param index
     * @return
     */
    public String index(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 获取key的list数量
     *
     * @param key
     * @return
     */
    public Long lsize(String key) {
        return redisTemplate.opsForList().size(key);
    }


    /**
     * 获取列表指定范围内的元素
     *
     * @param key
     * @param start 开始位置, 0是开始位置
     * @param end   结束位置, -1返回所有
     * @return
     */
    public List<String> lrange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 删除一个或多个哈希表字段
     *
     * @param key
     * @param fields
     * @return
     */
    public Long hdelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 移出并获取列表的第一个元素
     *
     * @param key
     * @return 删除的元素
     */
    public String lleftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }


    /**
     * 根据给定的布隆过滤器添加值
     */
    public <T> void addByBloomFilter(BloomFilterHelper<T> bloomFilterHelper, String key, T value) {
        Preconditions.checkArgument(bloomFilterHelper != null, "bloomFilterHelper不能为空");
        long[] offset = bloomFilterHelper.murmurHashOffset(value);
        for (long i : offset) {
            redisTemplate.opsForValue().setBit(key, i, true);
        }
    }

    /**
     * 根据给定的布隆过滤器判断值是否存在
     */
    public <T> boolean includeByBloomFilter(BloomFilterHelper<T> bloomFilterHelper, String key, T value) {
        Preconditions.checkArgument(bloomFilterHelper != null, "bloomFilterHelper不能为空");
        long[] offset = bloomFilterHelper.murmurHashOffset(value);
        for (long i : offset) {
            if (!redisTemplate.opsForValue().getBit(key, i)) {
                return false;
            }
        }
        return true;
    }


    public void scriptBfAdd(String key, List<String> valueList) {
        redisTemplate.executePipelined(new RedisCallback<Long>() {
            @Nullable
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                connection.scriptingCommands().eval(joinBfCommand("bf.madd", key, valueList).getBytes(Charset.forName("UTF-8")), ReturnType.MULTI, 0);
                return null;
            }
        });
    }

    public void scriptBfCreate(String key) {
        //获取redis连接
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection conn = factory.getConnection();
        List<String> valueList = new ArrayList<>();
        valueList.add(String.valueOf(bloomFilterConfig.getFpp()));
        valueList.add(String.valueOf(bloomFilterConfig.getSize()));
        conn.scriptingCommands().eval(joinBfCommand("bf.reserve", key, valueList).getBytes(Charset.forName("UTF-8")), ReturnType.STATUS, 0);
        redisTemplate.expire(key, TIME_OUT, TimeUnit.DAYS);
    }


    public Map<String, Boolean> scriptBfContains(List<String> keyList, List<String> valueList) {
        List<Object> result = new ArrayList<>();
        redisTemplate.executePipelined(new RedisCallback<Long>() {
            @Nullable
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                if (!CollectionUtils.isEmpty(keyList)) {
                    for (String key : keyList) {
                        connection.scriptingCommands().eval(joinBfCommand("bf.mexists", key, valueList).getBytes(Charset.forName("UTF-8")), ReturnType.MULTI, 0);
                    }
                }
                result.addAll(connection.closePipeline());
                return null;
            }
        }, redisTemplate.getValueSerializer());
        Map<String, Boolean> hashMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(result)) {
            hashMap = valueList.stream().collect(Collectors.toMap(key -> key, key -> (((ArrayList) result.get(0)).get(valueList.indexOf(key)).toString()).equals("1") ? true : false));
        }

        return hashMap;
    }


    public static String joinBfCommand(String command, String key, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("return redis.call('");
        sb.append(command);
        sb.append("','");
        sb.append(key);
        sb.append("'");
        for (String v : values) {
            String s = ",'" + v + "'";
            sb.append(s);
        }
        sb.append(")");
        return sb.toString();
    }

    public static void main(String[] args) {
        List<String> valueList = new ArrayList<>();
        valueList.add(String.valueOf(0.01));
        valueList.add(String.valueOf(4000));
        System.out.println(joinBfCommand("bf.reserve", "a", valueList));
    }

    @Autowired
    RedissonClient redissonClient;

    public void redissonBfAdd(String key, List<String> valueList) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(key);
        bloomFilter.tryInit(bloomFilterConfig.getSize(), bloomFilterConfig.getFpp());
        for (String value : valueList) {
            bloomFilter.add(value);
        }

    }


    public boolean redissonBfContains(String key, String value) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(key);
        bloomFilter.tryInit(bloomFilterConfig.getSize(), bloomFilterConfig.getFpp());
        return bloomFilter.contains(value);
    }

}
