package org.juxtalearn.rias.components.commons;

public enum NodeTypes {
    ACTOR("actor", 0, true,false),
    USER("ClipitUser", 0, true,false),
    FILE("ClipitFile", 1, false,true),
    BLOG("blog", 2, false,false),
    COMMENT("ClipitComment", 3, false,true),
    FIVESTAR("fivestar", 4, false,false),
    ACTIVITY("ClipitActivity", 5, false,false),
    GROUP("ClipitGroup", 6, false, false),
    COURSE("clipit_course", 7, false,false),
    MESSAGE("ClipitChat", 8, false,true),
    VIDEO("ClipitVideo", 9, false,true),
    NONE("none", 10, false,false),
    POST("ClipitPost", 11, false,true),
    TASK("ClipitTask", 12, false, false),
    STORYBOARD("ClipitStoryboard", 13, false,true),
    MEMBER("member_of_site", 15, false,false),
    PERFORMANCE("ClipitPerformance", 33, false,false),
    READYET("readYet", 20, false,false),
    SIMPLETYPE("simpletype", 25, false,false),
    TRICKYTOPIC("ClipitTrickyTopic", 31, false,false),
    TAG("ClipitTag", 32, false,true),
    TEACHER("teacher", 50, true,false),
    STUDENT("student", 51, true,false),
    UNKNOWN("Unknown", 100, false,false);

    private String typeString;

    private int typeNumber;

    private boolean isUser;

    private boolean useAsMeasure;

    private NodeTypes(String typeString, int typenumber, boolean isUser, boolean useAsMeasure) {
        this.typeString = typeString;
        this.typeNumber = typenumber;
        this.isUser = isUser;
        this.useAsMeasure = useAsMeasure;
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

    public boolean isUseAsMeasure() {
        return useAsMeasure;
    }

}