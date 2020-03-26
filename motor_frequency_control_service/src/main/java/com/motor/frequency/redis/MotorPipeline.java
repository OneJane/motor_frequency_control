package com.motor.frequency.redis;

import com.motor.frequency.util.BFCommand;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.List;

public class MotorPipeline extends Pipeline {

    public Response<List<Long>> bfMadd(String key, String... fields) {
        this.getClient(key).sendCommand(BFCommand.MADD, fields);
        return this.getResponse(BuilderFactory.LONG_LIST);
    }

    public Response<List<Long>> bfMexists(String key, String... fields) {
        this.getClient(key).sendCommand(BFCommand.MEXISTS, fields);
        return this.getResponse(BuilderFactory.LONG_LIST);
    }
}