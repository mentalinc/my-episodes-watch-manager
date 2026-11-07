package nz.mentalinc.watcher.domain;

import java.util.Comparator;

public class ShowRuntimeAscendingComparator implements Comparator<Show> {
    @Override
    public int compare(Show o1, Show o2) {

        String r1 = o1.getRunTime();
        String r2 = o2.getRunTime();
        if (r1 == null) r1 = "";
        if (r2 == null) r2 = "";

        //this is to make sure error mins to go to the top.
        if (r1.equals("Error mins")) {
            return -1;
        }
        if (r2.equals("Error mins")) {
            return 1;
        }

        if (r1.isEmpty() || "null".equals(r1)) {
            return -1;
        }
        if (r2.isEmpty() || "null".equals(r2)) {
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
            String n1 = o1.getShowName() == null ? "" : o1.getShowName();
            String n2 = o2.getShowName() == null ? "" : o2.getShowName();
            return n1.compareTo(n2);
        } else return Integer.compare(o1Runtime, o2Runtime);
    }
}


