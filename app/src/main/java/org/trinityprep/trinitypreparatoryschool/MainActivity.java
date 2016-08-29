package org.trinityprep.trinitypreparatoryschool;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;

/* Created by Trinity Preparatory School
*
*  Original Creators:
*       Dominic Martinez (DM)
*       Ethan Thomas (ET)
*  Consultants/Helpers:
*       Daniel Contaldi (DC)
*       Alex Guguchev (AG)
*  Mentors:
*       Susan Frederick
*       Sherry Hay
*       Denise Musselwhite
*       Rita Kniele
*
* Version 1.0.1; State: Open Beta
* 	    - Update Gradle - DM
* 	    - Fixed schedule error that would cause a "No Schedule" error (unremoved debug statement) - DM
*/

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //Index variables
    public static final int SCHEDULE_INDEX = 0;
    public static final int NEWS_INDEX = 1;
    public static final int GRILLE_INDEX = 2;
    public static final int SETTINGS_INDEX = 3;
    //Contains array of ids for rows in schedule_table
    private static ArrayList<Integer> scheduleRowIds = null;
    //String that contains link to load for grille menu
    private final String GRILLE_URL = "http://www.sagedining.com/menus/trinitypreparatory";
    //Boolean array for activity
    public ArrayList<Boolean> activities;
    //Object for scheduler
    ScheduleSetter schedule;
    //Object for news
    NewsSetter news;
    //Contains refresh menu
    Menu refreshMenu;
    //Booleans for activity
    private boolean inSchedule = true;
    private boolean inNews = false;
    private boolean inGrille = false;
    private boolean inSettings = false;
    //Contains dayType
    private String dayType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialize activity view, toolbar, and navbar
        super.onCreate(savedInstanceState);
        //Initialize ImageLoader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);
        //Instantiate objects
        schedule = new ScheduleSetter(this);
        news = new NewsSetter(this);
        //Populate boolean array for activity
        activities = new ArrayList<>();
        activities.add(inSchedule);
        activities.add(inNews);
        activities.add(inGrille);
        activities.add(inSettings);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //Call fetchXML to set schedule_title to current period if scheduler is not currently running
        if (!schedule.running) {
            schedule.fetchXML();
        }
    }

    /* Recreates toolbar and navigation view
     * Call when switching content views
     */
    public void navView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (news.inWeb) {
            View webview = findViewById(R.id.news_webview);
            View newsScroll = findViewById(R.id.news_scroll);
            webview.setVisibility(View.GONE);
            newsScroll.setVisibility(View.VISIBLE);
            news.inWeb = false;
            setRefreshMenuVisiblity(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        schedule.runRefresh = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        schedule.fetchXML();
        schedule.runRefresh = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        refreshMenu = menu;
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            //Refreshes schedule
            //Call fetchXML to set schedule_title to current period if in main content and scheduler is not currently running
            if (activities.get(SCHEDULE_INDEX) && !schedule.running) {
                schedule.fetchXML();
            } else if (activities.get(NEWS_INDEX) && !news.running) {
                news.fetchXML();
            } else if (activities.get(GRILLE_INDEX)) {
                try {
                    WebView webGrille = (WebView) findViewById(R.id.grille_webview);
                    webGrille.loadUrl(GRILLE_URL);
                } catch (Exception e) {
                    Toast.makeText(this, "Refresh error", Toast.LENGTH_LONG).show();
                    Log.e("Error", "Refresh", e);
                }
            } else {
                Toast.makeText(this, "Refresh error", Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void setViewVisible(int index) {
        ArrayList<View> viewArr = new ArrayList<>();
        /* 0 - Schedule view
         * 1 - News view
         * 2 - Grille view
         * 3 - Settings fragment */
        View scheduleView = findViewById(R.id.schedule_include);
        viewArr.add(scheduleView);

        View newsView = findViewById(R.id.news_include);
        viewArr.add(newsView);

        View grilleView = findViewById(R.id.grille_include);
        viewArr.add(grilleView);

        View settingsFragment = findViewById(R.id.settings_fragment);
        viewArr.add(settingsFragment);

        for (int i = 0; i < viewArr.size(); i++) {
            if (i == index) {
                viewArr.get(i).setVisibility(View.VISIBLE);
                activities.set(i, true);
            } else {
                viewArr.get(i).setVisibility(View.GONE);
                activities.set(i, false);
            }
        }
    }

    public void createWebViewGrille() {
        try {
            WebView webGrille = (WebView) findViewById(R.id.grille_webview);
            View loadingView = findViewById(R.id.grille_loader);
            loadingView.setVisibility(View.VISIBLE);
            webGrille.setVisibility(View.GONE);
            webGrille.setWebChromeClient(new WebChromeClient());
            webGrille.setWebViewClient(new WebViewClient() {

                public void onPageFinished(WebView view, String url) {
                    WebView webGrille = (WebView) findViewById(R.id.grille_webview);
                    View loadingView = findViewById(R.id.grille_loader);
                    loadingView.setVisibility(View.GONE);
                    webGrille.setVisibility(View.VISIBLE);
                }
            });
            webGrille.clearCache(true);
            webGrille.clearHistory();
            webGrille.setWebContentsDebuggingEnabled(false);
            webGrille.getSettings().setJavaScriptEnabled(true);
            webGrille.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            webGrille.getSettings().setBuiltInZoomControls(true);
            webGrille.getSettings().setDisplayZoomControls(false);
            webGrille.loadUrl(GRILLE_URL);
        } catch (Exception e) {
            Toast.makeText(this, "WebView error", Toast.LENGTH_LONG).show();
            Log.e("Error", "WebView", e);
        }
    }

    public void setRefreshMenuVisiblity(boolean visible) {
        refreshMenu.setGroupVisible(R.id.refresh_menu_group, visible);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_schedule) {
            setRefreshMenuVisiblity(true);
            //Refreshes schedule
            setViewVisible(SCHEDULE_INDEX);
            if (!schedule.running) {
                schedule.fetchXML();
            }
        } else if (id == R.id.nav_news) {
            setRefreshMenuVisiblity(true);
            setViewVisible(NEWS_INDEX);
            news.fetchXML();
        } else if (id == R.id.nav_grille) {
            setRefreshMenuVisiblity(true);
            setViewVisible(GRILLE_INDEX);
            createWebViewGrille();
        } else if (id == R.id.nav_settings) {
            //Change content view to settings
            setRefreshMenuVisiblity(false);
            setViewVisible(SETTINGS_INDEX);
            //Add settings fragment to navigation
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragment, new SettingsFragment())
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
