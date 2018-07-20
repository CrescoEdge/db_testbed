package org.nms.crescodbtest;

import io.cresco.agent.controller.app.gPayload;
import io.cresco.agent.controller.communication.KPIProducer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;

public class ControllerEngine {
    private PluginBuilder pluginBuilder;
    private BlockingQueue<gPayload> appScheduleQueue;
    private KPIProducer kpip;
    private Boolean DBManagerActive;
    private DBInterface gdb;

    public ControllerEngine(PluginBuilder plugin){
        this.pluginBuilder = plugin;

    }

    public DBInterface getGDB(){
        return this.gdb;
    }
    public PluginBuilder getPluginBuilder() {
        return pluginBuilder;
    }

    public String getStringFromError(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    public BlockingQueue<gPayload> getAppScheduleQueue() {
        return appScheduleQueue;
    }

    public KPIProducer getKPIProducer() {
        return kpip;
    }

    public Boolean isDBManagerActive(){
        return this.DBManagerActive;
    }
    public void setDBManagerActive(boolean DBManagerActive) {
        this.DBManagerActive = DBManagerActive;
    }

    public void startGDB(){
        gdb = new DBInterface(this);
    }
}
