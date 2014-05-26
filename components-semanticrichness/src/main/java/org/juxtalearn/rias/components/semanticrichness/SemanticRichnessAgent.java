package org.juxtalearn.rias.components.semanticrichness;

import info.collide.sqlspaces.commons.Field;
import info.collide.sqlspaces.commons.Tuple;
import info.collide.sqlspaces.commons.TupleSpaceException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.juxtalearn.rias.components.commons.NodeTypes;

import eu.sisob.components.framework.Agent;
import eu.sisob.components.framework.graph.SomeNode.NODETYPE;

public class SemanticRichnessAgent extends Agent {

    public static final String PROPERTY_TITLE = "SemanticRichness";

    public static final String META_THESAURUS_FILE = "meta_thesaurus.csv";

    public static final String THESAURUS_FILE = "thesaurus.csv";

    private LinkedHashMap<String, String> metaThes;

    private LinkedHashMap<String, String> thes;

    private List<String> domainConcepts;
    private List<String> generalConcepts;

    public SemanticRichnessAgent(Tuple commandTuple, String serverlocation, int port) {
        super(commandTuple, serverlocation, port);
        this.commandTupleStructure = commandTuple;
        Tuple dataTemplate = new Tuple(commandTuple.getField(0).getValue().toString(), 1, commandTuple.getField(5).getValue().toString(), Field.createWildCardField());
        setDataTupleStructure(dataTemplate);
        metaThes = loadThesaurus(META_THESAURUS_FILE);
        thes = loadThesaurus(THESAURUS_FILE);
        domainConcepts = getConceptsFromThesaurus(metaThes, "domain_concept");
        generalConcepts = getConceptsFromThesaurus(metaThes, "general_concept");
        generalConcepts.addAll(getConceptsFromThesaurus(metaThes, "signal_concept"));
        
    }

    @Override
    public void executeAgent(Tuple fetchedTuple) {
        this.indicateRunning();
        JSONArray fetchedTupleArray;

        try {
            fetchedTupleArray = new JSONArray(fetchedTuple.getField(3).getValue().toString());
            for (int y = 0; y < fetchedTupleArray.length(); y++) {
                // filename, filetype, specialfiletype, filedata
                JSONObject fetchedTupleData = fetchedTupleArray.getJSONObject(y);
                fetchedTupleData.put("filetype", "sgf");
                JSONArray activityStream = new JSONArray(fetchedTupleData.get("filedata").toString());
                JSONObject object;
                String content;
                Float semRicValue = 0f;

                for (int i = 0; i < activityStream.length(); i++) {
                    JSONObject activity = new JSONObject (activityStream.get(i).toString());
                    System.out.println(activity);
                    if (activity.has("actor") && activity.has("object") && activity.has("verb")) {
                        if (activity.get("verb").equals("annotate")) {
                            // If we have an annotation and the annotation
                            // is of type comment, we can go ahead and
                            // evaluate the content:
                            object = activity.getJSONObject("object");
                            if (getValueOrEmptyString(object, "objectSubtype").equals(NodeTypes.COMMENT.getTypeString())) {
                                content = getValueOrEmptyString(object, "content");
                                content = applyThesaurus(content, thes);
                                semRicValue = evaluateSemanticRichness(content, true);
                                if (!object.has("Properties")) {
                                    object.put("Properties", new JSONObject());
                                }
                                JSONObject properties = object.getJSONObject("Properties");
                                properties.put(PROPERTY_TITLE, semRicValue);
                                System.out.println("Semantic Richness: " + semRicValue);
                            }
                        }
                    }
                    activityStream.put(i, activity);
                }
                fetchedTupleData.put("filedata", activityStream.toString());
            }
            // write converted Data to sqlspaces
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

    private LinkedHashMap<String, String> loadThesaurus(String filename) {
        String line = "";
        String thesaurus_term = "";
        String term = "";
        LinkedHashMap<String, String> tempThesaurus = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> thesaurus = new LinkedHashMap<String, String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
            line = br.readLine();
            while (line != null) {
                if (!line.equals("")) {
                    term = line.substring(0, line.indexOf(","));
                    thesaurus_term = line.substring(line.indexOf(",") + 1);
                    tempThesaurus.put(String.valueOf(term), String.valueOf(thesaurus_term));
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> thesaurusTerms = new ArrayList<String>(tempThesaurus.keySet());
        Collections.sort(thesaurusTerms, new LengthSorter());
        for (String sortTerm : thesaurusTerms) {
            thesaurus.put(sortTerm, tempThesaurus.get(sortTerm));
        }
        return thesaurus;
    }

    private Float evaluateSemanticRichness(String comment, boolean doubles) {
        Float richness = 0f;
        Float amountDomain = 0f;
        Float amountGeneral = 0f;
        for (String concept : domainConcepts) {
            amountDomain = amountDomain + countOccurrences(comment, concept, doubles);
        }
        for (String concept : generalConcepts) {
            amountGeneral = amountGeneral + countOccurrences(comment, concept, doubles);
        }
        richness = (Float) (amountDomain / (amountGeneral + amountDomain));
        return richness;
    }

    private int countOccurrences(String comment, String concept, boolean doubles) {
        int lastIndex = 0;
        int count = 0;
        if (doubles) {
            while (lastIndex != -1) {
                lastIndex = comment.indexOf(concept, lastIndex);
                if (lastIndex != -1) {
                    count++;
                    lastIndex += concept.length();
                }
            }
        } else {
            if (comment.matches(".*(?!_)concept(?!_).*")) {
                count = 1;
            }
        }
        return count;
    }

    private List<String> getConceptsFromThesaurus(LinkedHashMap<String, String> metaThesaurus, String type) {
        List<String> concepts = new ArrayList<String>();
        String value;
        for (String key : metaThesaurus.keySet()) {
            value = metaThesaurus.get(key);
            if ((type.equals("") || type.equals(value)) && !concepts.contains(key)) {
                concepts.add(key);
            }
        }

        return concepts;
    }

    private static String applyThesaurus(String content, LinkedHashMap<String, String> thesaurus) {
        for (String term : thesaurus.keySet()) {
            if (content.contains(term)) {
                content = content.replaceAll(term, " " + thesaurus.get(term) + " ");
            }
        }
        // content = content.trim();
        return content;
    }

    private String getValueOrEmptyString(JSONObject obj, String key) throws JSONException {
        if (obj.has(key))
            return obj.get(key).toString();
        return "";
    }

    @Override
    public void executeAgent(ArrayList<Tuple> fetchedTuples) {

    }

    @Override
    protected void uploadResults() {
        this.indicateDone();

    }

    class LengthSorter implements Comparator<String> {

        @Override
        public int compare(String s1, String s2) {
            if (s1.length() > s2.length()) {
                return -1;
            }
            if (s1.length() == s2.length()) {
                return 0;
            } else {
                return 1;
            }
        }
    }

}
