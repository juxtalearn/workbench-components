package org.juxtalearn.rias.components.mapmode;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import eu.sisob.components.framework.Agent;

public class MapModeAgent extends Agent {

    protected Integer sourceMode;

    protected Integer targetMode;

    protected String graphData;

    protected Tuple fetchedTuple,
            commandTuple;

    public MapModeAgent(Tuple commandTuple, String serverlocation, int port) {
        super(commandTuple, serverlocation, port);
        this.commandTuple = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);

        String[] params = this.getCommandTupleStructure().getField(6).getValue().toString().split(",");
        sourceMode = Integer.parseInt(params[0]);
        targetMode = Integer.parseInt(params[1]);

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
                boolean atLeastOneReplacement = false;
                /* re-mapping of node types */
                JSONObject data = (JSONObject) jsonGraphdata.get("data");
                JSONArray nodes = (JSONArray) data.get("nodes");
                for (Object rawnode : nodes) {
                    JSONObject node = (JSONObject) rawnode;
                    /* transform node of type sourceMode to targetMode */
                    if (node.containsKey("type")) {
                        if (sourceMode.equals(node.get("type"))) {
                            node.put("type", targetMode);
                            atLeastOneReplacement = true;
                        }
                    }
                }
                if (atLeastOneReplacement) {
                    /* reduce graph mode by 1 */
                    JSONObject jsonMetadata = (JSONObject) jsonGraphdata.get("metadata");
                    String graphType = (String) jsonMetadata.get("type");
                    int indx = graphType.indexOf("mode network");
                    int oldType = Integer.parseInt(graphType.substring(0, indx - 1));
                    String newType = Integer.toString(oldType - 1) + " mode network";
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

    }

    @Override
    public void executeAgent(ArrayList<Tuple> fetchedTuples) {}

    @Override
    protected void uploadResults() {
        fetchedTuple.getField(3).setValue(graphData);
        try {
            this.sisobspace.update(fetchedTuple.getTupleID(), fetchedTuple);
        } catch (TupleSpaceException e) {
            indicateError("Error updating data tuple", e);
            e.printStackTrace();
        }
    }
}
