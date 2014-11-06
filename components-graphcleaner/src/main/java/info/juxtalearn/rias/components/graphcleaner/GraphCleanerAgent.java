package info.juxtalearn.rias.components.graphcleaner;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.sisob.components.framework.Agent;

public class GraphCleanerAgent extends Agent {

    protected TreeSet<String> deleteType;

    protected String graphData;

    protected Tuple fetchedTuple,commandTuple;

    protected HashMap<String, JSONObject> nodeMap = new HashMap<>();

    private boolean normalizeTwoMode;

    private boolean forceBipartite;

    public GraphCleanerAgent(Tuple commandTuple, String serverlocation, int port, boolean normalizeTwoMode, boolean forceBipartite) {
        super(commandTuple, serverlocation, port);
        this.normalizeTwoMode = normalizeTwoMode;
        this.forceBipartite = forceBipartite;
        this.commandTuple = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
    }

    @SuppressWarnings(value = { "unchecked" })
    protected void transformData() {

        JSONArray jsonFiledata = null;
        JSONParser parser = new JSONParser();
        try {
            jsonFiledata = (JSONArray) parser.parse(graphData);
        } catch (ParseException e) {
            indicateError(e.getMessage(), e);
        }
        if (jsonFiledata != null) {

            String fileDataString = (String) ((JSONObject) jsonFiledata.get(0)).get("filedata");

            JSONObject jsonGraphdata = null;
            try {
                jsonGraphdata = (JSONObject) parser.parse(fileDataString);
            } catch (ParseException e) {
                indicateError(e.getMessage(), e);
            }
            if (jsonGraphdata != null) {
                JSONObject data = (JSONObject) jsonGraphdata.get("data");

                JSONArray nodes = (JSONArray) data.get("nodes");
                JSONArray edges = (JSONArray) data.get("edges");
                int maxId = 0;
                HashMap<String, String> nodeMap = new HashMap<String, String>();
                for (Object rawNode : nodes) {

                    JSONObject node = (JSONObject) rawNode;
                    //
                    // Iterator<String> it = node.keySet().iterator();
                    // LinkedList<String> toBeRemoved = new LinkedList<>();
                    if (node.containsKey("id")) {
                        String nodeId = node.get("id").toString();
                        try {
                            int currentNodeId = Integer.parseInt(nodeId);

                            if (Long.compare(currentNodeId, maxId) > 0) {
                                maxId = currentNodeId;
                            }

                        } catch (NumberFormatException e) {
                            // do nothing
                        }
                    }

                    // while (it.hasNext()) {
                    // String key = it.next();
                    // if (!("id".equals(key) || "label".equals(key) || "type".equals(key) || "timeappearance".equals(key))) {
                    // toBeRemoved.add(key);
                    // }
                    // }
                    // it = toBeRemoved.iterator();
                    // while (it.hasNext()) {
                    // node.remove(it.next());
                    // }
                    // if normalization is expected everything that is not if type "0" will be type "1"
                    if (normalizeTwoMode && node.containsKey("type")) {
                        if (!"0".equals(node.get("type"))) {
                            node.put("type", "1");
                        }
                    }
                    nodeMap.put(node.get("id").toString(), node.get("type").toString());
                }
                if (forceBipartite) {
                    JSONArray newEdges = new JSONArray();
                    for (Object rawEdge : edges) {

                        JSONObject edge = (JSONObject) rawEdge;
                        // Iterator<String> it = edge.keySet().iterator();
                        // LinkedList<String> toBeRemoved = new LinkedList<>();
                        // while (it.hasNext()) {
                        // String key = it.next();
                        // if (!("id".equals(key) || "source".equals(key) || "target".equals(key) || "timeappearance".equals(key))) {
                        // toBeRemoved.add(key);
                        // }
                        // }
                        if (!nodeMap.get(edge.get("source")).equals(nodeMap.get(edge.get("target")))) {
                            newEdges.add(edge);
                            // maxId++; // first increase the maxId to be greater than any node (with a number id)
                            // edge.put("id", Long.toString(maxId));
                            // it = toBeRemoved.iterator();
                            // while (it.hasNext()) {
                            // edge.remove(it.next());
                            // }
                        }
                    }
                    data.put("edges", newEdges);
                }
                // update metadata
                JSONObject metadata = (JSONObject) jsonGraphdata.get("metadata");
                if (metadata.containsKey("measures")) {
                    // metadata.remove("measures");
                    JSONArray measures = (JSONArray) metadata.get("measures");
                    JSONObject edgeWeightMeasure = new JSONObject();
                    edgeWeightMeasure.put("title", "weight");
                    edgeWeightMeasure.put("description", "strength of a link");
                    edgeWeightMeasure.put("type", "integer");
                    edgeWeightMeasure.put("class", "edge");
                    edgeWeightMeasure.put("property", "weight");
                    measures.add(edgeWeightMeasure);
                }
                
                
                // if (metadata.containsKey("nodeproperties")) {
                // metadata.remove("nodeproperties");
                // }
                if (normalizeTwoMode && metadata.containsKey("type")) {
                    metadata.put("type", "2 mode network");
                }
                // if (!metadata.containsKey("time")) {
                // metadata.put("time", "0");
                // }
                // metadata.put("title", "blabla.sgf");
                // if (!metadata.containsKey("description")) {
                // metadata.put("description", "No description given!");
                // }

                /* store changes */
                // jsonGraphdata.put("data", outputData);
                ((JSONObject) jsonFiledata.get(0)).put("filedata", jsonGraphdata.toJSONString());
                graphData = jsonFiledata.toJSONString();
                /* write/store changes */
                /*
                 * ((JSONObject) jsonFiledata.get(0)).put("filedata", jsonGraphdata.toJSONString()); graphData = jsonFiledata.toJSONString(); // logger.info("GraphData: "+graphData.toString());
                 */
            }
        }
    }

    @Override
    public void executeAgent(Tuple fetchedTuple) {
        this.fetchedTuple = fetchedTuple;
        this.indicateRunning();
        graphData = fetchedTuple.getField(3).getValue().toString();
        transformData();
        this.uploadResults();
        this.indicateDone();

    }

    @Override
    public void executeAgent(ArrayList<Tuple> fetchedTuples) {}

    @Override
    protected void uploadResults() {

        Field[] fields = fetchedTuple.getFields();
        fields[0].setValue(this.getWorkflowID());
        fields[1].setValue(1);
        fields[2].setValue(this.getAgentInstanceID() + ".out_1");
        fields[3].setValue(graphData);
        Tuple writeTuple = new Tuple(fields);

        // fetchedTuple.getField(3).setValue(graphData);
        try {
            // this.sisobspace.update(fetchedTuple.getTupleID(), fetchedTuple);
            this.getSisobspace().write(writeTuple);
        } catch (TupleSpaceException e) {
            indicateError("Error updating data tuple", e);
            e.printStackTrace();
        }
    }
}