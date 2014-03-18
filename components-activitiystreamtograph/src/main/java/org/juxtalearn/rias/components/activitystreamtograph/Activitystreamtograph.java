package org.juxtalearn.rias.components.activitystreamtograph;

import info.collide.sqlspaces.commons.Tuple;
import eu.sisob.components.framework.AgentManager;

public class Activitystreamtograph {

    public static void main(String args[]) {
    	AgentManager proManager = new ActivitystreamToGraphManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Visualization To Server", String.class, String.class), ActivitystreamToGraphManager.class.getName(), "192.168.1.21", 32525);
        proManager.initialize();
        Thread runtime = new Thread(proManager);
        runtime.start();
    	
    }

}