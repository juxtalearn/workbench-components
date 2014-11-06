package org.juxtalearn.rias.components.contenttypeaggregator;
import info.collide.sqlspaces.commons.Tuple;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.AgentManager;
import eu.sisob.components.framework.componentdescription.BooleanField;
import eu.sisob.components.framework.componentdescription.Container;
import eu.sisob.components.framework.componentdescription.Filter;
import eu.sisob.components.framework.componentdescription.Input;
import eu.sisob.components.framework.componentdescription.Output;

public class ContenttypeAggregatorManager extends AgentManager {

    public ContenttypeAggregatorManager(Tuple templateCommandTuple, String mngId, String serverlocation, int port) {
        super(templateCommandTuple, mngId, serverlocation, port);
    }


    @Override
    protected void createAgent(Tuple commandTuple) {
        
        String param = commandTuple.getField(6).getValue().toString().trim();
        boolean keepExistingMeasures = Boolean.parseBoolean(param);
        
        Agent agent = new ContenttypeAggregatorAgent(commandTuple, super.getServerLocation(), super.getServerPort(), keepExistingMeasures);
        this.getAgentsList().add(agent);
        agent.setAgentListener(this);
        agent.initializeAgent();
        Thread runtime = new Thread(agent);
        runtime.start();
        logger.finer("ContentTypeAggregator agent started " + agent.getClass().getName());
    }

    public static void main(String[] args) {
        AgentManager proManager = new ContenttypeAggregatorManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "ContentType Aggregator", String.class, String.class), ContenttypeAggregatorManager.class.getName(), "localhost", 32525);
        //AgentManager proManager = new DeleteNodeOrEdgeManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Delete Node/Edge", String.class, String.class), DeleteNodeOrEdgeManager.class.getName(), "192.168.1.21", 32525);
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
                fields.add(new BooleanField("Keep existing measures: ", "value1", false));
                
                
                String description = "This JxL agent aggregates the artefacts of a JxL two mode network for each actor. It refers to commons.Nodetypes for inclusion of different types." ;
                String shortDescription_legend = "This JxL agent aggregates the artefacts of a JxL two mode network for each actor.";
                
                Container container = new Container(shortDescription_legend,description,inputs,outputs,fields);
                Filter filter = new Filter("ContentType Aggregator", "Tools", container);
                
                List<Filter> list = new ArrayList<Filter>(1);
                list.add(filter);
                
                return list;
        }
}