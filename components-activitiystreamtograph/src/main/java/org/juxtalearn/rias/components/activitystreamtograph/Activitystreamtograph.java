package org.juxtalearn.rias.components.activitystreamtograph;

import info.collide.sqlspaces.commons.Tuple;

import java.io.BufferedReader;
import java.io.FileReader;

import eu.sisob.components.framework.AgentManager;

public class Activitystreamtograph {

    public static void main(String args[]) {
        String serverdata[] = loadServerData();
        String serverlocation = serverdata[0];
        int port = Integer.parseInt(serverdata[1]);
        executeFileLoader(serverlocation, port);
    }

    public static void executeFileLoader(String serverlocation, int port) {
        AgentManager flm = new ActivitystreamToGraphManager(new Tuple(String.class, Integer.class, Integer.class, String.class, "Activitystream To Graph Converter", String.class, String.class), "Activitystream To Graph Converter Manager", serverlocation, port);
        flm.initialize();
        Thread runtime = new Thread(flm);
        runtime.start();

    }

    public static String[] loadServerData() {
        String serverdata[] = new String[2];
        try {
            BufferedReader reader = new BufferedReader(new FileReader("server.conf"));
            // server location
            serverdata[0] = reader.readLine();
            // port number
            serverdata[1] = reader.readLine();

            reader.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return serverdata;
    }
}