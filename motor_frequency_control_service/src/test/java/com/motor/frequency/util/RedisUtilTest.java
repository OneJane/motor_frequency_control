package com.motor.frequency.util;

import com.alibaba.fastjson.JSON;
import com.motor.frequency.redis.MotorJedis;
import com.motor.frequency.redis.MotorJedisSentinelPool;
import com.motor.frequency.redis.MotorPipeline;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class RedisUtilTest {
    @Autowired
    MotorJedisSentinelPool sentinelPool;

    @Test
    public void testBloomFilter() {
        for (int ii = 0; ii < 10; ii++) {
            MotorJedis jedis = sentinelPool.getResource();
            MotorPipeline p = jedis.pipelined();
            List<String> l = new ArrayList<>();
            l.add("name");
            for (int i = 1; i < 5000; i++) {
                l.add("one11jane" + i);
            }
            String[] str = new String[l.size()];
            l.toArray(str);
            long startTime = System.currentTimeMillis();   //获取开始时间
            p.bfMexists("name", str);
            p.bfMexists("name", str);
            p.bfMexists("name", str);
            p.bfMexists("name", str);
            p.bfMexists("name", str);
            p.bfMexists("name", str);
//        p.sync();
            List<Object> l1 = p.syncAndReturnAll();
            System.out.println(JSON.toJSONString(l1));
            long endTime = System.currentTimeMillis(); //获取结束时间
            System.out.println("程序运行时间： " + (endTime - startTime) + "ms");
            jedis.close();
        }
    }
}
