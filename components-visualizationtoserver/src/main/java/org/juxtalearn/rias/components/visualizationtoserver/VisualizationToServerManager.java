package org.juxtalearn.rias.components.visualizationtoserver;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import info.collide.sqlspaces.commons.Tuple;
import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.AgentManager;
import eu.sisob.components.framework.componentdescription.Container;
import eu.sisob.components.framework.componentdescription.Filter;
import eu.sisob.components.framework.componentdescription.Input;
import eu.sisob.components.framework.componentdescription.IntField;
import eu.sisob.components.framework.componentdescription.StringField;

public class VisualizationToServerManager extends AgentManager{

	public VisualizationToServerManager(Tuple templateCommandTuple,
			String compId, String serverlocation, int port) {
		super(templateCommandTuple, compId, serverlocation, port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createAgent(Tuple commandTuple) {
		Agent agent = new VisualizationToServer(commandTuple, super.getServerLocation(), super.getServerPort());
        this.getAgentsList().add(agent);
        agent.setAgentListener(this);
        agent.initializeAgent();
        Thread runtime = new Thread(agent);
        runtime.start();
        logger.finer("VisualizationToServerManager agent started " + agent.getClass().getName());
	}

	@Override
	protected List<Filter> getFilterDescriptions() {
		JSONArray inputs = new JSONArray();
		inputs.add(new Input("in_1", "HTML Data"));
		
		JSONArray outputs = null;
		JSONArray fields = new JSONArray();
		
		fields.add(new StringField("ReturnURL: ", "value1", true));
		fields.add(new IntField("ReturnId: ", "value2", false));
		fields.add(new StringField("AuthToken: ", "value3",false));
		//fields.add(new BooleanField("Location is file","value3",true,false));
		String description = "If you provide an AuthToken, you have to provide ReturnId, too!";
		String shortDescription_legend = "This agent stores the resulting html to a remote server.";
		
		Container container = new Container(shortDescription_legend,description,inputs,outputs,fields);
		Filter filter = new Filter("Visualization To Server", "Output", container);
		
		List<Filter> list = new ArrayList<Filter>(1);
		list.add(filter);
		
		return list;
	}
	
	public static void main(String[] args) {
        //AgentManager proManager = new VisualizationToServerManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Visualization To Server", String.class, String.class), VisualizationToServer.class.getName(), "localhost", 2525);
        AgentManager proManager = new VisualizationToServerManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Visualization To Server", String.class, String.class), VisualizationToServer.class.getName(), "192.168.1.21", 32525);
        //AgentManager proManager = new VisualizationToServerManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Visualization To Server", String.class, String.class), VisualizationToServer.class.getName(), "analyticstk.rias-institute.eu", 2525);
        proManager.initialize();
        Thread runtime = new Thread(proManager);
        runtime.start();
    }

}
