package nz.mentalinc.watcher.domain;

import java.util.Comparator;

public class ShowRuntimeAscendingComparator implements Comparator<Show> {
    @Override
    public int compare(Show o1, Show o2) {

        //this is to make sure error mins to go to the top.
        if (o1.getRunTime().equals("Error mins")) {
            return -1;
        }
        if (o2.getRunTime().equals("Error mins")) {
            return 1;
        }

        if (o1.getRunTime().equals("null") || o1.getRunTime().isEmpty()) {
            return -1;
        }
        if (o2.getRunTime().equals("null") || o2.getRunTime().isEmpty()) {
            return 1;
        }

        //convert from string to a number so can do proper sorting using numbers instead of String

        int o1Runtime;
        int o2Runtime;
        try {
            o1Runtime = Integer.parseInt(o1.getRunTime());
            o2Runtime = Integer.parseInt(o2.getRunTime());
        } catch (NumberFormatException e) {
            return 0;
        }

        if (o1Runtime == o2Runtime) {
            //return 0;
            //sort by showname when the time is the same.
            return o1.getShowName().compareTo(o2.getShowName());
        } else return Integer.compare(o1Runtime, o2Runtime);
    }
}


