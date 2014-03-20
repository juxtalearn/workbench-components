package org.juxtalearn.rias.components.activitystreamtograph;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import eu.sisob.components.framework.Agent;

public class ActivitystreamToTableAgent extends Agent{

	public ActivitystreamToTableAgent(Tuple commandTuple,
			String serverlocation, int port) {
		super(commandTuple, serverlocation, port);
		this.commandTupleStructure = commandTuple;
		
		Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
	}

	@Override
	public void executeAgent(Tuple fetchedTuple) {
		this.indicateRunning();
		try {
			JSONArray fetchedTupleArray = new JSONArray(fetchedTuple.getField(3).getValue().toString());
			
			for(int y = 0; y < fetchedTupleArray.length(); y++) {
				//filename, filetype, specialfiletype, filedata
				JSONObject fetchedTupleData = fetchedTupleArray.getJSONObject(y);
				
				JSONArray filedata = new JSONArray(fetchedTupleData.get("filedata").toString());
				
				//The output Sisob data table element
				JSONObject sisobDataTable = new JSONObject();
				//sisobDataTable.metadata
				sisobDataTable.put("metadata", new JSONObject());
				((JSONObject)sisobDataTable.get("metadata")).put("title", fetchedTupleData.get("filename"));
				//sisobDataTable.data
				sisobDataTable.put("data", new JSONArray());
				
				//convert input data to sisob data table format
				for (int i = 0; i < filedata.length(); i++) {
					JSONObject filedataElement = new JSONObject(filedata.get(i).toString());//(JSONObject) filedata.get(i);
					//filedataElement.actor: { IPAdress, displayName, objectType, groupID, courseID}
					//				 .verb: verb,
					//				 .object: { objectID, objectType, objectSubtype, objectClass, ownerGUID, content, groupID }
					//				 .published:
					//				 .transactionID: value
					
					//write Actor to output Format
					JSONObject sisobDataTableActor = createTemplate(filedataElement);
					sisobDataTableActor.put("label", "actor");
					if(filedataElement.has("actor")) {
						sisobDataTableActor.put("labelId", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "displayName"));
						sisobDataTableActor.put("type", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "objectType"));
						sisobDataTableActor.put("subtype", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "IPAdress"));
						sisobDataTableActor.put("groupId", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "groupId"));
						sisobDataTableActor.put("courseId", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "courseId"));
						sisobDataTableActor.put("activityId", getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "activityId"));
						sisobDataTableActor.put("objectClass", "");
						sisobDataTableActor.put("ownerGUID", "");
						sisobDataTableActor.put("content", "");
					}
					((JSONArray)sisobDataTable.get("data")).put(sisobDataTableActor);
					
					//write Object to output Format
					JSONObject sisobDataTableObject = createTemplate(filedataElement);
					sisobDataTableObject.put("label", "object");
					if(filedataElement.has("object")) {
						sisobDataTableObject.put("labelId", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"));
						sisobDataTableObject.put("type", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectType"));
						sisobDataTableObject.put("subtype", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectSubtype"));
						sisobDataTableObject.put("groupId", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "groupId"));
						sisobDataTableObject.put("courseId", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "courseId"));
						sisobDataTableObject.put("objectClass", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectClass"));
						sisobDataTableObject.put("ownerGUID", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "ownerGUID"));
						sisobDataTableObject.put("content", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "content"));
						sisobDataTableObject.put("activityId", getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "activityId"));
					}
					((JSONArray)sisobDataTable.get("data")).put(sisobDataTableObject);
				}
				
				fetchedTupleArray.getJSONObject(y).put("filedata", sisobDataTable);
				
			}
			//System.out.println(sisobDataTable.toString());
			
			//write converted Data to sqlspaces
			Field[] fields = fetchedTuple.getFields();
        	fields[0].setValue(this.getWorkflowID());
        	fields[1].setValue(1);
        	fields[2].setValue(this.getAgentInstanceID() + ".out_1");
        	fields[3].setValue(fetchedTupleArray.toString());
        	Tuple writeTuple = new Tuple();
        	writeTuple.setFields(fields);
        	this.getSisobspace().write(writeTuple);
			
			this.uploadResults();
		} catch (JSONException e) {
			this.indicateError(e.getMessage());
			e.printStackTrace();
		} catch (TupleSpaceException e) {
			this.indicateError(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	private JSONObject createTemplate(JSONObject filedataElement) throws JSONException {
		JSONObject targetDataElement = new JSONObject();
		targetDataElement.put("id", getValueOrEmptyString(filedataElement, "transactionId"));
		targetDataElement.put("verb", getValueOrEmptyString(filedataElement, "verb"));
		targetDataElement.put("published", getValueOrEmptyString(filedataElement, "published"));
		return targetDataElement;
	}
	
	
	private String getValueOrEmptyString(JSONObject obj, String key) throws JSONException {
		if(obj.has(key)) {
			return obj.get(key).toString();
		} else {
			return "";
		}
	}

	@Override
	public void executeAgent(ArrayList<Tuple> fetchedTuples) {
		
	}

	@Override
	protected void uploadResults() {
		this.indicateDone();
		
	}

}
