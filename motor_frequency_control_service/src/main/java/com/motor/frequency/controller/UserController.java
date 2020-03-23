package com.motor.frequency.controller;

import com.motor.frequency.constants.BloomFilterRedisKeys;
import com.motor.frequency.service.IBloomFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @program: motor_frequency_control
 * @description: 测试控制器
 * @author: OneJane
 * @create: 2020-03-16 13:56
 **/
@RestController
@RequestMapping("user")
@Slf4j
public class UserController {

    @Autowired
    IBloomFilterService bloomFilterService;



    @GetMapping(value = "/setIdList")
    public void addIdList() {
        List<String> list = new ArrayList();
        list.add("asdasd");
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.forEach(System.out::println);
        bloomFilterService.insertByValueList("feed", "home:fetch", list,true);
        list.add("werewer");
        Map map = bloomFilterService.selectByValues( "feed", "home:fetch", list);
        System.out.println(map);

    }

    @GetMapping(value = "/setIdQueue")
    public void addIdQueue() {
        List<String> list = new ArrayList();
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.add(UUID.randomUUID().toString().replaceAll("-", ""));
        list.forEach(System.out::println);
        bloomFilterService.insertByValueList("feed", "home:read", list,true);
        list.add("werewer");
        Map map = bloomFilterService.selectByValues( "feed", "home:fetch", list);
        System.out.println(map);
    }

    @GetMapping(value = "/getIdList")
    public boolean getIdList() {
        List<String> list = new ArrayList();
        String listKey = String.format(BloomFilterRedisKeys.BLOOM_FILTER_LIST_KEY, "feed", "home:fetch");
        list.add("45gh");
        list.add("12");
        list.add("asdasd");
        list.add("7f6059507766457aa294b701570d1282");
        list.add("26245d7c22e84649b56d192f2725f407");
        list.add("fea2a0791ae341a8b09af4f099502e19");
        Map map = bloomFilterService.selectByValues( "feed", "home:fetch", list);
        System.out.println(map);
        return false;
    }



}
