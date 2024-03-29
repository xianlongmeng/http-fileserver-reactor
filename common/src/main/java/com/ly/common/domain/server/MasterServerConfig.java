package com.ly.common.domain.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.annotation.JSONField;

public class MasterServerConfig {

    private Map<Long, ServerInfoConfiguration> masterServerMap = new ConcurrentHashMap<>();
    // 变化更新，master负责发送变化的情况
    private Map<Long, ServerInfoConfiguration> storeServerMap = new ConcurrentHashMap<>();

    private long updateLastTime;

    public int getStoreServerInitCount() {
        return storeServerMap.size();
    }

    public Map<Long, ServerInfoConfiguration> getMasterServerMap() {
        return masterServerMap;
    }

    public void setMasterServerMap(Map<Long, ServerInfoConfiguration> masterServerMap) {
        this.masterServerMap = masterServerMap;
    }

    public Map<Long, ServerInfoConfiguration> getStoreServerMap() {
        return storeServerMap;
    }

    public void setStoreServerMap(Map<Long, ServerInfoConfiguration> storeServerMap) {
        this.storeServerMap = storeServerMap;
    }

    public void putServerInfoConfiguration(ServerInfoConfiguration serverInfoConfiguration) {
        if (serverInfoConfiguration == null)
            return;
        if (serverInfoConfiguration.isMaster())
            masterServerMap.put(serverInfoConfiguration.getServerId(), serverInfoConfiguration);
        else
            storeServerMap.put(serverInfoConfiguration.getServerId(), serverInfoConfiguration);
    }

    public ServerInfoConfiguration removeServerInfoConfiguration(long serverId) {
        if (masterServerMap.containsKey(serverId))
            return masterServerMap.remove(serverId);
        return storeServerMap.remove(serverId);
    }

    public ServerInfoConfiguration removeMasterServer(long serverId) {
        return masterServerMap.remove(serverId);
    }

    public ServerInfoConfiguration removeStoreServer(long serverId) {
        return storeServerMap.remove(serverId);
    }

    public ServerInfoConfiguration getServerInfoConfiguration(long serverId) {
        if (masterServerMap.containsKey(serverId))
            return masterServerMap.get(serverId);
        else
            return storeServerMap.get(serverId);
    }

    public ServerInfoConfiguration getMasterServerInfoConfiguration(int serverId) {
        return masterServerMap.get(serverId);
    }

    public ServerInfoConfiguration getStoreServerInfoConfiguration(int serverId) {
        return storeServerMap.get(serverId);
    }

    public long getUpdateLastTime() {
        return updateLastTime;
    }

    public void setUpdateLastTime(long updateLastTime) {
        this.updateLastTime = updateLastTime;
    }
}
