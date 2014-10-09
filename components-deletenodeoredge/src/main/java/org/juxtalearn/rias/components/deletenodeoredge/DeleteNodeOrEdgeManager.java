package org.juxtalearn.rias.components.deletenodeoredge;
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
import eu.sisob.components.framework.componentdescription.IntField;
import eu.sisob.components.framework.componentdescription.Output;
import eu.sisob.components.framework.componentdescription.SelectField;

public class DeleteNodeOrEdgeManager extends AgentManager {

    public DeleteNodeOrEdgeManager(Tuple templateCommandTuple, String mngId, String serverlocation, int port) {
        super(templateCommandTuple, mngId, serverlocation, port);
    }


    @Override
    protected void createAgent(Tuple commandTuple) {
        Agent agent = new DeleteNodeOrEdgeAgent(commandTuple, super.getServerLocation(), super.getServerPort());
        this.getAgentsList().add(agent);
        agent.setAgentListener(this);
        agent.initializeAgent();
        Thread runtime = new Thread(agent);
        runtime.start();
        logger.finer("DeleteNodeOrEdge agent started " + agent.getClass().getName());
    }

    public static void main(String[] args) {
        AgentManager proManager = new DeleteNodeOrEdgeManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Delete Node/Edge", String.class, String.class), DeleteNodeOrEdgeManager.class.getName(), "localhost", 32525);
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
                fields.add(new IntField("Elements: ", "value1", true));
                fields.add(new SelectField("Attribute:", "elementattribute", true, new String[]{"type","id"}));
                fields.add(new SelectField("Elements to be deleted:", "elementtype", true, new String[]{"Nodes","Edges"}));
                fields.add(new BooleanField("Delete these elements?:", "invertdeletion", true, true));
                String description = "This filter deletes every node/edge of transferred attributes.";
                String shortDescription_legend = "Deletes all nodes/edges of transferred attributes.";
                
                Container container = new Container(shortDescription_legend,description,inputs,outputs,fields);
                Filter filter = new Filter("Delete Node/Edge", "Tools", container);
                
                List<Filter> list = new ArrayList<Filter>(1);
                list.add(filter);
                
                return list;
        }
}