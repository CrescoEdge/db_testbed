package org.nms.crescodbtest;

import com.google.gson.Gson;
import io.cresco.library.messaging.MsgEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DBTestHelpers {
    static Gson gson = new Gson();

    public static MsgEvent buildAddNodeMsg(String region, String agent, String plugin){
        Map<String,String> params = new HashMap<>();
        params.put("region_name",region);
        params.put("agent_name",agent);
        if(plugin != null){
            params.put("plugin_id",plugin);
        }
        //The event type might not be right but it should be ok w.r.t. AddNode in DBInterface
        return new MsgEvent(MsgEvent.Type.CONFIG,region,agent,plugin,params);
    }

    public static Map<String,Object> getGDBConfigMap(String gdb_host,String gdb_username,String gdb_password
            ,String gdb_dbname){
        Map<String,Object> configMap = new HashMap<>();
        configMap.put("gdb_host", gdb_host != null ? gdb_host : "localhost");
        configMap.put("gdb_username",gdb_username != null ? gdb_username : "root");
        configMap.put("gdb_password",gdb_password != null ? gdb_password : "root");
        configMap.put("gdb_dbname",gdb_dbname != null ? gdb_dbname : "crescodb");
        return configMap;
    }

    public static Map<String,Object> getMockPluginConfig(Optional<Map<String,Object>> GDBConfigMap) {
        Map<String,Object> testConfig = new HashMap<>();
        testConfig.put("plugin_conf_1","confval_1");
        testConfig.put("plugin_conf_2","confval_2");
        if(GDBConfigMap.isPresent()){
            testConfig.putAll(GDBConfigMap.get());
        }
        return testConfig;
    }

    public static ControllerEngine getControllerEngine(String agent,String region,String baseClassName
            ,Map<String,Object> configMap, Optional<ControllerEngine>parent_ce) {
        PluginBuilder mypb = new PluginBuilder(agent,region,baseClassName,configMap);
        PluginAdmin mypluginAdmin = new PluginAdmin();
        //Not having any plugins causes problems in the form of a NullPointerException, so add a bogus plugin
        mypluginAdmin.addPlugin("some_plugin","some_plugin.jar",configMap);
        ControllerEngine new_ce  = new ControllerEngine(mypb,mypluginAdmin);
        if(parent_ce.isPresent()){
            new_ce.setGDB(parent_ce.get().getGDB());
        } else {
            new_ce.startGDB();
        }
        for(String pluginid: new_ce.getPluginAdmin().getPluginMap().keySet()) {
            new_ce.getGDB().addNode(buildAddNodeMsg(mypb.getRegion(), mypb.getAgent(), pluginid));
        }
        return new_ce;
    }

}
