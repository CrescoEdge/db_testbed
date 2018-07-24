package org.nms.crescodbtest;

//import org.jhades.JHades;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.jna.platform.win32.DBT;
import io.cresco.library.messaging.MsgEvent;

import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] argv) {
        //Found this at https://stackoverflow.com/a/42860529
        Arrays.stream(LogManager.getLogManager().getLogger("").getHandlers()).forEach(h -> h.setLevel(Level.FINEST));
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.FINEST);

        Map<String,Object>testConfig = DBTestHelpers.getMockPluginConfig(
                Optional.of(DBTestHelpers.getGDBConfigMap("localhost","root","root","crescodb"))
        );
        testConfig.put("db_retry_count","50");

        ControllerEngine parent_ce = DBTestHelpers.getControllerEngine("global_controller","some_region","Main"
                ,testConfig,Optional.empty());
        //parent_ce.startGDB();

        /*for(String pluginid: mypluginAdmin.getPluginMap().keySet()){
            parent_ce.getGDB().addNode(Main.buildAddNodeMsg(mypb.getRegion(),mypb.getAgent(),pluginid));
        }*/
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

                /*PluginBuilder child_pluginBuilder = new PluginBuilder("agent-"+i,"some_region","AgentProcess",testConfig);
                PluginAdmin child_pluginAdmin = new PluginAdmin();
                child_pluginAdmin.addPlugin("some_plugin","some_plugin.jar",testConfig);
                new ControllerEngine(child_pluginBuilder,child_pluginAdmin);*/
                ControllerEngine child_ce = DBTestHelpers.getControllerEngine("agent-"+i,"some_region"
                        ,"AgentProcess",testConfig,Optional.of(parent_ce));

                /*for(String pluginid: child_pluginAdmin.getPluginMap().keySet()){
                    child_ce.getGDB().addNode(Main.buildAddNodeMsg(child_pluginBuilder.getRegion()
                    ,child_pluginBuilder.getAgent()
                    ,pluginid));
                }*/
                Thread otherAgentProcess = new Thread(new AgentProcess(agentLifetime,child_ce,parent_ce));
                otherAgentProcess.start();
                Thread.sleep(100);//stagger start times to increase concurrency pain
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
