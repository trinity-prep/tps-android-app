package org.trinityprep.trinitypreparatoryschool;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.ViewUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.w3c.dom.Text;

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
*  Current Version: 0.1.0
*  Current Version Changelog: (access full log in changelog.txt)
*       Version 0.1.0; State: Closed Alpha
*       - Added schedule for seeing current period - DM
*       - Added schedule refreshing for automatic period changes - DM
*       - Added settings menu and option to choose between MS and US - DM
*       - Added documentation to code and GitHub - DM
*/

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    //Object for scheduler
    ScheduleSetter schedule = new ScheduleSetter(this);
    //True when content view is activity_main, false otherwise
    private boolean inMain = true;
    //Contains dayType
    private String dayType = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    //Contains array of ids for rows in schedule_table
    private static ArrayList<Integer> scheduleRowIds = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Initialize activity view, toolbar, and navbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        //Call fetchXML to set schedule_title to current period if scheduler is not currently running
        if (!schedule.running) {
            schedule.fetchXML();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
            if (!schedule.running && inMain) {
                schedule.fetchXML();
            } else {
                Toast.makeText(this, "Not in schedule",
                        Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void setViewVisible(int index) {
        ArrayList<View> viewArr = new ArrayList<>();
        /* 0 - Schedule view
         * 1 - Settings fragment */
        View scheduleView = findViewById(R.id.schedule_include);
        viewArr.add(scheduleView);
        View newsView = findViewById(R.id.news_include);
        viewArr.add(newsView);
        View settingsFragment = findViewById(R.id.settings_fragement);
        viewArr.add(settingsFragment);

        for(int i = 0; i < viewArr.size(); i++) {
            if(i == index) {
                viewArr.get(i).setVisibility(View.VISIBLE);
            } else {
                viewArr.get(i).setVisibility(View.GONE);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_schedule) {
            //Refreshes schedule
            inMain = true;
            schedule.inMain = true;
            setViewVisible(0);
            if (!schedule.running) {
                schedule.fetchXML();
            }
        } else if (id == R.id.nav_news) {
            inMain = false;
            schedule.inMain = false;
            setViewVisible(1);
            NewsSetter news = new NewsSetter(this);
            news.fetchXML();
        } else if (id == R.id.nav_grille) {
            Uri uri = Uri.parse("http://www.sagedining.com/menus/trinitypreparatory");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            //Change content view to settings
            inMain = false;
            schedule.inMain = false;
            setViewVisible(2);
            //Add settings fragment to navigation
            getFragmentManager().beginTransaction()
                    .replace(R.id.settings_fragement, new SettingsFragment())
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://org.trinityprep.trinitypreparatoryschool/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://org.trinityprep.trinitypreparatoryschool/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
