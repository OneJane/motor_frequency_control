package com.motor.frequency.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;

public class MotorJedisPool extends JedisPool {

    public MotorJedisPool(GenericObjectPoolConfig poolConfig, String host, int port, int timeout) {
        super(poolConfig, host, port, timeout);
    }

    public MotorJedis getResource() {
        MotorJedis jedis = (MotorJedis) super.getResource();
        jedis.setDataSource(this);
        return jedis;
    }
}
