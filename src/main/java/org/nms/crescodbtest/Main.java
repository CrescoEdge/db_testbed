package org.nms.crescodbtest;

//import org.jhades.JHades;

import io.cresco.library.messaging.MsgEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;

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
        //Found this at https://stackoverflow.com/a/42860529
        Arrays.stream(LogManager.getLogManager().getLogger("").getHandlers()).forEach(h -> h.setLevel(Level.FINER));
        LogManager.getLogManager().getLogger("").setLevel(Level.FINER);

        Map<String,Object> testConfig = getMockPluginConfig();
        testConfig.putAll(getGDBConfigMap(null,null,null,null));
        testConfig.put("db_retry_count","50");
        PluginBuilder mypb = new PluginBuilder(null,"testregion",null
                ,"TestBase",testConfig);
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
        Thread global_controller = new Thread(new AgentProcess(0,parent_ce,"some_region","global_controller"));
        global_controller.start();
        //MsgEvent m = buildAddNodeMsg("some_region","global_ctrl_agent","some_plugin");
        //parent_ce.getGDB().addNode(m);
        //System.out.println(parent_db.getAgentList("some_region"));
        //Thread otherAgentProcess = new Thread(new AgentProcess(10000,parent_ce,"some_region","agent_smith"));
        //otherAgentProcess.start();
        try{
            int numberOfAgents = argv != null ? Integer.parseInt(argv[0]) : 1;
            long agentLifetime = 0;
            for(int i = 0;i < numberOfAgents;i++) {
                Thread otherAgentProcess = new Thread(new AgentProcess(agentLifetime,parent_ce,"some_region","agent-"+i));
                otherAgentProcess.start();
                //Thread.sleep(500);
            }
            while(true){
                    Thread.sleep(2000);
                    //System.out.println(parent_ce.getGDB().getAgentList("some_region"));
            }
        }
        catch(InterruptedException ex) {
            System.err.println("Caught InterruptedException");
        }
    }
}
class AgentProcess implements Runnable {
    private ControllerEngine ce;
    private String region;
    private String agent;
    private final String plugin_id ="some_plugin";
    private PluginBuilder plugin;
    private long lifetimems;

    public AgentProcess(long lifetimems, ControllerEngine ce, String region, String agent) {
        this.ce = ce;
        this.region = region;
        this.agent = agent;
        Map<String,Object> pluginConf = Main.getMockPluginConfig();
        String gdb_host = ce.getPluginBuilder().getConfig().getStringParam("gdb_host");
        String gdb_username = ce.getPluginBuilder().getConfig().getStringParam("gdb_username");
        String gdb_password = ce.getPluginBuilder().getConfig().getStringParam("gdb_password");
        String gdb_dbname = ce.getPluginBuilder().getConfig().getStringParam("gdb_dbname");
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
        Thread.sleep(5000);
        if(agent != "global_controller"){
            simulatedWDUpdate();
        }
        //reportToConsole(ce.getGDB().getAgentList(region));
        //ce.getGDB().getAgentList(region);
    }

    public void simulatedWDUpdate(){

        MsgEvent le = plugin.getRegionalControllerMsgEvent(MsgEvent.Type.WATCHDOG);

        le.setParam("desc","to-rc-agent");
        le.setParam("region_name",plugin.getRegion());
        le.setParam("agent_name",plugin.getAgent());


        String tmpJsonExport = ce.getPluginAdmin().getPluginExport();
        if(!jsonExport.equals(tmpJsonExport)) {

            jsonExport = tmpJsonExport;
            le.setCompressedParam("pluginconfigs", jsonExport);
        }
    }

    public void run() {
        try {
            MsgEvent m = Main.buildAddNodeMsg(region, agent,plugin_id);
            ce.getGDB().addNode(m);
            //m = Main.buildAddNodeMsg(region,agent,"some_plugin");
            //ce.getGDB().addNode(m);
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
            ce.getGDB().removeNode(region,agent,null);
        }
    }
}