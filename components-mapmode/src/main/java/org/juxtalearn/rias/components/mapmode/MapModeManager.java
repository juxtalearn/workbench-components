package org.juxtalearn.rias.components.mapmode;

import info.collide.sqlspaces.commons.Tuple;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.AgentManager;
import eu.sisob.components.framework.componentdescription.Container;
import eu.sisob.components.framework.componentdescription.Filter;
import eu.sisob.components.framework.componentdescription.Input;
import eu.sisob.components.framework.componentdescription.IntField;
import eu.sisob.components.framework.componentdescription.Output;

public class MapModeManager extends AgentManager {

    public MapModeManager(Tuple templateCommandTuple, String mngId, String serverlocation, int port) {
        super(templateCommandTuple, mngId, serverlocation, port);
    }


    @Override
    protected void createAgent(Tuple commandTuple) {
        Agent agent = new MapModeAgent(commandTuple, super.getServerLocation(), super.getServerPort());
        this.getAgentsList().add(agent);
        agent.setAgentListener(this);
        agent.initializeAgent();
        Thread runtime = new Thread(agent);
        runtime.start();
        logger.finer("SaveProcess agent started " + agent.getClass().getName());
    }

    public static void main(String[] args) {
    	 AgentManager proManager = new MapModeManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Map Mode", String.class, String.class), MapModeManager.class.getName(), "localhost", 32525);
    	//AgentManager proManager = new ReduceModeManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Reduce Mode", String.class, String.class), ReduceModeManager.class.getName(), "analyticstk.rias-institute.eu", 2525);
        proManager.initialize();
        Thread runtime = new Thread(proManager);
        runtime.start();
    }
    
    @SuppressWarnings("unchecked")
	@Override
	protected List<Filter> getFilterDescriptions() {
		JSONArray inputs = new JSONArray();
		inputs.add(new Input("in_1", "input data"));
		
		JSONArray outputs = new JSONArray();
		outputs.add(new Output("out_1","out data"));
		JSONArray fields = new JSONArray();
		fields.add(new IntField("Source Mode: ", "value1", true));
		fields.add(new IntField("Target Mode", "value2", true));
		String description = "This filter transforms every node of type \"source mode\" to \"target mode\". This reduces the available modes and therefore a n-mode graph becomes a (n-1)-mode graph." ;
		String shortDescription_legend = "Reduces a n-mode graph to a (n-1)-mode graph";
		
		Container container = new Container(shortDescription_legend,description,inputs,outputs,fields);
		Filter filter = new Filter("Map Mode", "Tools", container);
		
		List<Filter> list = new ArrayList<Filter>(1);
		list.add(filter);
		
		return list;
	}
    
}
