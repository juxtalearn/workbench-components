package org.juxtalearn.rias.components.mapmode;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.util.ArrayList;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.sisob.components.framework.Agent;

public class MapModeAgent extends Agent {

    protected TreeSet<Long> sourceMode;

    protected Long targetMode;

    protected String graphData;

    protected Tuple fetchedTuple,
            commandTuple;

    public MapModeAgent(Tuple commandTuple, String serverlocation, int port) {
        super(commandTuple, serverlocation, port);
        this.commandTuple = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);

        String[] params = this.getCommandTupleStructure().getField(6).getValue().toString().split(",");
        String[] sourceModeStrings = params[0].split(";");
        sourceMode = new TreeSet<Long>();

        for (int i = 0; i < sourceModeStrings.length; i++) {
            sourceMode.add(Long.parseLong(sourceModeStrings[i]));
        }
        logger.info(sourceMode.toString());
        targetMode = Long.parseLong(params[1]);

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
                Object o = parser.parse(fileDataString);
                jsonGraphdata = (JSONObject) parser.parse(fileDataString);
            } catch (ParseException e) {
                indicateError(e.getMessage(), e);
            }
            if (jsonGraphdata != null) {
                TreeSet<Long> replacedTypes = new TreeSet<Long>();
                boolean isTargetModeNew = true;
                /* re-mapping of node types */
                JSONObject data = (JSONObject) jsonGraphdata.get("data");
                JSONArray nodes = (JSONArray) data.get("nodes");

                for (Object rawnode : nodes) {
                    JSONObject node = (JSONObject) rawnode;
                    /* transform node of type sourceMode to targetMode */
                    if (node.containsKey("type")) {
                        Long nodeType = (Long) node.get("type");
                        if (sourceMode.contains(nodeType)) {
                            node.put("type", targetMode);
                            replacedTypes.add(nodeType);
                        } else if (isTargetModeNew && targetMode.equals(nodeType)) {
                            isTargetModeNew = false;
                        }
                    }
                }
                if (!replacedTypes.isEmpty()) {
                    /* reduce graph mode by amount of replaced modes */
                    JSONObject jsonMetadata = (JSONObject) jsonGraphdata.get("metadata");
                    String graphType = (String) jsonMetadata.get("type");
                    int indx = graphType.indexOf("mode network");
                    int oldType = Integer.parseInt(graphType.substring(0, indx - 1));
                    String newType = null;
                    StringBuffer logInfo = new StringBuffer();
                    for (long type : replacedTypes) {
                        logInfo.append(type).append(" ");
                    }
                    
                    logger.info("Replaced "+replacedTypes.size() + " types:"+logInfo.toString());
                    if (isTargetModeNew || sourceMode.contains(targetMode)) {
                        logger.info("Added a new type!");
                        newType = Integer.toString(oldType - replacedTypes.size() + 1) + " mode network";
                    } else {
                        logger.info("Just projected to  an existing type!");
                        newType = Integer.toString(oldType - replacedTypes.size()) + " mode network";
                    }
                    jsonMetadata.put("type", newType);

                    /* write/store changes */
                    ((JSONObject) jsonFiledata.get(0)).put("filedata", jsonGraphdata.toJSONString());
                    graphData = jsonFiledata.toJSONString();
                }
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
           //this.sisobspace.update(fetchedTuple.getTupleID(), fetchedTuple);
            this.getSisobspace().write(writeTuple);
        } catch (TupleSpaceException e) {
            indicateError("Error updating data tuple", e);
            e.printStackTrace();
        }
    }
}
