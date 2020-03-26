package com.motor.frequency.redis;

import redis.clients.jedis.Jedis;

public class MotorJedis extends Jedis {
    public MotorJedis(String host, int port) {
        super(host, port);
    }

    public MotorPipeline pipelined() {
        MotorPipeline p = new MotorPipeline();
        p.setClient(client);
        return p;
    }
}