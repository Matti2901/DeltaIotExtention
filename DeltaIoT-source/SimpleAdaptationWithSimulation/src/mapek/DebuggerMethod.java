package mapek;

import java.util.ArrayList;

import deltaiot.services.Link;
import deltaiot.services.Mote;

public class DebuggerMethod {

    public static void debugPrintLinks(ArrayList<Mote> motes,String when) {
        System.out.println("📡 "+when+"link state with distributions:");
        for (Mote mote : motes) {
            System.out.print("Mote " + mote.getMoteid() + " → ");
            for (Link link : mote.getLinks()) {
                System.out.print(link.getDest() + "(dist=" + link.getDistribution() + "%)  ");
            }
            System.out.println();
        }
    }
}
