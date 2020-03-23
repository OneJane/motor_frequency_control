package com.motor.frequency.rpc.service;

import java.util.List;
import java.util.Map;

public interface RpcFrequencyService {

    /**
     * 向bf中存入多条用户拉取过的数据
     * @param ids
     */
    void setHomeFetchedList(String deviceId, List<String> ids);

    /**
     * 想bf中存入多条用户读过的数据
     * @param ids
     */
    void setHomeReadList(String deviceId, List<String> ids);

    /**
     * 从bf中获取指定拉取过的数据是否存在
     * @param ids
     * @return
     */
    Map<String,Boolean> getHomeFetchedList(String deviceId, List<String> ids);

    /**
     * 从bf中获取指定读取过的数据是否存在
     * @param ids
     * @return
     */
    Map<String,Boolean> getHomeReadList(String deviceId, List<String> ids);


}
