package org.trinityprep.trinitypreparatoryschool;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Dominic martinez on 6/13/2016.
 * <p/>
 * Constructor:
 * ScheduleSetter (Activity mainActivity //pass on MainActivity in order to set schedule text)
 * <p/>
 * Methods:
 * public String fetchXML() {
 * Creates connection to XML file and runs helper methods to set schedule
 * Logs error message if unable to connect
 * }
 * <p/>
 * public void noSchedule() {
 * Sets schedule text to "No Schedule" if no valid schedule is found on server
 * }
 * <p/>
 * public void startRefreshThread() {
 * If another refresh thread is not currently running, starts a refresh thread
 * Every time there is a minute change, if (dayType != null && !running && inMain), re-run setSchedule
 * Based on Thread.sleep(60000 - (date.get(Calendar.SECOND) * 1000));
 * }
 * <p/>
 * public void setSchedule(String dayType) {
 * Depending on application settings, calls setScheduleMS(dayType) or setScheduleUS(dayType)
 * Sets schedule_title to an error if preferences are invalid
 * }
 * <p/>
 * private void setScheduleUS(String dayType) {
 * Given a valid day type, sets schedule_title to the current period for Upper School
 * If between periods, outputs "Going from (period1) to (period2)
 * }
 * <p/>
 * private void setScheduleMS(String dayType) {
 * Given a valid day type, sets schedule_title to the current period for Middle School
 * If between periods, outputs "Going from (period1) to (period2)
 * }
 * <p/>
 * private void createScheduleTable(String[] periods, Integer[] startTimes, Integer[] endTimes, int activeIndex) {
 * Populates schedule table with given data, and highlights the row that corresponds to activeIndex.
 * Any invalid activeIndex will cause no row to be highlighted, although it is HIGHLY recommended to
 * set activeIndex to -1 if you do not want any row to be highlighted
 * }
 * <p/>
 * private String parseXML(XMLPullParser myParser) {
 * Given an XMLPullParser connected to the calendar XML feed, finds current day type
 * If day type for current day found, outputs day A-F. Otherwise, outputs null
 * }
 */
public class ScheduleSetter {

    //True when currently finding schedule, false otherwise
    public static boolean running;
    //URL to grab XML from
    private final String calendarURL = "http://www.trinityprep.org/data/calendar/rsscache/calendar_1516.rss";
    //If schedule is no longer active
    public boolean runRefresh = true;
    //Activity used to set schedule_title
    private MainActivity activity;
    //contains dayType
    private String dayType;
    //True when refresh thread running, false otherwise
    private boolean refreshRunning;
    //Contains array of ids for rows in schedule_table
    private boolean tableExists = false;

    public ScheduleSetter(MainActivity mainActivity) {
        activity = mainActivity;
        running = false;
        refreshRunning = false;
        runRefresh = true;
    }

    /* Establishes connection to RSS feed, creates XML parser, passes parser to parseXML()
    * Returns day type as a string A-F or null upon error */
    public void fetchXML() {
        dayType = null;
        TextView schedule = (TextView) activity.findViewById(R.id.schedule_title);
        schedule.setText("Loading Schedule");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(calendarURL);
                    //Opens connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);

                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    InputStream stream;

                    // Starts the query
                    conn.connect();
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        stream = conn.getInputStream();
                    } else {
                        //Ends thread if unable to obtain proper connection
                        return;
                    }

                    //Creates parser
                    XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myparser = xmlFactoryObject.newPullParser();

                    //Set parser to parse input stream
                    myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    myparser.setInput(stream, null);

                    //Get dayType A-F
                    dayType = parseXML(myparser);
                    if (dayType != null) {
                        setSchedule(dayType);
                        startRefreshThread();
                    } else {
                        noSchedule();
                    }
                    stream.close();
                } catch (Exception e) {
                    Log.e("Scheduler", "Cannot connect to server", e);
                }
                running = false;
            }
        });
        running = true;
        thread.start();
    }

    public void noSchedule() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView schedule = (TextView) activity.findViewById(R.id.schedule_title);
                schedule.setText("No Schedule");
            }
        });
    }

    public void startRefreshThread() {
        if (refreshRunning) {
            return;
        }
        refreshRunning = true;
        Thread th = new Thread(new Runnable() {
            public void run() {
                while (dayType != null && runRefresh) {
                    if (dayType != null && !running && activity.activities.get(activity.SCHEDULE_INDEX)) {
                        setSchedule(dayType);
                    }
                    try {
                        Calendar date = Calendar.getInstance();
                        Thread.sleep(60000 - (date.get(Calendar.SECOND) * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                refreshRunning = false;
            }
        });
        th.start();
    }

    public void setSchedule(String dayType) {
        running = true;
        final String DAY_TYPE = dayType;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
                String schoolPref = sharedPref.getString(SettingsFragment.KEY_SCHOOL_PREF, "");
                if (schoolPref.equals("MS")) {
                    setScheduleMS(DAY_TYPE);
                } else {
                    setScheduleUS(DAY_TYPE);
                }
                running = false;
            }
        });
    }

    //**PRECONDITION** Uppercase letter from A-F
    //**POSTCONDITION** schedule_title is set to current period for US
    private void setScheduleUS(String dayType) {
        //Start and end times of periods on different day types in minutes since 0000 (12 AM)
        Integer[] dayAStart = {470, 479, 527, 575, 604, 652, 700, 749, 796, 844, 888};
        Integer[] dayAEnd = {475, 523, 571, 600, 648, 696, 744, 792, 840, 888, 915};
        String[] dayAPeriods = {"Advisory", "1st Period", "2nd Period",
                "Assembly/Break", "3rd Period", "4th Period", "5th Period",
                "US Lunch", "6th Period", "7th Period", "Study Period"};
        Integer[] dayBStart = {470, 479, 563, 592, 676, 760, 804, 888};
        Integer[] dayBEnd = {475, 559, 588, 672, 756, 800, 884, 915};
        String[] dayBPeriods = {"Advisory", "1st Period",
                "US Assembly", "3rd Period", "5th Period",
                "US Lunch", "7th Period", "Study Period"};
        Integer[] dayCStart = {470, 504, 588, 621, 705, 753, 801, 885};
        Integer[] dayCEnd = {500, 584, 617, 701, 749, 797, 881, 915};
        String[] dayCPeriods = {"Advisory", "2nd Period",
                "Break", "4th Period", "US Chapel",
                "US Lunch", "6th Period", "Study Period"};
        Integer[] dayDStart = {470, 479, 527, 575, 584, 632, 680, 728, 776, 824, 872};
        Integer[] dayDEnd = {475, 523, 571, 580, 628, 676, 724, 772, 820, 868, 900};
        String[] dayDPeriods = {"Advisory", "1st Period", "2nd Period",
                "Break", "3rd Period", "4th Period", "5th Period",
                "US Lunch", "6th Period", "7th Period", "Assembly/Pep Rally"};
        Integer[] dayEStart = {470, 479, 563, 621, 705, 753, 801, 885};
        Integer[] dayEEnd = {475, 559, 617, 701, 749, 797, 881, 915};
        String[] dayEPeriods = {"Advisory", "2nd Period",
                "Assembly", "4th Period", "US Chapel",
                "US Lunch", "6th Period", "Study Period"};
        Integer[] dayFStart = {470, 504, 588, 672, 756, 800, 884};
        Integer[] dayFEnd = {500, 584, 668, 752, 796, 880, 915};
        String[] dayFPeriods = {"Advisory", "2nd Period",
                "Assembly", "4th Period", "US Lunch",
                "6th Period", "Study Period"};
        Integer[] daySStart = {470, 485, 519, 558, 597, 621, 660, 699, 738, 777, 816, 855};
        Integer[] daySEnd = {480, 515, 554, 593, 617, 656, 695, 734, 773, 812, 851, 890};
        String[] daySPeriods = {"Advisory", "Assembly", "1st Period", "2nd Period",
                "Break", "3rd Period", "4th Period", "5th Period",
                "US Lunch", "6th Period", "7th Period", "Advisory"};
        //Declare ArrayLists to store values polymorphically
        ArrayList<Integer[]> startTimes = new ArrayList<>();
        ArrayList<Integer[]> endTimes = new ArrayList<>();
        ArrayList<String[]> periods = new ArrayList<>();
        //Input data
        startTimes.add(dayAStart);
        startTimes.add(dayBStart);
        startTimes.add(dayCStart);
        startTimes.add(dayDStart);
        startTimes.add(dayEStart);
        startTimes.add(dayFStart);
        startTimes.add(daySStart);
        endTimes.add(dayAEnd);
        endTimes.add(dayBEnd);
        endTimes.add(dayCEnd);
        endTimes.add(dayDEnd);
        endTimes.add(dayEEnd);
        endTimes.add(dayFEnd);
        endTimes.add(daySEnd);
        periods.add(dayAPeriods);
        periods.add(dayBPeriods);
        periods.add(dayCPeriods);
        periods.add(dayDPeriods);
        periods.add(dayEPeriods);
        periods.add(dayFPeriods);
        periods.add(daySPeriods);
        //Select correct array
        int index;
        switch (dayType) {
            case "A":
                index = 0;
                break;
            case "B":
                index = 1;
                break;
            case "C":
                index = 2;
                break;
            case "D":
                index = 3;
                break;
            case "E":
                index = 4;
                break;
            case "F":
                index = 5;
                break;
            case "S":
                index = 6;
                break;
            default:
                Toast.makeText(activity, "Day type error",
                        Toast.LENGTH_LONG).show();
                return;
        }
        //Get current # of minutes since 12:00 AM
        Calendar date = Calendar.getInstance();
        int minute = (date.get(Calendar.HOUR_OF_DAY) * 60) + date.get(Calendar.MINUTE);

        Integer[] startTime = startTimes.get(index);
        Integer[] endTime = endTimes.get(index);
        String[] periodsDay = periods.get(index);
        if (minute < startTime[0] || minute >= endTime[endTime.length - 1]) {
            createScheduleTable(periods.get(index), startTimes.get(index), endTimes.get(index), -1);
            //Set schedule title to current day type
            TextView scheduleText = (TextView) activity.findViewById(R.id.schedule_title);
            scheduleText.setText("Day " + dayType);
            return;
        }

        // Get current period
        int currentPeriod = -1;
        for (int i = 0; i < startTime.length; i++) {
            if (minute < startTime[i]) {
                if (minute < endTime[i - 1]) {
                    currentPeriod = i - 1;
                    break;
                } else {
                    currentPeriod = i;
                    break;
                }
            }
        }
        if(currentPeriod == -1 && minute < endTime[endTime.length - 1]) {
            currentPeriod = endTime.length - 1;
        } else if(currentPeriod == -1) {
            Toast.makeText(activity, "Critical Error", Toast.LENGTH_SHORT).show();
            return;
        }

        //Set schedule title to current day type
        TextView scheduleText = (TextView) activity.findViewById(R.id.schedule_title);
        scheduleText.setText("Day " + dayType + " (" + (endTime[currentPeriod] - minute) + " minutes left in period)");

        createScheduleTable(periods.get(index), startTimes.get(index), endTimes.get(index), currentPeriod);
    }

    //**PRECONDITION** Uppercase letter from A-F
    //**POSTCONDITION** schedule_title is set to current period for MS
    private void setScheduleMS(String dayType) {
        //Start and end times of periods on different day types in minutes since 0000 (12 AM)
        Integer[] dayAStart = {470, 479, 527, 575, 604, 652, 700, 749, 796, 844, 888};
        Integer[] dayAEnd = {475, 523, 571, 600, 648, 696, 744, 792, 840, 888, 915};
        String[] dayAPeriods = {"Advisory", "1st Period", "2nd Period",
                "Assembly/Break", "3rd Period", "4th Period", "MS Lunch",
                "5th Period", "6th Period", "7th Period", "Study Period"};
        Integer[] dayBStart = {470, 479, 563, 592, 676, 720, 804, 888};
        Integer[] dayBEnd = {475, 559, 588, 672, 716, 800, 884, 915};
        String[] dayBPeriods = {"Advisory", "1st Period",
                "MS Break", "3rd Period", "MS Lunch",
                "5th Period", "7th Period", "Study Period"};
        Integer[] dayCStart = {470, 504, 588, 621, 705, 753, 801, 885};
        Integer[] dayCEnd = {500, 584, 617, 701, 749, 797, 881, 915};
        String[] dayCPeriods = {"Advisory", "2nd Period",
                "Break", "4th Period", "MS Lunch",
                "MS Chapel", "6th Period", "Study Period"};
        Integer[] dayDStart = {470, 479, 527, 575, 584, 632, 680, 728, 776, 824, 872};
        Integer[] dayDEnd = {475, 523, 571, 580, 628, 676, 724, 772, 820, 868, 900};
        String[] dayDPeriods = {"Advisory", "1st Period", "2nd Period",
                "Break", "3rd Period", "4th Period", "MS Lunch",
                "5th Period", "6th Period", "7th Period", "Assembly/Pep Rally"};
        Integer[] dayEStart = {470, 479, 563, 621, 705, 753, 801, 885};
        Integer[] dayEEnd = {475, 559, 617, 701, 749, 797, 881, 915};
        String[] dayEPeriods = {"Advisory", "2nd Period",
                "Assembly", "4th Period", "MS Lunch",
                "MS Chapel", "6th Period", "Study Period"};
        Integer[] dayFStart = {470, 504, 588, 672, 716, 800, 884};
        Integer[] dayFEnd = {500, 584, 668, 712, 796, 880, 915};
        String[] dayFPeriods = {"Advisory", "2nd Period",
                "Assembly", "MS Lunch", "4th Period",
                "6th Period", "Study Period"};
        Integer[] daySStart = {470, 485, 519, 558, 597, 621, 660, 699, 738, 777, 816, 855};
        Integer[] daySEnd = {480, 515, 554, 593, 617, 656, 695, 734, 773, 812, 851, 890};
        String[] daySPeriods = {"Advisory", "Assembly", "1st Period", "2nd Period",
                "Break", "3rd Period", "4th Period", "MS Lunch",
                "5th Period", "6th Period", "7th Period", "Advisory"};
        //Declare ArrayLists to store values polymorphically
        ArrayList<Integer[]> startTimes = new ArrayList<>();
        ArrayList<Integer[]> endTimes = new ArrayList<>();
        ArrayList<String[]> periods = new ArrayList<>();
        //Input data
        startTimes.add(dayAStart);
        startTimes.add(dayBStart);
        startTimes.add(dayCStart);
        startTimes.add(dayDStart);
        startTimes.add(dayEStart);
        startTimes.add(dayFStart);
        startTimes.add(daySStart);
        endTimes.add(dayAEnd);
        endTimes.add(dayBEnd);
        endTimes.add(dayCEnd);
        endTimes.add(dayDEnd);
        endTimes.add(dayEEnd);
        endTimes.add(dayFEnd);
        endTimes.add(daySEnd);
        periods.add(dayAPeriods);
        periods.add(dayBPeriods);
        periods.add(dayCPeriods);
        periods.add(dayDPeriods);
        periods.add(dayEPeriods);
        periods.add(dayFPeriods);
        periods.add(daySPeriods);
        //Select correct array
        int index;
        switch (dayType) {
            case "A":
                index = 0;
                break;
            case "B":
                index = 1;
                break;
            case "C":
                index = 2;
                break;
            case "D":
                index = 3;
                break;
            case "E":
                index = 4;
                break;
            case "F":
                index = 5;
                break;
            case "S":
                index = 6;
                break;
            default:
                Toast.makeText(activity, "Day type error",
                        Toast.LENGTH_LONG).show();
                return;
        }
        //Get current # of minutes since 12:00 AM
        Calendar date = Calendar.getInstance();
        int minute = (date.get(Calendar.HOUR_OF_DAY) * 60) + date.get(Calendar.MINUTE);

        Integer[] startTime = startTimes.get(index);
        Integer[] endTime = endTimes.get(index);
        String[] periodsDay = periods.get(index);

        if (minute < startTime[0] || minute >= endTime[endTime.length - 1]) {
            createScheduleTable(periods.get(index), startTimes.get(index), endTimes.get(index), -1);
            //Set schedule title to current day type
            TextView scheduleText = (TextView) activity.findViewById(R.id.schedule_title);
            scheduleText.setText("Day " + dayType);
            return;
        }

        // Get current period
        int currentPeriod = -1;
        for (int i = 0; i < startTime.length; i++) {
            if (minute < startTime[i]) {
                if (minute < endTime[i - 1]) {
                    currentPeriod = i - 1;
                    break;
                } else {
                    currentPeriod = i;
                    break;
                }
            }
        }
        if(currentPeriod == -1 && minute < endTime[endTime.length - 1]) {
            currentPeriod = endTime.length - 1;
        } else if(currentPeriod == -1) {
            Toast.makeText(activity, "Critical Error", Toast.LENGTH_SHORT).show();
            return;
        }

        //Set schedule title to current day type
        TextView scheduleText = (TextView) activity.findViewById(R.id.schedule_title);
        scheduleText.setText("Day " + dayType + " (" + (endTime[currentPeriod] - minute) + " minutes left in period)");

        createScheduleTable(periods.get(index), startTimes.get(index), endTimes.get(index), currentPeriod);
    }

    /* PRECONDITION: periods[] and times[] are of the same length
     * POSTCONDITION: Creates a table containing all periods and times
     * if a schedule table is already created (scheduleRowIds != null), delete and replace existing schedule table*/
    private void createScheduleTable(String[] periods, Integer[] startTimes, Integer[] endTimes, int activeIndex) {
        // Find TableLayout defined in schedule_layout.xml
        TableLayout tl = (TableLayout) activity.findViewById(R.id.schedule_table);
        // If schedule table already exists, delete all children of schedule_table
        if (tableExists) {
            try {
                tl.removeAllViews();
            } catch (Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        tableExists = true;
        for (int i = 0; (i < periods.length && i < startTimes.length && i < endTimes.length); i++) {
            // Import TableRow that contains text elements and inflate
            ViewStub stub = new ViewStub(activity);
            stub.setLayoutResource(R.layout.schedule_table_text);
            // Add view stub to schedule_table
            try {
                tl.addView(stub, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            } catch (Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
            TableRow trText = (TableRow) stub.inflate();
            int rowId = View.generateViewId();
            try {
                trText.setId(rowId);
            } catch (Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (activeIndex == i && activeIndex != -1) {
                trText.setBackgroundColor(Color.parseColor("#FF69F0AE"));
            }
            // Get period TextView and set parameters
            TextView periodTV = (TextView) trText.getChildAt(0);
            periodTV.setText(periods[i]);
            // Get time TextView
            TextView timeTV = (TextView) trText.getChildAt(1);
            int startHour = startTimes[i] / 60;
            int endHour = endTimes[i] / 60;
            int startMinuteInt = startTimes[i] % 60;
            int endMinuteInt = endTimes[i] % 60;
            if (startHour > 12) {
                startHour -= 12;
            }
            if (endHour > 12) {
                endHour -= 12;
            }
            String startMinute = String.format("%02d", startMinuteInt);
            String endMinute = String.format("%02d", endMinuteInt);
            timeTV.setText(startHour + ":" + startMinute + "-" + endHour + ":" + endMinute);

            // Add divider if not last
            if (i < periods.length - 1) {
                ViewStub stub2 = new ViewStub(activity);
                stub2.setLayoutResource(R.layout.table_divider);
                // Add view stub to schedule_table
                try {
                    tl.addView(stub2, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.MATCH_PARENT));
                    stub2.inflate();
                } catch (Exception e) {
                    Toast.makeText(activity, "Critical error",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    private String parseXML(XmlPullParser myParser) {
        int event;
        String text = null;

        try {
            event = myParser.next();
            //Loops through calendar looking for day type
            boolean foundDayType = false;
            String dayType = null;
            String[] calendarMonthsStringSpecial = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
            Calendar cSpecial = Calendar.getInstance();
            String dateSpecial = cSpecial.get(Calendar.DAY_OF_MONTH) + " " + calendarMonthsStringSpecial[cSpecial.get(Calendar.MONTH)] + " " + cSpecial.get(Calendar.YEAR);
            if (dateSpecial.equalsIgnoreCase("16 aug 2016")) {
                return "S";
            }
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (myParser.getName().equalsIgnoreCase("title") && text != null) {
                            if (text.toLowerCase().contains("day a")) {
                                dayType = "A";
                                foundDayType = true;
                            } else if (text.toLowerCase().contains("day b")) {
                                dayType = "B";
                                foundDayType = true;
                            } else if (text.toLowerCase().contains("day c")) {
                                dayType = "C";
                                foundDayType = true;
                            } else if (text.toLowerCase().contains("day d")) {
                                dayType = "D";
                                foundDayType = true;
                            } else if (text.toLowerCase().contains("day e")) {
                                dayType = "E";
                                foundDayType = true;
                            } else if (text.toLowerCase().contains("day f")) {
                                dayType = "F";
                                foundDayType = true;
                            } else {
                                foundDayType = false;
                            }
                        } else if (myParser.getName().equalsIgnoreCase("description")) {
                            String[] calendarMonthsString = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
                            Calendar c = Calendar.getInstance();
                            String date = c.get(Calendar.DAY_OF_MONTH) + " " + calendarMonthsString[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
                            if(c.get(Calendar.DAY_OF_MONTH) < 10) {
                                date = " 0" + date;
                            } else {
                                date = " " + date;
                            }
                            if (text != null) {
                                if (text.toLowerCase().contains(date) && foundDayType) {
                                    return dayType;
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
                event = myParser.next();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
