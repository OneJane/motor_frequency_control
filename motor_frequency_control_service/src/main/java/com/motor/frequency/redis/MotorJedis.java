package com.motor.frequency.redis;


import redis.clients.jedis.Jedis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

public class MotorJedis extends Jedis {

    protected MotorJedisPoolAbstract dataSource = null;

    public MotorJedis(String host, int port) {
        super(host, port);
    }

    public MotorJedis(final String host, final int port, final int connectionTimeout, final int soTimeout,
                      final boolean ssl, final SSLSocketFactory sslSocketFactory, final SSLParameters sslParameters,
                      final HostnameVerifier hostnameVerifier) {
        super(host, port, connectionTimeout, soTimeout, ssl, sslSocketFactory, sslParameters,
                hostnameVerifier);
    }

    public MotorPipeline pipelined() {
        MotorPipeline p = new MotorPipeline();
        p.setClient(client);
        return p;
    }

    public void setDataSource(MotorJedisPoolAbstract jedisPool) {
        this.dataSource = jedisPool;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            MotorJedisPoolAbstract pool = this.dataSource;
            this.dataSource = null;
            if (client.isBroken()) {
                pool.returnBrokenResource(this);
            } else {
                pool.returnResource(this);
            }
        } else {
            super.close();
        }
    }
}