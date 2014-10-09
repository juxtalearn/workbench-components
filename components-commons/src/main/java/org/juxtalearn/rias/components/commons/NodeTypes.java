package org.juxtalearn.rias.components.commons;


public enum NodeTypes {
    ACTOR("actor", 0, true),
    USER("ClipitUser",0, true),
    FILE("ClipitFile", 1, false),
    BLOG("blog", 2, false),
    COMMENT("ClipitComment", 3, false),
    FIVESTAR("fivestar", 4, false),
    ACTIVITY("ClipitActivity", 5, false),
    GROUP("ClipitGroup", 6, false),
    COURSE("clipit_course", 7, false),
    MESSAGE("ClipitChat", 8, false),
    VIDEO("ClipitVideo",9, false),
    NONE("none", 10, false),
    POST("ClipitPost", 11, false),
    TASK("ClipitTask",12, false),
    STORYBOARD("ClipitStoryboard",13, false),
    MEMBER("member_of_site", 15, false),
    PERFORMANCE("ClipitPerformance",33, false),
    READYET("readYet", 20, false),
    SIMPLETYPE("simpletype", 25, false),
    TRICKYTOPIC("ClipitTrickyTopic",31, false),
    TAG("ClipitTag",32, false),
    TEACHER("teacher",50, true),
    STUDENT("student",51, true),
    UNKNOWN("Unknown",100, false);

    private String typeString;
    private int typeNumber;
    private boolean isUser;

    private NodeTypes(String typeString, int typenumber, boolean isUser) {
        this.typeString = typeString;
        this.typeNumber = typenumber;
        this.isUser = isUser;
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

	public boolean isUser() {

		return isUser;
	}
}