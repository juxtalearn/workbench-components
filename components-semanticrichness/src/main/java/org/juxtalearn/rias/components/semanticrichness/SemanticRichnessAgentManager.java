package org.juxtalearn.rias.components.semanticrichness;

import info.collide.sqlspaces.commons.Tuple;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.AgentManager;
import eu.sisob.components.framework.componentdescription.Container;
import eu.sisob.components.framework.componentdescription.Filter;
import eu.sisob.components.framework.componentdescription.Input;
import eu.sisob.components.framework.componentdescription.Output;

public class SemanticRichnessAgentManager extends AgentManager {

	public SemanticRichnessAgentManager(Tuple templateCommandTuple,
			String compId, String serverlocation, int port) {
		super(templateCommandTuple, compId, serverlocation, port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createAgent(Tuple commandTuple) {
		/* Creation of a new agent */
	Agent vis = new SemanticRichnessAgent(commandTuple,super.getServerLocation(),super.getServerPort());
        getAgentsList().add(vis);
        logger.info("instantiating "+SemanticRichnessAgent.class.getSimpleName());

        /* Initializing the new agent */
        vis.initializeAgent();
        /* Adding the listeners */
        vis.setAgentListener(this);
        /* Creating the agent runtime */
        Thread runTime = new Thread(vis);
        /* Executing the agent */
        runTime.start();
	}

	@Override
	protected List<Filter> getFilterDescriptions() {
		String shortDescription = "This Agent evaluates the semantic richness for texts from an activity stream based on a dictionary.";
		String longDescription = null;
		JSONArray inputs = new JSONArray();
		inputs.add(new Input("in_1", "activity stream"));
		JSONArray outputs = new JSONArray();
		outputs.add(new Output("out_1", "activity stream"));
		JSONArray fields = null;
		Container container = new Container(shortDescription, longDescription, inputs, outputs, fields);
		Filter filter = new Filter("JxL Semantic Richness", "Data Converters", container);
		List<Filter> filters = new ArrayList<Filter>(1);
		filters.add(filter);
		return filters;
	}
	
        public static void main(String args[]) {
            
            AgentManager proManager = new SemanticRichnessAgentManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "JxL Semantic Richness", String.class, String.class), SemanticRichnessAgentManager.class.getName(), "192.168.1.21", 32525);
            proManager.initialize();
            Thread runtime = new Thread(proManager);
            runtime.start();
            
        }
	
	

}
