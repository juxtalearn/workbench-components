package org.juxtalearn.rias.components.commons;


public enum NodeTypes {
    ACTOR("actor", 0),
    FILE("file", 1),
    BLOG("blog", 2),
    COMMENT("clipit_post", 3),
    FIVESTAR("fivestar", 4),
    ACTIVITY("clipit_activity", 5),
    GROUP("clipit_group", 6),
    COURSE("clipit_course", 7),
    MESSAGE("message", 8),
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