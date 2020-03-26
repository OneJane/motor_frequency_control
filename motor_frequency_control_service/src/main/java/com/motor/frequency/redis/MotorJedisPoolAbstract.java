package com.motor.frequency.redis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.util.Pool;

public class MotorJedisPoolAbstract extends Pool<MotorJedis> {


    public MotorJedisPoolAbstract() {
        super();
    }

    public MotorJedisPoolAbstract(GenericObjectPoolConfig poolConfig, PooledObjectFactory<MotorJedis> factory) {
        super(poolConfig, factory);
    }

    @Override
    protected void returnBrokenResource(MotorJedis resource) {
        super.returnBrokenResource(resource);
    }

    @Override
    protected void returnResource(MotorJedis resource) {
        super.returnResource(resource);
    }


}