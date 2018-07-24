package org.nms.crescodbtest;

import io.cresco.library.messaging.MsgEvent;

import java.util.HashMap;

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
        this.plugin = ce.getPluginBuilder();
        this.lifetimems = lifetimems;
    }

    public void reportToConsole(String msg){
        System.out.println("TS "+System.currentTimeMillis()+"[Region: "+region+" Agent:"+agent+"] - "+msg);
    }

    public void doSomeStuff() throws InterruptedException {
        simulatedPluginChanges();
        Thread.sleep(1000);
        simulatedWDUpdate(ce);
    }

    public void simulatedPluginChanges(){
        Double rand = Math.random();
        if(rand < 0.05){
            ce.getPluginAdmin().addPlugin("random_plugin_"+rand,"random_plugin.jar",new HashMap<>());
            //reportToConsole("Added random plugin 'random_plugin_"+rand+"'");
        }
         /*else if(rand >= 0.333 && rand < 0.666) {
            //ce.getGDB().removeNode(plugin.getRegion(),plugin.getAgent(),null);
            String pluginList = ce.getGDB().getPluginList(plugin.getRegion(),plugin.getAgent());
            Type maptype = new TypeToken<Map<String,List<Map<String,String>>>>(){}.getType();
            Map<String,List<Map<String,String>>> pluginMaps = Main.gson.fromJson(pluginList,maptype);
            List pluginConfigs = pluginMaps.get("plugins");
            Map<String,String> toRemove = (Map<String,String>)pluginConfigs.get(pluginConfigs.size()-1);
            ce.getGDB().removeNode(plugin.getRegion(),plugin.getAgent(),toRemove.get("name"));

        }*/ else {
            //do nothing
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