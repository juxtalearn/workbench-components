package org.juxtalearn.rias.components.activitystreamtograph;

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

public class ActivitystreamToGraphManager extends AgentManager {

	public ActivitystreamToGraphManager(Tuple templateCommandTuple,
			String compId, String serverlocation, int port) {
		super(templateCommandTuple, compId, serverlocation, port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createAgent(Tuple commandTuple) {
		/* Creation of a new agent */
        Agent vis = new ActivitystreamToGraphAgent(commandTuple,super.getServerLocation(),super.getServerPort());
        getAgentsList().add(vis);
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
		String shortDescription = "The Agent converts an ActivityStream in SISOB Graph Format.";
		String longDescription = null;
		JSONArray inputs = new JSONArray();
		inputs.add(new Input("in_1", "activity stream"));
		JSONArray outputs = new JSONArray();
		outputs.add(new Output("out_1", "SISOB Graph Data"));
		JSONArray fields = null;
		Container container = new Container(shortDescription, longDescription, inputs, outputs, fields);
		Filter filter = new Filter("Activitystream To Graph Converter", "Data Converters", container);
		List<Filter> filters = new ArrayList<Filter>(1);
		filters.add(filter);
		return filters;
	}

}
