package org.nms.crescodbtest;

//import org.jhades.JHades;

import io.cresco.library.messaging.MsgEvent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static MsgEvent buildAddNodeMsg(String region,String agent,String plugin){
        Map<String,String> params = new HashMap<>();
        params.put("region_name",region);
        params.put("agent_name",agent);
        params.put("plugin_id",plugin);
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

    public static Map<String,Object> getMockPluginConfig() {
        Map<String,Object> testConfig = new HashMap<>();
        testConfig.put("plugin_conf_1","confval_1");
        testConfig.put("plugin_conf_2","confval_2");
        return testConfig;
    }
    public static void main(String[] argv) {
        Map<String,Object> testConfig = getMockPluginConfig();
        testConfig.putAll(getGDBConfigMap(null,null,null,null));
        PluginBuilder mypb = new PluginBuilder(null,"testregion",null
                ,"ScrubClass",testConfig);
        ControllerEngine parent_ce = new ControllerEngine(mypb);
                //ControllerEngine.getGDB
                //getStringFromError
                //getAppScheduleQueue
                //getKPIProducer
                //setDBManagerActive

        //getPluginBuilder().getGlobalPluginMsgEvent
        //getPluginBuilder().sendRPC
        //getPluginBuilder().getLogger
        //getPluginBuilder().get ??
        parent_ce.startGDB();

        MsgEvent m = buildAddNodeMsg("some_region","global_ctrl_agent","some_plugin");
        parent_ce.getGDB().addNode(m);
        //System.out.println(parent_db.getAgentList("some_region"));
        Thread otherAgentProcess = new Thread(new AgentProcess(10000,parent_ce,"some_region","agent_smith"));
        otherAgentProcess.start();
        while(true){
            try {
                Thread.sleep(2000);
                System.out.println(parent_ce.getGDB().getAgentList("some_region"));
            }
            catch(InterruptedException ex) {
                System.err.println("Caught InterruptedException");
            }
        }
    }
}
class AgentProcess implements Runnable {
    private ControllerEngine global_ce;
    private String region;
    private String agent;
    private final String plugin_id ="some_plugin";
    private PluginBuilder plugin;
    private long lifetimems;

    public AgentProcess(long lifetimems, ControllerEngine global_ce, String region, String agent) {
        this.global_ce = global_ce;
        this.region = region;
        this.agent = agent;
        Map<String,Object> pluginConf = Main.getMockPluginConfig();
        String gdb_host = global_ce.getPluginBuilder().getConfig().getStringParam("gdb_host");
        String gdb_username = global_ce.getPluginBuilder().getConfig().getStringParam("gdb_username");
        String gdb_password = global_ce.getPluginBuilder().getConfig().getStringParam("gdb_password");
        String gdb_dbname = global_ce.getPluginBuilder().getConfig().getStringParam("gdb_dbname");
        if(gdb_host != null && gdb_username != null && gdb_password != null && gdb_dbname != null) {
            Map<String, Object> gdbConf = Main.getGDBConfigMap(gdb_host, gdb_username, gdb_password, gdb_dbname);
            pluginConf.putAll(gdbConf);
        }
        this.plugin = new PluginBuilder(agent,region,plugin_id,"Some_Base_Class"
                ,pluginConf);
        this.lifetimems = lifetimems;
    }

    public void reportToConsole(String msg){
        System.out.println("TS "+System.currentTimeMillis()+"[Region: "+region+" Agent:"+agent+" Plugin:"+plugin_id+"] - "+msg);
    }

    public void doSomeStuff() throws InterruptedException {
        Thread.sleep(2000);
        reportToConsole(global_ce.getGDB().getAgentList(region));
    }

    public void run() {
        try {
            MsgEvent m = Main.buildAddNodeMsg(region, agent,plugin_id);
            global_ce.getGDB().addNode(m);
            //m = Main.buildAddNodeMsg(region,agent,"some_plugin");
            //global_ce.getGDB().addNode(m);
            long starting_ts = System.currentTimeMillis();
            if(lifetimems > 0){
                while( (System.currentTimeMillis() - starting_ts) < lifetimems)
                doSomeStuff();
            } else {
                while(true){
                    doSomeStuff();
                }
            }
        }
        catch (Exception ex){
            System.out.println("Caught exception "+ex);
        }
        finally {
            global_ce.getGDB().removeNode(region,agent,null);
        }
    }
}