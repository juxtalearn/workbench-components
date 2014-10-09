package org.juxtalearn.rias.components.deletenodeoredge;

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

public class DeleteNodeOrEdgeAgent extends Agent {

	protected TreeSet<String> values;

	protected String graphData;

	protected Tuple fetchedTuple, commandTuple;
	/* delete nodes or edges? */
	protected boolean deleteNodes = true;
	/* delete entries or keep them */
	protected boolean deleteEntries = true;
	/* which node attribute to look for? */
	protected String attribute;
	/* transmitted values */
	protected String[] valueStrings;

	public DeleteNodeOrEdgeAgent(Tuple commandTuple, String serverlocation,
			int port) {
		super(commandTuple, serverlocation, port);
		this.commandTuple = commandTuple;
		Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue()
				.toString(), 1, commandTuple.getField(5).getValue().toString(),
				Field.createWildCardField());
		setDataTupleStructure(dataTemplate);

		String[] params = this.getCommandTupleStructure().getField(6)
				.getValue().toString().split(",");

		attribute = params[1];

		/* TODO what about multiple params (more than nodes/edges)? */
		if (!params[params.length - 2].equals("Nodes")) {
			deleteNodes = false;
		}
		if (params[params.length - 1].equals("false")) {
			deleteEntries = false;
		}

		// logger.info("Delete what? => " + params[params.length-1] +
		// " | deleteNodes: "+deleteNodes);
		valueStrings = params[0].split(";");
	}

	protected void determineValues(String[] valueStrings, JSONArray edges) {
		values = new TreeSet<String>();
		for (int i = 0; i < valueStrings.length; i++) {
			values.add(valueStrings[i]);
		}
		if ("id".equals(attribute)) {
			for (Object rawEdge : edges) {
				JSONObject edge = (JSONObject) rawEdge;
				String source = edge.get("source").toString();
				String target = edge.get("target").toString();
				for (int i = 0; i < valueStrings.length; i++) {
					if (source.equals(valueStrings[i])) {
						values.add(target);
					} else if(target.equals(valueStrings[i])) {
						values.add(source);
					}
				}
			}
		} else {
			for (int i = 0; i < valueStrings.length; i++) {
				values.add(valueStrings[i]);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected JSONArray filterNodes(JSONArray data) {
		JSONArray newData = new JSONArray();
		for (Object rawelement : data) {
			JSONObject element = (JSONObject) rawelement;

			if (element.containsKey(attribute)) {
				String nodeAttribute = (String) element.get(attribute);
				// delete or keep them?
				if (deleteEntries) {
					// delete
					if (!values.contains(nodeAttribute)) {
						newData.add(element);
					}
				} else {
					// keep
					if (values.contains(nodeAttribute)) {
						newData.add(element);
					}
				}
			} else {
				/* TODO shall nodes/edges with no type-property remain in data */
				// logger.info("No type => add it as well");
				newData.add(element);
			}
		}
		return newData;

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

			String fileDataString = (String) ((JSONObject) jsonFiledata.get(0))
					.get("filedata");

			JSONObject jsonGraphdata = null;
			try {
				jsonGraphdata = (JSONObject) parser.parse(fileDataString);
			} catch (ParseException e) {
				indicateError(e.getMessage(), e);
			}
			
			if (jsonGraphdata != null) {
				JSONObject data = (JSONObject) jsonGraphdata.get("data");
				JSONArray oldData = null;
				
				if (deleteNodes) {
					oldData = (JSONArray) data.get("nodes");
				} else {
					oldData = (JSONArray) data.get("edges");
				}

				determineValues(valueStrings, (JSONArray) data.get("edges"));
				
				logger.info("Values (" + attribute + ") to delete/keep: "
						+ values.toString());
				
				JSONArray newData = filterNodes(oldData);
				if (deleteNodes) {
					// look at the edges
					JSONArray oldEdges = (JSONArray) data.get("edges");
					HashMap<String, Boolean> hm = new HashMap<String,Boolean>();
					JSONArray newEdges = new JSONArray();
					// prepare hashmap for nodeIds of the surviving nodes
					for (Object rawelement : newData) {
						JSONObject element = (JSONObject) rawelement;
						String nodeId = element.get("id").toString();
						hm.put(nodeId, true);
					}

					for (Object rawEdge : oldEdges) {
						JSONObject edge = (JSONObject) rawEdge;
						String source = edge.get("source").toString();
						String target = edge.get("target").toString();
						if (hm.containsKey(source) && hm.containsKey(target)) {
							newEdges.add(edge);
						}
					}

					/* replace data */
					data.put("nodes", newData);
					data.put("edges", newEdges);
					logger.info("(Deleted Nodes) Edge size before: "+oldEdges.size()+ " - "+newEdges.size());
				} else {
					data.put("edges", newData);
				}

				logger.info("Size before: " + oldData.size() + " - after "
						+ newData.size());

				/* write/store changes */
				((JSONObject) jsonFiledata.get(0)).put("filedata",
						jsonGraphdata.toJSONString());
				graphData = jsonFiledata.toJSONString();
				// logger.info("GraphData: "+graphData.toString());
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
	public void executeAgent(ArrayList<Tuple> fetchedTuples) {
	}

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