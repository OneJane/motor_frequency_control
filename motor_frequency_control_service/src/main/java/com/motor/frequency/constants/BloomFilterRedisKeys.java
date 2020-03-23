package com.motor.frequency.constants;

public class BloomFilterRedisKeys {
    public final static String BLOOM_FILTER_LIST_LOCK_KEY= "bf:lock:%s:%s:list";
    public final static String BLOOM_FILTER_LIST_KEY= "bf:list:%s:%s";
    public final static String BLOOM_FILTER_LIST_COUNT_KEY= "bf:list:count:%s:%s";
    public final static String BLOOM_FILTER_PREFIX_KEY= "bf:%s:%s";

}
