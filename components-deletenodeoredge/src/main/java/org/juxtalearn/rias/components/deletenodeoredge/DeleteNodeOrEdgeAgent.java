package org.juxtalearn.rias.components.deletenodeoredge;

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

public class DeleteNodeOrEdgeAgent extends Agent {

	protected TreeSet deleteType;

	protected String graphData;

	protected Tuple fetchedTuple, commandTuple;
	protected boolean deleteNodes = true;

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
		/* TODO what about multiple params (more than nodes/edges)? */
		if (!params[params.length - 1].equals("Nodes")) {
			deleteNodes = false;

		}
		// logger.info("Delete what? => " + params[params.length-1] +
		// " | deleteNodes: "+deleteNodes);
		String[] deleteTypeStrings = params[0].split(";");

		if (deleteNodes) {
			// Nodes -> Type = Long
			deleteType = new TreeSet<Long>();
			for (int i = 0; i < deleteTypeStrings.length; i++) {
				deleteType.add(Long.parseLong(deleteTypeStrings[i]));
			}
		} else {
			// e.g. Edges -> Type = String
			deleteType = new TreeSet<String>();
			for (int i = 0; i < deleteTypeStrings.length; i++) {
				deleteType.add(deleteTypeStrings[i]);
			}
		}
		logger.info("Type(s) to delete: " + deleteType.toString());

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
				Object o = parser.parse(fileDataString);
				jsonGraphdata = (JSONObject) parser.parse(fileDataString);
			} catch (ParseException e) {
				indicateError(e.getMessage(), e);
			}
			if (jsonGraphdata != null) {
				/* delete nodes by type */
				JSONObject data = (JSONObject) jsonGraphdata.get("data");
				JSONArray oldData = null;
				JSONArray newData = new JSONArray();

				if (deleteNodes) {
					oldData = (JSONArray) data.get("nodes");
				} else {
					oldData = (JSONArray) data.get("edges");
				}

				for (Object rawelement : oldData) {
					JSONObject element = (JSONObject) rawelement;
					if (element.containsKey("type")) {
						Object nodeType = null;
						if (deleteNodes) {
							// Nodes
							nodeType = (Long) element.get("type");
						} else {
							// e.g. Edges
							nodeType = (String) element.get("type");
						}
						if (!deleteType.contains(nodeType)) {
							newData.add(element);
						}
					} else {
						/*
						 * TODO nodes/edges with no type-property remain in data
						 * (?!)
						 */
						// logger.info("No type => add it as well");
						newData.add(element);
					}
				}

				/* replace data */
				if (deleteNodes) {
					data.put("nodes", newData);
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