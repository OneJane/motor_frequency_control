package com.motor.frequency.util;

import org.junit.Assert;
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
    RedisUtil redisUtil;

    @Test
    public void testBloomFilter() {
    }

    @Test
    public void testBfAdd() {
        List<String> aaa = new ArrayList<>();
        aaa.add("111");
        aaa.add("222");
        redisUtil.scriptBfAdd("nnn", aaa);
    }

    @Test
    public void testGet() {
        redisUtil.setValue("aaaa", "1");
        String a = redisUtil.getValue("aaaa");
        System.out.println(a);
        Assert.assertEquals(a, "1");
    }

    @Test
    public void testBatch() {
        List<String> a = new ArrayList<>();
        a.add("nnn");
        List<String> aaa = new ArrayList<>();
        aaa.add("789");
        aaa.add("153");
        redisUtil.scriptBfAdd("nnn",aaa);
        aaa.add("asd");
        redisUtil.scriptBfContains(a,aaa);
    }
}
