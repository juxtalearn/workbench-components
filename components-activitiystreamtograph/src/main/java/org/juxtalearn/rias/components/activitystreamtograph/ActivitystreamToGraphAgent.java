package org.juxtalearn.rias.components.activitystreamtograph;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import eu.sisob.components.framework.Agent;

public class ActivitystreamToGraphAgent extends Agent {

    public enum NodeTypes {
        ACTOR("actor", 0),
        FILE("file", 1),
        BLOG("blog", 2),
        COMMENT("message", 3),
        RATING("fivestar", 4),
        ACTIVITY("clipit_activity", 5),
        GROUP("clipit_group", 6),
        COURSE("clipit_course", 7),
        NONE("none", 10),
        MEMBER("member_of_site", 15),
        READYET("readYet", 20),
        SIMPLETYPE("simpletype", 25);

        private String typeString;

        private int typeNumber;

        private NodeTypes(String typeString, int typenumber) {
            this.typeString = typeString;
            this.typeNumber = typenumber;
        }

        public String getTypeString() {
            return typeString;
        }

        public int getTypeNumber() {
            return typeNumber;
        }

        public static NodeTypes getEnum(int typeNumber) {
            NodeTypes[] arr = NodeTypes.values();
            for (int i = 0; i < arr.length; i++) {
                if (typeNumber == arr[i].getTypeNumber())
                    return arr[i];
            }
            return null;
        }

        public static NodeTypes getEnum(String typeString) {
            NodeTypes[] arr = NodeTypes.values();
            for (int i = 0; i < arr.length; i++) {
                if (typeString.equals(arr[i].getTypeString()))
                    return arr[i];
            }
            System.err.println("Could not find: " + typeString);
            return null;
        }
    }

    protected class NodeList {

        private TreeMap<String, Node> nodes;

        public NodeList() {
            this.nodes = new TreeMap<String, Node>();
        }

        public Node nodeExists(Node n) {
            return nodes.get(n.getId());

        }

        private void merge(Node current, Node other) {
            if (current.compareTo(other) == 0) {
                JSONArray oClusters = other.getClusters();
                for (int i = 0; i < oClusters.length(); i++) {
                    // a new node has only one groupid or none
                    // if the object isn't in this group add it
                    try {
                        if (!current.inCluster(oClusters.getString(i))) {
                            current.addCluster(oClusters.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                JSONArray oTimeappearance = other.getTimeappearance();

                for (int i = 0; i < oTimeappearance.length(); i++) {
                    // a new node has only one groupid or none
                    // if the object isn't in this group add it
                    try {
                        if (!current.inTimeappearance(oTimeappearance.getString(i))) {
                            current.addTimeappearance(oTimeappearance.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                
                current.weight = current.weight>other.weight?current.weight:other.weight;

            }
        }

        public void addElement(Node n) {
            try {
                Node existing = nodes.get(n.getId());
                // if node not exists add it to
                if (existing == null) {
                    this.nodes.put(n.getId(), n);
                } else {
                    merge(existing, n);

                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        public JSONArray toJSONArray() {
            JSONArray returnArray = new JSONArray();
            for (Node n : nodes.values()) {
                returnArray.put(n.toJSONString());
            }
            ;

            return returnArray;
        }

        public int size() {
            return this.nodes.size();
        }
    }

    protected class Node implements Comparable<Node> {

        private String id;

        private String label;

        private JSONArray timeappearance;

       // private String type;
        private NodeTypes type;
        
            
        private JSONArray clusters;
        
        private int weight = 0;
        
        private String content="";
        

        /**
         * Constructor
         * 
         * @param id
         * @param label
         * @param timeappearance
         * @param type
         * @param cluster
         *            Cluster stands for Group
         */
        public Node(String id, String label, String timeappearance, NodeTypes type, String cluster) {
            this.id = id;
            this.label = label;
            this.timeappearance = new JSONArray();
            this.timeappearance.put(timeappearance);
            this.type = type;
            this.clusters = new JSONArray();
            if (cluster != "") {
                this.clusters.put(this.type.getTypeNumber());
            }

        }
        public Node(String id, String label, String timeappearance, NodeTypes type, String cluster, JSONObject object) {
            this(id,label,timeappearance,type,cluster);
            switch(type) {
                case FIVESTAR:
                    try {
                        this.weight = object.getInt("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    default:
                    try {
                        this.content = object.getString("content");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.content="no content";
                    }
                        
            }
        }
        
        
        
        

        public JSONObject toJSONString() {
            JSONObject returnJSON = new JSONObject();
            try {
                returnJSON.put("id", this.id);
                returnJSON.put("label", this.label);
                returnJSON.put("type", this.type.getTypeNumber());
                returnJSON.put("clusters", ((JSONArray) new JSONArray()).put(this.clusters));
                returnJSON.put("content",this.content);
                returnJSON.put("timeappearance", timeappearance);// ((JSONArray) new JSONArray()).put(this.timeappearance + "-" + Long.MAX_VALUE));
                returnJSON.put("weight", Integer.toString(this.weight));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return returnJSON;
        }

        public String getId() {
            return this.id;
        }

        public String getLabel() {
            return this.label;
        }

        public JSONArray getTimeappearance() {
            return this.timeappearance;
        }

        public JSONArray getClusters() {
            return this.clusters;
        }

        public NodeTypes getType() {
            return this.type;
        }

        protected boolean inCollection(String needle, JSONArray haystack) {
            try {
                for (int i = 0; i < haystack.length(); i++) {

                    if (haystack.getString(i).equals(needle)) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        // Check if node exists in cluster / group
        public boolean inCluster(String cluster) {
            return inCollection(cluster, this.clusters);
        }

        // Check if node exists in cluster / group
        public boolean inTimeappearance(String timestamp) {
            return inCollection(timestamp, this.timeappearance);
        }

        // add node to cluster / group
        public void addCluster(String cluster) {
            if ("1".equals(cluster)) {
                System.out.println("cluster nicht berücksichtigt");
            } else {
                this.clusters.put(cluster);
            }

        }

        public void addTimeappearance(String timestamp) {
            this.timeappearance.put(timestamp);
        }

        @Override
        public int compareTo(Node other) {
            return this.id.compareTo(other.id);
        }
    }

    protected class Edge {

        private String id,
                label,
                source,
                target,
                timeappearance;

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

        public String getTimeappearance() {
            return timeappearance;
        }

        public void addSameEdge(Edge e) {
            if (this.equals(e)) {
                weight++;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Edge) {
                Edge e = (Edge) obj;
                return this.label.equals(e.getLabel()) && this.source.equals(e.getSource()) && this.target.equals(e.getTarget()) && this.timeappearance.equals(e.getTimeappearance());
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

    protected class EdgeList {

        private ArrayList<Edge> edges;

        public EdgeList() {
            edges = new ArrayList<Edge>();
        }

        public void addEdge(Edge e) {
            boolean exists = false;
            for (int i = 0; i < edges.size(); i++) {
                if (edges.get(i).equals(e)) {
                    exists = true;
                    edges.get(i).addSameEdge(e);
                }
            }
            if (exists == false) {
                edges.add(e);
            }
        }

        public JSONArray toJSONArray() {
            JSONArray result = new JSONArray();
            for (Edge e : edges) {
                result.put(e.toJSONObject());
            }
            return result;
        }
    }

    public ActivitystreamToGraphAgent(Tuple commandTuple, String serverlocation, int port) {
        super(commandTuple, serverlocation, port);

        this.commandTupleStructure = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
    }

    private JSONObject createMetadata(JSONObject fetchedTupleData, int mode) throws JSONException {

        // metadata SISOB Graph Format
        JSONObject metadata = new JSONObject();

        metadata.put("title", getValueOrEmptyString(fetchedTupleData, "filename"));
        metadata.put("directed", "true");
        metadata.put("type", mode + " mode network");
        metadata.put("datalinks", ((JSONArray) new JSONArray()).put("0.json"));
        
        JSONArray measures = new JSONArray();
        //edge measures
        JSONObject weightMeasure = new JSONObject();
        weightMeasure.put("title", "weight");
        weightMeasure.put("description", "weight describes the occurrences of this edge in the activitystream");
        weightMeasure.put("class", "edge");
        weightMeasure.put("property", "weight");
        weightMeasure.put("type", "integer");
        measures.put(weightMeasure);
        //node measures
        weightMeasure = new JSONObject();
        weightMeasure.put("title", "weight");
        weightMeasure.put("description", "weight of node e.g. fivestar ratings");
        weightMeasure.put("class", "node");
        weightMeasure.put("property", "weight");
        weightMeasure.put("type", "integer");
        measures.put(weightMeasure);
        
        
        metadata.put("measures", measures);
        JSONArray nodeprop = new JSONArray();
        nodeprop.put(new JSONObject().put("clusters", "array"));
        nodeprop.put(new JSONObject().put("content","string"));
        metadata.put("nodeproperties", nodeprop);

        return metadata;
    }
    protected JSONArray activityStreamArray;
    @Override
    public void executeAgent(Tuple fetchedTuple) {
        this.indicateRunning();
        
        try {
            activityStreamArray = new JSONArray(fetchedTuple.getField(3).getValue().toString());
            for (int y = 0; y < activityStreamArray.length(); y++) {
                HashSet<NodeTypes> networkmodes = new HashSet<NodeTypes>();

                // filename, filetype, specialfiletype, filedata
                JSONObject fetchedTupleData = activityStreamArray.getJSONObject(y);
                fetchedTupleData.put("filetype", "sgf");
                JSONObject sisobGraphFormat = new JSONObject();
                sisobGraphFormat.put("data", new JSONObject());

                // JSONArray edges = new JSONArray();
                EdgeList edges = new EdgeList();
                // there can be collisions between actor id and object id, so there are actor nodes and object nodes, at the end they are merged
                // Edit by Oliver: This is wrong, but separate lists will be kept for separation reasons
                NodeList actorNodes = new NodeList();
                NodeList objectNodes = new NodeList();

                JSONArray filedata = new JSONArray(fetchedTupleData.get("filedata").toString());
                for (int i = 0; i < filedata.length(); i++) {
                    String s = filedata.getString(i);
                   
                    
                    
                    JSONObject filedataElement = new JSONObject(s);

                    // filter join and leave verbs after having checked that the action is well-formed
                    if (filedataElement.has("verb") && !filedataElement.get("verb").toString().equals("join") && !filedataElement.get("verb").toString().equals("leave") && filedataElement.has("actor") && filedataElement.has("object")) {
                        // We can always create the actor node,
                        // but the object nodes and edges will be different depending on the activity being an annotation or not
                        JSONObject actor = filedataElement.getJSONObject("actor");
                        String actorId = getValueOrEmptyString(actor, "actorId");
                        actorNodes.addElement(new Node(actorId, getValueOrEmptyString(actor, "displayName"), "", NodeTypes.ACTOR, getValueOrEmptyString(actor, "groupId")));
                        // The Object "worked" on
                        // If Verb is annotate we need to create the ao Edge to the targetId, else it will be made to the objectId
                        // annotate is add of fivestar raking, comment or like
                        JSONObject object = filedataElement.getJSONObject("object");

                        String published = getValueOrEmptyString(filedataElement, "published");
                        String transactionId = getValueOrEmptyString(filedataElement, "transactionId");
                        String verb = filedataElement.get("verb").toString();
                        String objectId = getValueOrEmptyString(object, "objectId");
                        NodeTypes objectSubType = NodeTypes.getEnum(getValueOrEmptyString(object, "objectSubtype"));
                        String groupId = getValueOrEmptyString(object, "groupId");
                        networkmodes.add(objectSubType);

                        if ("annotate".equals(verb)) {
                            String targetId = getValueOrEmptyString(object, "targetId");
                            NodeTypes targetType = NodeTypes.getEnum(getValueOrEmptyString(object, "targetSubtype"));
                            objectNodes.addElement(new Node("a_" + objectId, objectSubType.getTypeString(), published, objectSubType, groupId,object));

                            objectNodes.addElement(new Node(targetId, getValueOrEmptyString(object, "targetName"), "", targetType, groupId,object));
                            // this add an edge between the Actor and the new object (fivestar, comment or like)

                            edges.addEdge(new Edge("ac_" + transactionId, "create", actorId, "a_" + objectId, published));
                            // this add an edge between the Actor and the annotated object
                            edges.addEdge(new Edge("ao_" + transactionId, verb, actorId, targetId, published));
                            // this add an edge between the annotation(fivestar, comment or like) and the Object worked on
                            edges.addEdge(new Edge("co_" + transactionId, verb, "a_" + objectId, targetId, published));
                        } else {
                            objectNodes.addElement(new Node(objectId, getValueOrEmptyString(object, "displayName"), published, objectSubType, groupId, object));
                            edges.addEdge(new Edge("ao_" + transactionId, verb, actorId, objectId, published));
                        }
                    }
                }
                sisobGraphFormat.put("metadata", createMetadata(fetchedTupleData, networkmodes.size() + 1));
                // add nodes and edges to data
                ((JSONObject) sisobGraphFormat.get("data")).put("edges", edges.toJSONArray());
                ((JSONObject) sisobGraphFormat.get("data")).put("nodes", concatArray(actorNodes.toJSONArray(), objectNodes.toJSONArray()));
                activityStreamArray.getJSONObject(y).put("filedata", sisobGraphFormat.toString());
            }

        } catch (JSONException e) {
            
            this.indicateError(e.getMessage(), e);
        } catch (Exception e) {
            this.indicateError("Error:", e);
        }
        this.uploadResults();
        indicateDone();
    }

    private String getValueOrEmptyString(JSONObject obj, String key) throws JSONException {
        if (obj.has(key))
            return obj.get(key).toString();
        return "";
    }

    private JSONArray concatArray(JSONArray... arrs) throws JSONException {
        JSONArray result = new JSONArray();
        for (JSONArray arr : arrs) {
            for (int i = 0; i < arr.length(); i++) {
                result.put(arr.get(i));
            }
        }
        return result;
    }

    @Override
    public void executeAgent(ArrayList<Tuple> fetchedTuples) {
            //doing nothing on purpose!
    }

    @Override
    protected void uploadResults() {
        Tuple resultingDataTuple = new Tuple(this.getWorkflowID(), 1,
                this.getAgentInstanceID() + ".out_1",
                activityStreamArray.toString(), "");

        try {
            this.getSisobspace().write(resultingDataTuple);
        } catch (TupleSpaceException e) {
            indicateError("Couldn't upload results", e);
        }
        
    }

}
