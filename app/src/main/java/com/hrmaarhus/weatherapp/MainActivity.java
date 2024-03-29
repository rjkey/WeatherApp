package com.hrmaarhus.weatherapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hrmaarhus.weatherapp.model.Main;
import com.hrmaarhus.weatherapp.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

import static com.hrmaarhus.weatherapp.utils.Globals.CELSIUS_UNICODE;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_DETAILS_REQ_CODE;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_NAME;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_NAME_EXTRA;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_NAME_TO_BE_REMOVED;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_WAS_REMOVED;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_WEATHER_DATA;
import static com.hrmaarhus.weatherapp.utils.Globals.CWD_OBJECT;
import static com.hrmaarhus.weatherapp.utils.Globals.IS_BOUND;
import static com.hrmaarhus.weatherapp.utils.Globals.LOG_TAG;
import static com.hrmaarhus.weatherapp.utils.Globals.NEW_WEATHER_EVENT;
import static com.hrmaarhus.weatherapp.utils.Globals.ONE_CITY_WEATHER_EXTRA;
import static com.hrmaarhus.weatherapp.utils.Globals.WEATHER_CITY_EVENT;

public class MainActivity extends AppCompatActivity{
    private Button refreshBtn;

    private WeatherService weatherService;
    boolean mBound = false;

    private Button addCityBtn;
    private EditText newCityEditText;

    private CityAdapter adapter;
    private ListView listView;

    private ArrayList<CityWeatherData> citiesList;
    private String cityNameToBeRemoved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newCityEditText = (EditText) findViewById(R.id.newCityText);

        refreshBtn = (Button)findViewById(R.id.refreshBtn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                weatherService.requestAllCitiesWeatherUpdate();
            }
        });

        addCityBtn = findViewById(R.id.addCityBtn);
        addCityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newCity = newCityEditText.getText().toString();
                if(newCity != null && newCity != "") {
                    weatherService.addCity(newCity);
                    newCityEditText.setText("");
                }
            }
        });

        //starting the weather service
        Intent weatherIntent = new Intent(getApplicationContext(), WeatherService.class);
        startService(weatherIntent);

        bindService(weatherIntent, mConnection, Context.BIND_AUTO_CREATE);

        //creating local broadcast receiver
        //to be able to get data from the weather service
        //listen for 'new data available' broadcast
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mWeatherReceiver,
                        new IntentFilter(NEW_WEATHER_EVENT));


        //setting cities list that will be displayed in the list view
        citiesList = new ArrayList<CityWeatherData>();

        //setting up the list view of cities
        prepareListView();

    }

    //---------------------------------------------broadcast receiver
    private BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //received local broadcast from weather service
            Log.d(LOG_TAG,"MainActivity: received broadcast from weather service ");

            if(intent.getAction().equals(NEW_WEATHER_EVENT)){
                //broadcast informs about new weather data available
                //so get newest data
                Log.d(LOG_TAG,"MainActivity: there is new weather data available");

                ArrayList<CityWeatherData> cityWeatherDataArrayList =
                        (ArrayList<CityWeatherData>)weatherService.getAllCitiesWeather();
                updateCitiesWeatherListView(cityWeatherDataArrayList);


            }
        }
    };


    //------------------------------------------------handling connection with service
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //after binding to WeatherService
            //casting the IBinder
            WeatherService.LocalBinder binder = (WeatherService.LocalBinder)iBinder;
            //getting WeatherService instance to call its public methods
            weatherService = binder.getService();

            mBound = true;

            if(cityNameToBeRemoved != null){
                doRemoveCity(cityNameToBeRemoved);
                cityNameToBeRemoved = null;
            }
        }

        @Override
            public void onServiceDisconnected(ComponentName componentName) {

            Log.d(LOG_TAG, "MainActivity: onServiceDisconnected");
            mBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        if(mConnection != null && mBound){
            Log.d(LOG_TAG, "MainActivity unbinding from the service");
            unbindService(mConnection);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherReceiver);

        super.onDestroy();
    }

    @Override
    protected void onPause() {

        super.onPause();

    }


    //-------------------------------------------------------------------list view management
    //setting up ListView, that will display contents of citiesList
    //clicking on a city item results in opening details for that city
    private void prepareListView(){
        adapter = new CityAdapter(this, citiesList);
        listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String clickedCityString = citiesList.get(position).getCityName();
                Log.d(LOG_TAG,"MainActivity: opening details activity for "+clickedCityString);
                //get startup weather data for that activity
                CityWeatherData cityWeatherData = weatherService.getCurrentWeather(clickedCityString);

                Intent openCityDetailsIntent =
                        new Intent(getApplicationContext(), CityDetailsActivity.class);
                openCityDetailsIntent.putExtra(CITY_NAME, clickedCityString);
                openCityDetailsIntent.putExtra(CWD_OBJECT, cityWeatherData);
                startActivityForResult(openCityDetailsIntent, CITY_DETAILS_REQ_CODE);

            }
        });
    }

    //update local citiesList with the list @param cityWeatherDataArrayList
    //and notify observers of adapter, so that list view refreshes
    private void updateCitiesWeatherListView(ArrayList<CityWeatherData> cityWeatherDataArrayList){
        Log.d(LOG_TAG, "MainActivity list received in broadcast with " + cityWeatherDataArrayList.size() + " elements");
        citiesList = cityWeatherDataArrayList;

        adapter.setData(citiesList);

        adapter.notifyDataSetChanged();
        listView.invalidateViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CITY_DETAILS_REQ_CODE){
            Log.d(LOG_TAG, "MainActivityL came back from details activity with result code "+resultCode);
            if(resultCode == RESULT_OK){
                //user is returning from details activity
                //check if user removed city they were viewing
                //if user clicked 'ok' button, no extras were set, so default values stand
                boolean removeCity = data.getBooleanExtra(CITY_WAS_REMOVED, false);
                if(removeCity){
                    //get the name of the city to be removed
                    String cityToBeRemoved = data.getStringExtra(CITY_NAME_TO_BE_REMOVED);
                    //remove city from the storage
                    doRemoveCity(cityToBeRemoved);

                }
            }
        }
    }

    private void doRemoveCity(final String cityToBeRemoved) {
        if(weatherService !=null){
            weatherService.removeCity(cityToBeRemoved);
            //and update the list view
            updateCitiesWeatherListView(
                    (ArrayList<CityWeatherData>)weatherService.getAllCitiesWeather());

        }else{
            cityNameToBeRemoved = cityToBeRemoved;
        }

    }

}
