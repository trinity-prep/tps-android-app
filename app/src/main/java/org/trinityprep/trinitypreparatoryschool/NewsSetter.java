package org.trinityprep.trinitypreparatoryschool;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.transform.URIResolver;

/**
 * Created by Dominic martinez on 6/13/2016.
 *
 * Constructor:
 *      ScheduleSetter (Activity mainActivity //pass on MainActivity in order to set schedule text)
 *
 * Methods:
 *      public String fetchXML() {
 *          Creates connection to XML file and runs helper methods to set schedule
 *          Logs error message if unable to connect
 *      }
 *
 *      public void noSchedule() {
 *          Sets schedule text to "No Schedule" if no valid schedule is found on server
 *      }
 *
 *      public void setSchedule(String dayType) {
 *          Depending on application settings, calls setScheduleMS(dayType) or setScheduleUS(dayType)
 *          Sets schedule_title to an error if preferences are invalid
 *      }
 *
 *      private void setScheduleUS(String dayType) {
 *          Given a valid day type, sets schedule_title to the current period for Upper School
 *          If between periods, outputs "Going from (period1) to (period2)
 *      }
 *
 *      private void setScheduleMS(String dayType) {
 *          Given a valid day type, sets schedule_title to the current period for Middle School
 *          If between periods, outputs "Going from (period1) to (period2)
 *      }
 *
 *      private void createScheduleTable(String[] periods, Integer[] startTimes, Integer[] endTimes, int activeIndex) {
 *          Populates schedule table with given data, and highlights the row that corresponds to activeIndex.
 *          Any invalid activeIndex will cause no row to be highlighted, although it is HIGHLY recommended to
 *          set activeIndex to -1 if you do not want any row to be highlighted
 *      }
 *
 *      private String parseXML(XMLPullParser myParser) {
 *          Given an XMLPullParser connected to the calendar XML feed, finds current day type
 *          If day type for current day found, outputs day A-F. Otherwise, outputs null
 *      }
 */
public class NewsSetter {

    //Activity used to set schedule_title
    private Activity activity;
    //URL to grab XML from
    private final String newsURL = "http://www.trinityprep.org/rss.cfm?news=10";
    //True when currently finding schedule, false otherwise
    public boolean running;
    //True when in schedule_layout, false otherwise
    public boolean inMain;
    Thread XMLThread;
    private boolean tableExists = false;

    public NewsSetter (Activity mainActivity) {
        activity = mainActivity;
        running = false;
    }

    /* Establishes connection to RSS feed, creates XML parser, passes parser to parseXML()
    * Returns day type as a string A-F or null upon error */
    public void fetchXML() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(newsURL);
                    //Opens connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);

                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    InputStream stream;

                    // Starts the query
                    conn.connect();
                    if(conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
                    parseXML(myparser);
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

    /* PRECONDITION: periods[] and times[] are of the same length
     * POSTCONDITION: Creates a table containing all periods and times
     * if a schedule table is already created (scheduleRowIds != null), delete and replace existing schedule table*/
    private void createNewsTable(ArrayList<String> titles, ArrayList<String> links, ArrayList<String> dates, ArrayList<String> imageLinks) {
        // Find TableLayout defined in schedule_layout.xml
        TableLayout tl = (TableLayout) activity.findViewById(R.id.news_table);
        // If schedule table already exists, delete all children of schedule_table
        if(tableExists) {
            try {
                tl.removeAllViews();
            } catch(Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        tableExists = true;
        for(int i = 0; (i < titles.size() && i < links.size() && i < dates.size() && i < imageLinks.size()); i++) {
            // Import TableRow that contains text elements and inflate
            ViewStub stub = new ViewStub(activity);
            stub.setLayoutResource(R.layout.news_table_text);
            // Add view stub to schedule_table
            try {
                tl.addView(stub, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            } catch(Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
            TableRow trText = (TableRow) stub.inflate();
            int rowId = View.generateViewId();
            try {
                trText.setId(rowId);
            } catch(Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
            // Get ImageView and set image
            //ImageView img = (ImageView) trText.getChildAt(0);
            //Bitmap map = getBitmapFromURL(imageLinks.get(i));
            //img.setImageBitmap(map);
            // Set other text
            TextView title = (TextView) trText.getChildAt(1);
            String date = dates.get(i);
            Calendar cal = Calendar.getInstance();
            date = date.substring(0, date.indexOf(Integer.toString(cal.get(Calendar.YEAR))) + 4);
            title.setText(titles.get(i) + "\n\n" + date);

            // Add divider if not last
            if(i < titles.size() - 1) {
                ViewStub stub2 = new ViewStub(activity);
                stub2.setLayoutResource(R.layout.table_divider);
                // Add view stub to schedule_table
                try {
                    tl.addView(stub2, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.MATCH_PARENT));
                    stub2.inflate();
                } catch(Exception e) {
                    Toast.makeText(activity, "Critical error",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    public Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void parseXML(XmlPullParser myParser) {
        int event;
        String text = null;
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        ArrayList<String> imageLinks = new ArrayList<>();
        boolean inItems = false;

        try {
            event = myParser.next();
            //Loops through calendar looking for day type
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.TEXT:
                        text = myParser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (myParser.getName().equalsIgnoreCase("managingEditor")) {
                            inItems = true;
                        } else if (myParser.getName().equalsIgnoreCase("title") && text != null && inItems) {
                            titles.add(text);
                        } else if (myParser.getName().equalsIgnoreCase("link") && text != null && inItems) {
                            links.add(text);
                        } else if (myParser.getName().equalsIgnoreCase("pubDate") && text != null && inItems) {
                            dates.add(text);
                        }
                        break;
                    case XmlPullParser.START_TAG:
                        if (myParser.getName().equalsIgnoreCase("enclosure") && text != null && inItems) {
                            String value = myParser.getAttributeValue(null, "url");
                            imageLinks.add(value);
                        }
                        break;
                    default:
                        break;
                }
                event = myParser.next();
            }
            final ArrayList<String> titles1 = titles;
            final ArrayList<String> links1 = links;
            final ArrayList<String> dates1 = dates;
            final ArrayList<String> imageLinks1 = imageLinks;


            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    createNewsTable(titles1, links1, dates1, imageLinks1);
                }
            });
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }
}
