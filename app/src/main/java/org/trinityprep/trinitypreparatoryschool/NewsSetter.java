package org.trinityprep.trinitypreparatoryschool;

import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

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
public class NewsSetter {

    //Boolean for if user is currently in webview
    public static boolean inWeb = false;
    //URL to grab XML from
    private final String newsURL = "http://www.trinityprep.org/rss.cfm?news=10";
    //True when currently finding schedule, false otherwise
    public boolean running;
    //True when in schedule_layout, false otherwise
    public boolean inMain;
    Thread XMLThread;
    ArrayList<String> links;
    //Activity used to set schedule_title
    private MainActivity activity;
    private boolean tableExists = false;
    private ArrayList<Integer> rowIds;

    public NewsSetter(MainActivity mainActivity) {
        links = new ArrayList<>();
        rowIds = new ArrayList<>();
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
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            View scrollView = activity.findViewById(R.id.news_scroll);
                            scrollView.setVisibility(View.GONE);
                            View loading = activity.findViewById(R.id.news_loader);
                            loading.setVisibility(View.VISIBLE);
                        }
                    });
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

    /* PRECONDITION: periods[] and times[] are of the same length
     * POSTCONDITION: Creates a table containing all periods and times
     * if a schedule table is already created (scheduleRowIds != null), delete and replace existing schedule table*/
    private void createNewsTable(ArrayList<String> titles, final ArrayList<String> links, ArrayList<String> dates, ArrayList<String> imageLinks) {
        // Find TableLayout defined in schedule_layout.xml
        TableLayout tl = (TableLayout) activity.findViewById(R.id.news_table);
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
        for (int i = 0; (i < titles.size() && i < links.size() && i < dates.size() && i < imageLinks.size()); i++) {
            // Import TableRow that contains text elements and inflate
            ViewStub stub = new ViewStub(activity);
            stub.setLayoutResource(R.layout.news_table_text);
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
                rowIds.add(rowId);
                trText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i = 0; (i < links.size() && i < rowIds.size()); i++) {
                            if (v.getId() == rowIds.get(i)) {
                                try {
                                    WebView webNews = (WebView) activity.findViewById(R.id.news_webview);
                                    View scrollView = activity.findViewById(R.id.news_scroll);
                                    View loadingView = activity.findViewById(R.id.news_loader);
                                    webNews.setVisibility(View.GONE);
                                    scrollView.setVisibility(View.GONE);
                                    loadingView.setVisibility(View.VISIBLE);
                                    webNews.setWebChromeClient(new WebChromeClient());
                                    webNews.setWebViewClient(new WebViewClient() {

                                        public void onPageFinished(WebView view, String url) {
                                            WebView webGrille = (WebView) activity.findViewById(R.id.news_webview);
                                            View loadingView = activity.findViewById(R.id.news_loader);
                                            webGrille.setVisibility(View.VISIBLE);
                                            loadingView.setVisibility(View.GONE);
                                        }
                                    });
                                    webNews.clearCache(true);
                                    webNews.clearHistory();
                                    webNews.setWebContentsDebuggingEnabled(false);
                                    webNews.getSettings().setJavaScriptEnabled(true);
                                    webNews.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                                    webNews.getSettings().setBuiltInZoomControls(true);
                                    webNews.getSettings().setDisplayZoomControls(false);
                                    webNews.loadUrl(links.get(i));
                                    inWeb = true;
                                    activity.setRefreshMenuVisiblity(false);
                                } catch (Exception e) {
                                    Toast.makeText(activity, "WebView error", Toast.LENGTH_LONG).show();
                                    Log.e("Error", "WebView", e);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Toast.makeText(activity, "Critical error",
                        Toast.LENGTH_LONG).show();
                return;
            }
            // Get ImageView and set image
            ImageLoader imgLoader = ImageLoader.getInstance();
            ImageView img = (ImageView) trText.getChildAt(0);
            imgLoader.displayImage(imageLinks.get(i), img);
            // Set title
            TextView title = (TextView) trText.getChildAt(1);
            String date = dates.get(i);
            Calendar cal = Calendar.getInstance();
            date = date.substring(0, date.indexOf(Integer.toString(cal.get(Calendar.YEAR))) + 4);
            title.setText(titles.get(i) + "\n\n" + date);

            // Add divider if not last
            if (i < titles.size() - 1) {
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

    private void parseXML(XmlPullParser myParser) {
        int event;
        String text = null;
        ArrayList<String> titles = new ArrayList<>();
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
                    View scrollView = activity.findViewById(R.id.news_scroll);
                    scrollView.setVisibility(View.VISIBLE);
                    View loading = activity.findViewById(R.id.news_loader);
                    loading.setVisibility(View.GONE);
                    createNewsTable(titles1, links1, dates1, imageLinks1);
                }
            });
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }
}
