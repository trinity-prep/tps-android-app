package org.trinityprep.trinitypreparatoryschool;

import android.os.Bundle;
import android.view.Menu;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
        implements NavigationView.OnNavigationItemSelectedListener
{
    //Object for scheduler
    ScheduleSetter schedule = new ScheduleSetter(this);
    //True when content view is activity_main, false otherwise
    private boolean inMain = true;
    //Contains dayType
    private String dayType = null;
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

        //Call fetchXML to set schedule_text to current period if scheduler is not currently running
        if(!schedule.running) {
            dayType = schedule.fetchXML();
            if(dayType != null) {
                schedule.setSchedule(dayType);
                schedule.startRefreshThread();
            } else {
                TextView text = (TextView) findViewById(R.id.schedule_text);
                text.setText("Unable to find day type, please try again later");
            }
            schedule.running = false;
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
            //Call fetchXML to set schedule_text to current period if in main content and scheduler is not currently running
            if(!schedule.running && inMain) {
                dayType = schedule.fetchXML();
                if(dayType != null) {
                    schedule.setSchedule(dayType);
                    schedule.startRefreshThread();
                } else {
                    TextView text = (TextView) findViewById(R.id.schedule_text);
                    text.setText("Unable to find day type, please try again later");
                }
                schedule.running = false;
            }
            View content = findViewById(R.id.drawer_layout);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_schedule) {
            //Change content view to main
            setContentView(R.layout.activity_main);
            navView();
            //Refreshes schedule without fetching XML if valid day type is already found, otherwise refreshes normally
            inMain = true;
            schedule.inMain = true;
            if(!schedule.running) {
                dayType = schedule.fetchXML();
                if(dayType != null) {
                    schedule.setSchedule(dayType);
                    schedule.startRefreshThread();
                } else {
                    dayType = schedule.fetchXML();
                    if(dayType != null) {
                        schedule.setSchedule(dayType);
                        schedule.startRefreshThread();
                    } else {
                        TextView text = (TextView) findViewById(R.id.schedule_text);
                        text.setText("Unable to find day type, please try again later");
                    }
                }
                schedule.running = false;
            }
        } else if (id == R.id.nav_news) {

        } else if (id == R.id.nav_grille) {

        } else if (id == R.id.nav_exam_schedule) {

        } else if (id == R.id.nav_settings) {
            //Change content view to settings
            setContentView(R.layout.activity_preferences);
            inMain = false;
            schedule.inMain = false;
            //Add settings fragment to navigation
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .commit();
            navView();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
