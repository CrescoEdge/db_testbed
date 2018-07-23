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

    public static Map<String,Object> getMockPluginConfig() {
        Map<String,Object> testConfig = new HashMap<>();
        testConfig.put("plugin_conf_1","confval_1");
        testConfig.put("plugin_conf_2","confval_2");
        return testConfig;
    }
    public static void main(String[] argv) {
        //Found this at https://stackoverflow.com/a/42860529
        Arrays.stream(LogManager.getLogManager().getLogger("").getHandlers()).forEach(h -> h.setLevel(Level.FINEST));
        LogManager.getLogManager().getLogger("").setLevel(Level.FINEST);

        Map<String,Object> testConfig = getMockPluginConfig();
        testConfig.putAll(getGDBConfigMap(null,null,null,null));
        testConfig.put("db_retry_count","50");
        PluginBuilder mypb = new PluginBuilder("global_controller","some_region","Main",testConfig);
        PluginAdmin mypluginAdmin = new PluginAdmin();
        mypluginAdmin.addPlugin("some_plugin","some_plugin.jar",testConfig);
        ControllerEngine parent_ce = new ControllerEngine(mypb,mypluginAdmin);
        parent_ce.startGDB();
        for(String pluginid: mypluginAdmin.getPluginMap().keySet()){
            parent_ce.getGDB().addNode(Main.buildAddNodeMsg(mypb.getRegion(),mypb.getAgent(),pluginid));
        }
        Thread global_controller = new Thread(new AgentProcess(0,parent_ce,null));
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
                PluginBuilder child_pluginBuilder = new PluginBuilder("agent-"+i,"some_region","AgentProcess",testConfig);
                PluginAdmin child_pluginAdmin = new PluginAdmin();
                child_pluginAdmin.addPlugin("some_plugin","some_plugin.jar",testConfig);
                ControllerEngine child_ce = new ControllerEngine(child_pluginBuilder,child_pluginAdmin);
                child_ce.setGDB(parent_ce.getGDB());
                for(String pluginid: child_pluginAdmin.getPluginMap().keySet()){
                    child_ce.getGDB().addNode(Main.buildAddNodeMsg(child_pluginBuilder.getRegion()
                    ,child_pluginBuilder.getAgent()
                    ,pluginid));
                }
                Thread otherAgentProcess = new Thread(new AgentProcess(agentLifetime,child_ce,parent_ce));
                otherAgentProcess.start();
                Thread.sleep(500);//stagger start times to increase concurrency pain
            }
            while(true){
                    //Thread.sleep(2000);
                    //System.out.println(parent_ce.getGDB().getAgentList("some_region"));
            }
        }
        catch(Exception ex) {
            System.err.println("Caught Exception!");
            ex.printStackTrace();
        }
    }
}
class AgentProcess implements Runnable {
    private ControllerEngine ce;
    private String region;
    private String agent;
    private PluginBuilder plugin;
    private long lifetimems;
    private String jsonExport;
    private ControllerEngine parent_ce;

    public AgentProcess(long lifetimems, ControllerEngine ce, ControllerEngine parent_ce) {
        this.ce = ce;
        this.region = ce.getPluginBuilder().getRegion();
        this.agent = ce.getPluginBuilder().getAgent();
        this.parent_ce = parent_ce;
        Map<String,Object> pluginConf = Main.getMockPluginConfig();
        String gdb_host = ce.getPluginBuilder().getConfig().getStringParam("gdb_host");
        String gdb_username = ce.getPluginBuilder().getConfig().getStringParam("gdb_username");
        String gdb_password = ce.getPluginBuilder().getConfig().getStringParam("gdb_password");
        String gdb_dbname = ce.getPluginBuilder().getConfig().getStringParam("gdb_dbname");
        if(gdb_host != null && gdb_username != null && gdb_password != null && gdb_dbname != null) {
            Map<String, Object> gdbConf = Main.getGDBConfigMap(gdb_host, gdb_username, gdb_password, gdb_dbname);
            pluginConf.putAll(gdbConf);
        }
        this.plugin = ce.getPluginBuilder();
        this.lifetimems = lifetimems;
    }

    public void reportToConsole(String msg){
        System.out.println("TS "+System.currentTimeMillis()+"[Region: "+region+" Agent:"+agent+" Plugin:"+ce.getPluginBuilder().getPluginID()+"] - "+msg);
    }

    public void doSomeStuff() throws InterruptedException {
        simulatedPluginChanges();
        Thread.sleep(5000);
        simulatedWDUpdate(ce);
    }

    public void simulatedPluginChanges(){
        Double rand = Math.random();
        if(rand <= 0.333){
            ce.getPluginAdmin().addPlugin("random_plugin_"+rand,"random_plugin.jar",new HashMap<>());
            reportToConsole("Added random plugin 'random_plugin_"+rand+"'");
        } else {
            //Do nothing for now. Might eventually make this remove plugins?
        }
    }
    public void simulatedWDUpdate(ControllerEngine ce){
        MsgEvent le = plugin.getRegionalControllerMsgEvent(MsgEvent.Type.WATCHDOG);
        le.setParam("desc","to-rc-agent");
        le.setParam("region_name",plugin.getRegion());
        le.setParam("agent_name",plugin.getAgent());
        String tmpJsonExport = ce.getPluginAdmin().getPluginExport();
        if(jsonExport == null || !jsonExport.equals(tmpJsonExport)) {

            jsonExport = tmpJsonExport;
            le.setCompressedParam("pluginconfigs", jsonExport);
        }
        ce.getGDB().watchDogUpdate(le);
    }

    public void run() {
        try {
                //MsgEvent m = Main.buildAddNodeMsg(region, agent,null);
                //ce.getGDB().addNode(m);

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
            ex.printStackTrace();
        }
        finally {
            //ce.getGDB().removeNode(region,agent,null);
        }
    }
}