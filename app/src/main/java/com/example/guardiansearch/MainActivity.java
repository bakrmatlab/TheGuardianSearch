package com.example.guardiansearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    static boolean active = false;
    final private String API_URL = "https://content.guardianapis.com/search?api-key=1c059c32-ecea-4045-a202-a9c1399222d5";
    SharedPreferences sharedPreferences;
    private ArrayList<NewsItem> elements = new ArrayList<>();
    private MyListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        boolean isTablet = findViewById(R.id.fragmentLocation) != null;

        setContentView(R.layout.activity_main);

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                drawer, myToolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        String savedSearchValue = sharedPreferences.getString("searchValue", "");

        TextView searchInput = findViewById(R.id.searchInput);

        if (!savedSearchValue.isEmpty()) {
            searchInput.setText(savedSearchValue);
        }

        Button searchButton = findViewById(R.id.searchButton);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        searchButton.setOnClickListener((v) -> {
            if (!TextUtils.isEmpty(searchInput.getText())) {
                editor.putString("searchValue", searchInput.getText().toString());
                editor.apply();

                if (inputMethodManager.isAcceptingText()) {

                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }

                Toast.makeText(getApplicationContext(), "Searching...", Toast.LENGTH_LONG).show();

                GuardianAPI req = new GuardianAPI();
                req.execute(String.format("%s&q=%s", API_URL, searchInput.getText().toString()));
            } else {
                Toast.makeText(getApplicationContext(), "Enter a value to search", Toast.LENGTH_LONG).show();
            }
        });

        ListView theList = findViewById(R.id.listView);

        adapter = new MyListAdapter();

        theList.setAdapter(adapter);

        theList.setOnItemClickListener((list, item, position, id) -> {
            Bundle itemsToPass = new Bundle();
            NewsItem listItem = elements.get(position);
            itemsToPass.putString("TITLE", listItem.getTitle());
            itemsToPass.putString("SECTION_NAME", listItem.getSectionName());
            itemsToPass.putString("URL", listItem.getUrl());
            itemsToPass.putString("DATE", listItem.getDate());

            if (isTablet) {
                DetailsFragment dFragment = new DetailsFragment();
                dFragment.setArguments(itemsToPass);
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentLocation, dFragment)
                        .commit();
            } else {
                Intent nextActivity = new Intent(MainActivity.this, EmptyActivity.class);
                nextActivity.putExtras(itemsToPass);
                startActivity(nextActivity);
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Intent myIntent;
        switch (item.getItemId()) {

            case R.id.home:
                if (!active) {
                    myIntent = new Intent(this, MainActivity.class);
                    this.startActivity(myIntent);
                }
                break;
            case R.id.favourites:
                myIntent = new Intent(this, SavedActivity.class);
                this.startActivity(myIntent);
                break;
        }

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);

        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String message = null;

        if (item.getItemId() == R.id.about) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Home")
                    .setMessage("You can search for news articles in this activity")
                    .setCancelable(true)
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }


    public class GuardianAPI extends AsyncTask<String, Integer, ArrayList<NewsItem>> {
        private final ProgressBar progressBar = findViewById(R.id.progressBar);

        @Override
        protected ArrayList<NewsItem> doInBackground(String... urls) {
            ArrayList<NewsItem> list = new ArrayList<>();

            publishProgress(0);
            progressBar.setAlpha(1);

            try {

                URL url = new URL(urls[0]);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();


                InputStream response = urlConnection.getInputStream();


                BufferedReader reader = new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8), 8);
                StringBuilder sb = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String result = sb.toString();

                JSONObject json = new JSONObject(result);


                JSONArray results = json.getJSONObject("response").getJSONArray("results");


                for (int i = 0; i < results.length(); i++) {
                    JSONObject object = results.getJSONObject(i);

                    list.add(new NewsItem(object.getString("id"), object.getString("sectionName"), object.getString("webTitle"), object.getString("webUrl"), object.getString("webPublicationDate")));
                }


                for (int i = 0; i <= 20; i++) {
                    try {
                        publishProgress(i * 5);
                        Thread.sleep(0, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            }

            progressBar.setAlpha(0);

            return list;
        }

        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(ArrayList<NewsItem> arrayList) {
            elements = arrayList;
            adapter.notifyDataSetChanged();
        }

    }


    private static class NewsItem {
        private String id, sectionName, title, url, date;

        public NewsItem(String id, String sectionName, String title, String url, String date) {
            this.id = id;
            this.sectionName = sectionName;
            this.title = title;
            this.url = url;
            this.date = date;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSectionName() {
            return sectionName;
        }

        public void setSectionName(String sectionName) {
            this.sectionName = sectionName;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }


    private class MyListAdapter extends BaseAdapter {
        public int getCount() {
            return elements.size();
        }

        public NewsItem getItem(int position) {
            return elements.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View old, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();


            @SuppressLint("ViewHolder") View newView = inflater.inflate(R.layout.row_layout, parent, false);

            TextView title = newView.findViewById(R.id.newsTitle);
            title.setText(getItem(position).getTitle());


            return newView;
        }
    }
}