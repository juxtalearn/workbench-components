package info.juxtalearn.rias.components.graphcleaner;
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

public class GraphCleanerManager extends AgentManager {

    public GraphCleanerManager(Tuple templateCommandTuple, String mngId, String serverlocation, int port) {
        super(templateCommandTuple, mngId, serverlocation, port);
    }


    @Override
    protected void createAgent(Tuple commandTuple) {
        
        String[] params = commandTuple.getField(6).getValue().toString().trim().split(",");
        
        boolean normalizeTwoMode = Boolean.parseBoolean(params[0]);
        boolean forceBipartite = Boolean.parseBoolean(params[1]);
        Agent agent = new GraphCleanerAgent(commandTuple, super.getServerLocation(), super.getServerPort(),normalizeTwoMode,forceBipartite);
        this.getAgentsList().add(agent);
        agent.setAgentListener(this);
        agent.initializeAgent();
        Thread runtime = new Thread(agent);
        runtime.start();
        logger.finer("Graphcleaner agent started " + agent.getClass().getName());
    }

    public static void main(String[] args) {
        AgentManager proManager = new GraphCleanerManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Graphcleaner", String.class, String.class), GraphCleanerManager.class.getName(), "localhost", 32525);
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
                fields.add(new BooleanField("Normalize to 0 - 1 two-mode network?", "value1", false,true));
                fields.add(new BooleanField("Force bipartite?to 0 - 1 two-mode network?", "value2", false,true));
                
                String description = "This agent removes all measures and edgeweights, labels etc. and additional node information (apart from node labels and types). Works on Sisob graph formats only!" ;
                String shortDescription_legend = "Cleans up the network information by removing metadata from nodes and edges as well as measures.";
                
                Container container = new Container(shortDescription_legend,description,inputs,outputs,fields);
                Filter filter = new Filter("Graphcleaner", "Tools", container);
                
                List<Filter> list = new ArrayList<Filter>(1);
                list.add(filter);
                
                return list;
        }
}