package com.darryncampbell.wifi_rtt_trilateration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Configuration {

    public static final int NUM_HISTORICAL_POINTS = 10;
    public static final int MILLISECONDS_BETWEEN_RANGING_REQUESTS = 0; //  1000

    public enum CONFIGURATION_TYPE {
        THREE_DIMENSIONAL_1,
        TWO_DIMENSIONAL_1,   //  2D will just set all the 3rd dimensions to 0.0
        TWO_DIMENSIONAL_2,
        TESTING,
        TESTING_2,
        TESTING_3
    }

    AccessPoint ap1;
    AccessPoint ap2;
    AccessPoint ap3;
    AccessPoint ap4;
    ArrayList<AccessPoint> accessPoints;
    ArrayList<String> macAddresses;

    public Configuration(CONFIGURATION_TYPE configuration_type)
    {
        if (configuration_type == CONFIGURATION_TYPE.THREE_DIMENSIONAL_1)
        {
            //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 100.0, 0.0, "Lounge");
            ap2 = new AccessPoint("58:cb:52:a9:b3:cd", 2980.0, 6900.0, 800.0, "Dining Room");
            ap3 = new AccessPoint("58:cb:52:a9:a9:0f", 3050.0, 6900.0, 2550.0, "Bedroom 3");
            ap4 = new AccessPoint("58:cb:52:a9:bd:78", 7840.0, 6850.0, 3410.0, "Bedroom 4");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);
            accessPoints.add(ap2);
            accessPoints.add(ap3);
            accessPoints.add(ap4);

            macAddresses.add(ap1.getBssid());
            macAddresses.add(ap2.getBssid());
            macAddresses.add(ap3.getBssid());
            macAddresses.add(ap4.getBssid());
        }
        else if (configuration_type == CONFIGURATION_TYPE.TWO_DIMENSIONAL_1)
        {
            //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
            //  Description:
            //  2 APs, one in the lounge by the bay window and one in the dining room, on the floor in front of the dresser (in line with right foot)
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 490.0, 0.0, "Lounge");
            ap2 = new AccessPoint("58:cb:52:a9:b3:cd", 2980.0, 6900.0, 0.0, "Dining Room");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);
            accessPoints.add(ap2);

            macAddresses.add(ap1.getBssid());
            macAddresses.add(ap2.getBssid());
        }
        else if (configuration_type == CONFIGURATION_TYPE.TWO_DIMENSIONAL_2)
        {
            //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
            //  Description:
            //  3 APs, one in the lounge by the bay window and one in the dining room, on the floor in front of the dresser (in line with right foot)
            //  1 in the downstairs study (below socket)
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 490.0, 0.0, "Lounge");
            ap2 = new AccessPoint("58:cb:52:a9:b3:cd", 2980.0, 6900.0, 0.0, "Dining Room");
            ap3 = new AccessPoint("58:cb:52:a9:bd:78", 6320.0, -600.0, 0.0, "Study");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);
            accessPoints.add(ap2);
            accessPoints.add(ap3);

            macAddresses.add(ap1.getBssid());
            macAddresses.add(ap2.getBssid());
            macAddresses.add(ap3.getBssid());
        }
        else if (configuration_type == CONFIGURATION_TYPE.TESTING)
        {
            //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
            //  Description:
            //  3 APs, one in the lounge by the bay window and one in the dining room, on the floor in front of the dresser (in line with right foot)
            //  1 in the downstairs study (below socket)
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 490.0, 0.0, "Lounge");
            ap2 = new AccessPoint("58:cb:52:a9:a9:0f", 3050.0, 6900.0, 2550.0, "Bedroom 3");
            ap3 = new AccessPoint("58:cb:52:a9:bd:78", 6320.0, -600.0, 0.0, "Study");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);
            accessPoints.add(ap2);
            accessPoints.add(ap3);

            macAddresses.add(ap1.getBssid());
            macAddresses.add(ap2.getBssid());
            macAddresses.add(ap3.getBssid());
        }
        else if (configuration_type == CONFIGURATION_TYPE.TESTING_2)
        {
            //  The configuration consists of a number of Access points in 3 dimensional space (x,y,z) identified by their BSSID
            //  Description:
            //  3 APs, one in the lounge by the bay window and one in the dining room, on the floor in front of the dresser (in line with right foot)
            //  1 in the downstairs study (below socket)
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 490.0, 0.0, "Lounge");
            ap3 = new AccessPoint("58:cb:52:a9:bd:78", 6320.0, -600.0, 0.0, "Study");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);
            accessPoints.add(ap3);

            macAddresses.add(ap1.getBssid());
            macAddresses.add(ap3.getBssid());
        }
        else if (configuration_type == CONFIGURATION_TYPE.TESTING_3)
        {
            ap1 = new AccessPoint("3c:28:6d:ad:9e:ee", 3260.0, 490.0, 0.0, "Lounge");

            accessPoints = new ArrayList<>();
            macAddresses = new ArrayList<>();
            accessPoints.add(ap1);

            macAddresses.add(ap1.getBssid());
        }


        Collections.sort(accessPoints);
    }

    public List<AccessPoint> getConfiguration()
    {
        return accessPoints;
    }

    //  Used to check if the access point we are ranging to is in our map
    public List<String> getMacAddresses()
    {
        return macAddresses;
    }


}
