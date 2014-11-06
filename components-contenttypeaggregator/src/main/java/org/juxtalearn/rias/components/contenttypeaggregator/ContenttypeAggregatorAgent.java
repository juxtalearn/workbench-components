package org.juxtalearn.rias.components.contenttypeaggregator;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import org.juxtalearn.rias.components.commons.NodeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.sisob.components.framework.Agent;

public class ContenttypeAggregatorAgent extends Agent {

    protected TreeSet<String> deleteType;

    protected String graphData;

    protected Tuple fetchedTuple,
            commandTuple;

    protected JSONArray outputData = new JSONArray();

    protected HashMap<String, JSONObject> nodeMap = new HashMap<>();

    private boolean keepMeasures;

    public ContenttypeAggregatorAgent(Tuple commandTuple, String serverlocation, int port, boolean keepMeasures) {
        super(commandTuple, serverlocation, port);
        this.keepMeasures = keepMeasures;

        this.commandTuple = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
    }

    private void addToType(HashMap<String, HashMap<String, Integer>> occurrences, String nodeId, String type) {
        if (!occurrences.containsKey(nodeId)) {
            occurrences.put(nodeId, new HashMap<String, Integer>());
        }
        HashMap<String, Integer> counter = occurrences.get(nodeId);
        if (counter.containsKey(type)) {
            counter.put(type, counter.get(type) + 1);
        } else {
            counter.put(type, 1);
        }
    }

    private void writeJsonOutputNode(String id, HashMap<String, Integer> counter) {
        JSONObject node = nodeMap.get(id);
        JSONObject newNode = new JSONObject();
        newNode.put("id", id);
        newNode.put("label", (String) node.get("label"));
        newNode.put("type", (String) node.get("type"));
        /*
         * String[] time = (String[]) node.get("timeappearence"); newNode.put("timeappearence", time);e logger.info("Time: "+node.get("timeappearence")+" | "+time);
         */
//        for (Map.Entry e : counter.entrySet()) {
//            logger.finest("id: " + id + " | " + e.getKey() + " = " + e.getValue());
//        }

        for (NodeTypes nodeType : NodeTypes.values()) {
            if (!nodeType.isUser() ) {
                if (counter.containsKey(nodeType.getTypeString())) {
                    newNode.put(nodeType.getTypeString(), counter.get(nodeType.getTypeString()));
                } else {
                    newNode.put(nodeType.getTypeString(), 0);
                }
            }
        }
        outputData.add(newNode);
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

                List<String> userNodeIds = new ArrayList<String>();
                for (Object rawNode : nodes) {
                    JSONObject node = (JSONObject) rawNode;
                    if (node.containsKey("objectType")) {
                        String nodeType = (String) node.get("objectType");
                        String nodeId = (String) node.get("id");
                        nodeMap.put(nodeId, node);
                        NodeTypes enumType = NodeTypes.getEnum(nodeType);
                        if (enumType.isUser()) {
                            userNodeIds.add(nodeId);
                        }
                    }
                }

                HashMap<String, HashMap<String, Integer>> occurrences = new HashMap<>();
                for (Object rawEdge : edges) {
                    JSONObject edge = (JSONObject) rawEdge;
                    String source = edge.get("source").toString();
                    String target = edge.get("target").toString();
                    String edgeType = edge.get("type").toString();
                    String type = null;
                    JSONObject node = null;
                    if (userNodeIds.contains(source)) {
                        node = nodeMap.get(target);
                        type = (String) node.get("objectType");
                        addToType(occurrences, source, type);
                    } else if (userNodeIds.contains(target)) {
                        node = nodeMap.get(source);
                        type = (String) node.get("objectType");
                        addToType(occurrences, target, type);
                    }
                }

                Iterator<Entry<String, HashMap<String, Integer>>> it = occurrences.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, HashMap<String, Integer>> pair = (Map.Entry<String, HashMap<String, Integer>>) it.next();
                    String id = pair.getKey();
                    HashMap<String, Integer> counter = pair.getValue();
                    writeJsonOutputNode(id, counter);
                    it.remove(); // avoids a ConcurrentModificationException
                }

                // update metadata
                JSONObject metadata = (JSONObject) jsonGraphdata.get("metadata");
                JSONArray measures = (JSONArray) metadata.get("measures");
                if  ( !keepMeasures ) { //throw away all other measures.
                    measures.clear();
                }
                for (NodeTypes nodeType : NodeTypes.values()) {
                    if (!nodeType.isUser()&& nodeType.isUseAsMeasure()) {
                        JSONObject newMeasure = new JSONObject();
                        newMeasure.put("title", nodeType.getTypeString());
                        newMeasure.put("description", nodeType.getTypeString());
                        newMeasure.put("class", "node");
                        newMeasure.put("property", nodeType.getTypeString());
                        newMeasure.put("type", "integer");
                        measures.add(newMeasure);
                    }
                }

                /* store changes */
                jsonGraphdata.put("data", outputData);
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
        System.out.println("finished");

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