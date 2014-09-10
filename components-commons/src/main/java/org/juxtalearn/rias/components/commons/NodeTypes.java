package org.juxtalearn.rias.components.commons;


public enum NodeTypes {
    ACTOR("actor", 0),
    USER("ClipitUser",0),
    FILE("ClipitFile", 1),
    BLOG("blog", 2),
    COMMENT("ClipitPost", 3),
    FIVESTAR("fivestar", 4),
    ACTIVITY("ClipitActivity", 5),
    GROUP("ClipitGroup", 6),
    COURSE("clipit_course", 7),
    MESSAGE("message", 8),
    VIDEO("ClipitVideo",9),
    NONE("none", 10),
    TASK("ClipitTask",12),
    STORYBOARD("ClipitStoryboard",13),
    MEMBER("member_of_site", 15),
    READYET("readYet", 20),
    SIMPLETYPE("simpletype", 25),
    TRICKYTOPIC("ClipitTrickyTopic",31),
    TAG("ClipitTag",32),
    TEACHER("teacher",50),
    STUDENT("student",51),
    UNKNOWN("Unknown",100);

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
                return getEnum(arr[i].getTypeNumber());
        }
        System.err.println("Could not find: " + typeString);
        return UNKNOWN;
    }
}