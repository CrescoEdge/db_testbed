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

class TestAgent extends AgentProcess {
    public TestAgent(long lifetimems, ControllerEngine ce){
        super(lifetimems,ce);
    }
    public void doSomeStuff(){
        try {
            /*MsgEvent add_remove_msg = DBTestHelpers.buildAddNodeMsg("test_region","test_agent","test_plugin");
            getCe().getGDB().addNode(add_remove_msg);
            Thread.sleep(500);
            Optional<String> node_id = Optional.of(getCe().getGDB().gdb.getNodeId("test_region","test_agent","test_plugin"));
            if(node_id.isPresent()) {
                getCe().getGDB().removeNode(add_remove_msg);
            }*/

            //this.simulatedWDUpdate();
            DBBaseFunctions gdb = getCe().getGDB().gdb;
            String node_id = gdb.getNodeId("test_region", "test_agent", "test_plugin");
            if (node_id == null) {
                gdb.addNode("test_region", "test_agent", "test_plugin");
                node_id = gdb.getNodeId("test_region", "test_agent", "test_plugin");
            }
            gdb.setNodeParam(node_id,"test_param_key","test_param_value");
        }
        catch(Exception ex){
            getAgentLogger().log(Level.SEVERE,"Exception in 'doSomeStuff' of thread "+Thread.currentThread().getId(),ex);
        }

    }
}

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

        Thread global_controller = new Thread(new TestAgent(0,parent_ce));
        global_controller.start();

        try{
            int numberOfAgents = argv != null ? Integer.parseInt(argv[0]) : 1;
            long agentLifetime = 0;
            for(int i = 0;i < numberOfAgents;i++) {
                ControllerEngine child_ce = DBTestHelpers.getControllerEngine("agent-"+i,"some_region"
                        ,"AgentProcess",testConfig,Optional.of(parent_ce));

                Thread otherAgentProcess = new Thread(new TestAgent(agentLifetime,child_ce));
                otherAgentProcess.start();
                Thread.sleep(100);//stagger start times to increase concurrency pain
            }
            while(true){
                    //sit and spin
            }
        }
        catch(Exception ex) {
            System.err.println("Caught Exception!");
            ex.printStackTrace();
        }
    }
}
