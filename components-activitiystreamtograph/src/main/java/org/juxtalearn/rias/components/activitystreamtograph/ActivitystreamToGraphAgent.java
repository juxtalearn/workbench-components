package org.juxtalearn.rias.components.activitystreamtograph;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mysql.jdbc.NotImplemented;

import eu.sisob.components.framework.Agent;



class Node {
	public enum NodeTypes{
		ACTOR("actor",0),
		FILE("file",1),
		BLOG("blog",2),
		COMMENT("generic_comment",3),
		FIVESTAR("fivestar",4);
		private String typeString;
		private int typeNumber;
		private NodeTypes(String typeString, int typenumber){
			this.typeString = typeString;
			this.typeNumber = typenumber;
		}
		public String getTypeString(){
			return typeString;
		}
		public int getTypeNumber(){
			return typeNumber;
		}
		public static NodeTypes getEnum(int typeNumber) {
			NodeTypes[] arr = NodeTypes.values();
			for (int i=0; i<arr.length; i++){
				if (typeNumber==arr[i].getTypeNumber()) 
					return arr[i];
			}
			return null;
		}
		public static NodeTypes getEnum(String typeString) {
			NodeTypes[] arr = NodeTypes.values();
			for (int i=0; i<arr.length; i++){
				if (typeString.equals(arr[i].getTypeString())) 
					return arr[i];
			}
			System.err.println("Could not find: "+typeString);
			return null;
		}
	}
	private String id;
	private String label;
	private String timeappearance;
	private String type;
	private JSONArray clusters;
	
	/**
	 * Constructor
	 * @param id
	 * @param label
	 * @param timeappearance
	 * @param type
	 * @param cluster Cluster stands for Group
	 */
	public Node(String id, String label, String timeappearance, String type, String cluster) {
		this.id = id;
		this.label = label;
		this.timeappearance = timeappearance;
		this.type = type;
		this.clusters = new JSONArray();
		if(cluster != "") {
			this.clusters.put(NodeTypes.getEnum(this.type).getTypeNumber());			
		}
		
	}

	public JSONObject toJSONString() {
		JSONObject returnJSON = new JSONObject();
		try {
			returnJSON.put("id", this.id);
			returnJSON.put("label", this.label);
			returnJSON.put("type", NodeTypes.getEnum(this.type).getTypeNumber());
			returnJSON.put("clusters", ((JSONArray)new JSONArray()).put(this.clusters));
			returnJSON.put("timeappearance", ((JSONArray)new JSONArray()).put(this.timeappearance+"-"+Long.MAX_VALUE));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return returnJSON;
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public String getTimeappearance() {
		return this.timeappearance;
	}
	
	public JSONArray getClusters() {
		return this.clusters;
	}
	
	public String getType() {
		return this.type;
	}
	
	//Check if node exists in cluster / group
	public boolean inCluster(int cluster) {
		try {
			for(int i = 0; i < this.clusters.length(); i++) {
				if(Integer.parseInt(this.clusters.get(i).toString()) == cluster) {
					return true;
				}
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	//add node to cluster / group
	public void addCluster(int cluster) {
		if(cluster == 1) {
			System.out.println("cluster nicht berücksichtigt");
		} else {
			this.clusters.put(cluster);
		}
		
	}
}

class NodeList {
	private ArrayList<Node> nodes;
	
	public NodeList() {
		this.nodes = new ArrayList<Node>();
	}
	
	public Node nodeExists(Node n) {
		for(int i = 0; i < this.nodes.size(); i++) {
			if(this.nodes.get(i).getId().equals(n.getId()))
				return this.nodes.get(i);
		}
		return null;
	}
	
	public void addElement(Node n) {
		try {
			//if node exists in existing is the object
			Node existing = this.nodeExists(n);
			//if node not exists add it to 
			if(existing == null) {
				this.nodes.add(n);
			} else {
				if (n.getClusters().length() == 1) {
					// a new node has only one groupid or none
					// if the object isn't in this group add it
					if(!existing.inCluster(Integer.parseInt(n.getClusters().get(0).toString()))) {
						existing.addCluster(Integer.parseInt(n.getClusters().get(0).toString()));
					}					
				}
				
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public JSONArray toJSONArray() {
		JSONArray returnArray = new JSONArray();
		for(int i = 0; i < this.nodes.size(); i++) {
			returnArray.put(this.nodes.get(i).toJSONString());
		}
		return returnArray;
	}
	
	public int size() {
		return this.nodes.size();
	}
}

class Edge {
	private String id, label, source, target, timeappearance;
	private int weight;
	
	
	
	public Edge(String id, String label, String source, String target, String timeappearance) {
		this.id = id;
		this.label = label;
		this.source = source;
		this.target = target;
		this.weight = 1;
		this.timeappearance = timeappearance;
	}
	
	public String getLabel() {
		return label;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public void addSameEdge(Edge e) {
		if(this.equals(e)) {
			weight++;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Edge) {
			Edge e = (Edge)obj;
			return this.label.equals(e.getLabel()) && this.source.equals(e.getSource()) && this.target.equals(e.getTarget());
		} else {
			throw new java.lang.UnsupportedOperationException();
		}
	}
	
	public JSONObject toJSONObject() {
		JSONObject result = new JSONObject();
		try {
			
			result.put("id", this.id);
			result.put("source", this.source);
			result.put("target", this.target);
			result.put("label", this.label);
			result.put("weight", Integer.toString(this.weight));
			result.put("timeappearance", ((JSONArray) new JSONArray()).put(timeappearance));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}
	
}

class EdgeList {
	private ArrayList<Edge> edges;
	
	public EdgeList() {
		edges = new ArrayList<Edge>();
	}
	
	public void addEdge(Edge e) {
		boolean exists = false;
		for(int i = 0; i < edges.size(); i++) {
			if(edges.get(i).equals(e)) {
				exists = true;
				edges.get(i).addSameEdge(e);
			}
		}
		if(exists == false) {
			edges.add(e);
		}
	}
	
	public JSONArray toJSONArray() {
		JSONArray result = new JSONArray();
		for (int i = 0; i < edges.size(); i++) {
			result.put(edges.get(i).toJSONObject());
		}
		return result;
	}
}

public class ActivitystreamToGraphAgent extends Agent{

	public ActivitystreamToGraphAgent(Tuple commandTuple,
			String serverlocation, int port) {
		super(commandTuple, serverlocation, port);
		
		this.commandTupleStructure = commandTuple;
		Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
	}

	@Override
	public void executeAgent(Tuple fetchedTuple) {
		this.indicateRunning();
		
		System.out.println("läuft");
		JSONArray fetchedTupleArray;
		
		try {
 			fetchedTupleArray = new JSONArray(fetchedTuple.getField(3).getValue().toString());
			for(int y = 0; y < fetchedTupleArray.length(); y++) {
				//filename, filetype, specialfiletype, filedata
				JSONObject fetchedTupleData = fetchedTupleArray.getJSONObject(y);
				((JSONObject)fetchedTupleArray.getJSONObject(y)).put("filetype", "sgf");
				
				JSONArray filedata = new JSONArray(fetchedTupleData.get("filedata").toString());
				
				JSONObject sisobGraphFormat = new JSONObject();
				//metadata SISOB Graph Format
				sisobGraphFormat.put("metadata", new JSONObject());
				((JSONObject)sisobGraphFormat.get("metadata")).put("title", getValueOrEmptyString(fetchedTupleData, "filename"));
				((JSONObject)sisobGraphFormat.get("metadata")).put("directed", "true");
				((JSONObject)sisobGraphFormat.get("metadata")).put("type", "6 mode network");
				((JSONObject)sisobGraphFormat.get("metadata")).put("datalinks", ((JSONArray)new JSONArray()).put("0.json"));
				JSONObject weightMeasure = new JSONObject();
				weightMeasure.put("title", "weight");
				weightMeasure.put("description", "weight describes the occurrences of this edge in the activitystream");
				weightMeasure.put("class", "edge");
				weightMeasure.put("property", "weight");
				weightMeasure.put("type", "integer");
				((JSONObject)sisobGraphFormat.get("metadata")).put("measures", ((JSONArray)new JSONArray()).put(weightMeasure));
				
				JSONArray nodeprop = new JSONArray();
				nodeprop.put(new JSONObject().put("clusters", "array"));
				((JSONObject)sisobGraphFormat.get("metadata")).put("nodeproperties", nodeprop);
				
				
				sisobGraphFormat.put("data", new JSONObject());
				
				//JSONArray edges = new JSONArray();
				EdgeList edges = new EdgeList();
				//there can be collisions between actor id and object id, so there are actor nodes and object nodes, at the end they are merged
				//Edit by Oliver: This is wrong, but separate lists will be kept for separation reasons
				NodeList actorNodes = new NodeList();
				NodeList objectNodes = new NodeList();
				int z = 0;
				for (int i = 0; i < filedata.length(); i++) {
					JSONObject filedataElement = new JSONObject(filedata.get(i).toString()); 
							//(JSONObject) filedata.get(i);
					//filter join and leave verbs
					if(!filedataElement.get("verb").toString().equals("join") && !filedataElement.get("verb").toString().equals("leave")) {
						if(filedataElement.has("actor") && filedataElement.has("object") && filedataElement.has("verb")) {
							//We can always create the actor node, 
							//but the object nodes and edges will be different depending on the activity beeing an annotation or not							
							actorNodes.addElement(new Node(
									getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "actorId"),
									getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "displayName"),
									"",
									"actor",
									getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "groupId")
								)
							);
							//The Object "worked" on
							//If Verb is annotate we need to create the ao Edge to the targetId, else it will be made to the objectId
							//annotate is add of fivestar raking, comment or like
							if(filedataElement.get("verb").equals("annotate")) {
								//This is the annotation node, due to elggs id system, we need to add prefix a_ for annotations
//								if (getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectSubtype").equals("null")) {
//									System.out.println("Subtype missing: " + getValueOrEmptyString(filedataElement, "transactionId"));
//									System.out.println(((JSONObject)filedataElement).toString());
//								}
								objectNodes.addElement(new Node(
										"a_" + getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectSubtype"),
										getValueOrEmptyString(filedataElement, "published"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectSubtype"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "groupId")
									));
								//This is the object node
								//targetID is the id of the annotated object, so we need to create this object if it doesnt already exist
								//This can only happen in incomplete activitystreams! Otherwise the object will be created beforehand
//								if (getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetName").equals("null")) {
//									System.out.println("targetname missing: " + getValueOrEmptyString(filedataElement, "transactionId"));
//									System.out.println(((JSONObject)filedataElement).toString());
//								}
								
								objectNodes.addElement(new Node(
									getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetId"),
									getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetName"),
									"",
									getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetSubtype"),
									getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "groupId")
								));
								//this add an edge between the Actor and the new object (fivestar, comment or like)
								edges.addEdge(new Edge(
										"ac_" + getValueOrEmptyString(filedataElement, "transactionId"),
										"create",
										getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "actorId"),
										"a_" + getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"),
										getValueOrEmptyString(filedataElement, "published")
								));
								//this add an edge between the Actor and the annotated object
								edges.addEdge(new Edge(
										"ao_" + getValueOrEmptyString(filedataElement, "transactionId"),
										filedataElement.get("verb").toString(),
										getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "actorId"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetId"),
										getValueOrEmptyString(filedataElement, "published")
								));
								//this add an edge between the annotation(fivestar, comment or like) and the Object worked on
								edges.addEdge(new Edge(
										"co_" + getValueOrEmptyString(filedataElement, "transactionId"),
										filedataElement.get("verb").toString(),
										"a_" + getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "targetId"),
										getValueOrEmptyString(filedataElement, "published")
								));
							}
							else {
//								if (getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "displayName").equals("null")) {
//									System.out.println("displayName missing: " + getValueOrEmptyString(filedataElement, "transactionId"));
//									System.out.println(((JSONObject)filedataElement).toString());
//								}
								objectNodes.addElement(new Node(
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "displayName"),
										getValueOrEmptyString(filedataElement, "published"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectSubtype"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "groupId")
									));
								edges.addEdge(new Edge(
										"ao_" + getValueOrEmptyString(filedataElement, "transactionId"),
										filedataElement.get("verb").toString(),
										getValueOrEmptyString(((JSONObject)filedataElement.get("actor")), "actorId"),
										getValueOrEmptyString(((JSONObject)filedataElement.get("object")), "objectId"),
										getValueOrEmptyString(filedataElement, "published")
									)
								);

							}
						}
					}
					z++;
				}
				System.out.println(z);
				
				
				//add nodes and edges to data
				((JSONObject)sisobGraphFormat.get("data")).put("edges", edges.toJSONArray());
				((JSONObject)sisobGraphFormat.get("data")).put("nodes", concatArray(actorNodes.toJSONArray(), objectNodes.toJSONArray()));
				fetchedTupleArray.getJSONObject(y).put("filedata", sisobGraphFormat.toString());
			}
			
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
			this.indicateError(e.getMessage(), e);
		} catch (TupleSpaceException e) {
			this.indicateError(e.getMessage(), e);
		} catch (Exception e) {
			this.indicateError("Error:", e);
		}
		
		
	}
	
	private String getValueOrEmptyString(JSONObject obj, String key) throws JSONException {
		if(obj.has(key))
			return obj.get(key).toString();
		return "";
	}
	
	private JSONArray concatArray(JSONArray... arrs) throws JSONException {
		JSONArray result = new JSONArray();
		for(JSONArray arr : arrs) {
			for(int i = 0; i < arr.length(); i++) {
				result.put(arr.get(i));
			}
		}
		return result;
	}

	@Override
	public void executeAgent(ArrayList<Tuple> fetchedTuples) {
		
		
	}

	@Override
	protected void uploadResults() {
		this.indicateDone();
		
	}

}
