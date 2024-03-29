package com.hrmaarhus.weatherapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hrmaarhus.weatherapp.model.CityWeather;

import java.util.ArrayList;

import static com.hrmaarhus.weatherapp.utils.Globals.CELSIUS_UNICODE;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_NAME;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_NAME_TO_BE_REMOVED;
import static com.hrmaarhus.weatherapp.utils.Globals.CITY_WAS_REMOVED;
import static com.hrmaarhus.weatherapp.utils.Globals.CWD_OBJECT;
import static com.hrmaarhus.weatherapp.utils.Globals.IS_BOUND;
import static com.hrmaarhus.weatherapp.utils.Globals.LOG_TAG;
import static com.hrmaarhus.weatherapp.utils.Globals.NEW_WEATHER_EVENT;
import static com.hrmaarhus.weatherapp.utils.Globals.ONE_CITY_WEATHER_EXTRA;

public class CityDetailsActivity extends AppCompatActivity {

    private Button removeBtn, okBtn;
    private TextView cityNameTextView, humidityTextView, temperatureTextView, descriptionTextView;

    private WeatherService mWeatherService;
    private boolean mBound = false;

    private String cityName;
    private CityWeatherData cityWeatherData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_details);

        removeBtn = findViewById(R.id.removeBtn);
        okBtn = findViewById(R.id.okBtn);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);

        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //return from activity with information to delete city
                Intent returnIntent = new Intent();
                returnIntent.putExtra(CITY_WAS_REMOVED, true);
                returnIntent.putExtra(CITY_NAME_TO_BE_REMOVED, cityName);
                setResult(RESULT_OK, returnIntent);

                finish();
            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //binding to service
        Intent weatherIntent = new Intent(getApplicationContext(), WeatherService.class);
        bindService(weatherIntent, mConnection, Context.BIND_AUTO_CREATE);

        //creating local broadcast receiver
        //to be able to get data from the weather service
        //listen for 'new data available' broadcast
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mWeatherReceiver,
                        new IntentFilter(NEW_WEATHER_EVENT));


        //retrieve name of the city that will be displayed in the activity
        Intent parentIntent = getIntent();
        cityName = parentIntent.getStringExtra(CITY_NAME);
        cityNameTextView.setText(cityName);

        //and startup weather data for it
        cityWeatherData = (CityWeatherData)parentIntent.getSerializableExtra(CWD_OBJECT);
        if(cityWeatherData!=null){
            displayWeatherData(cityWeatherData);
        }

    }

    @Override
    protected void onDestroy() {
        if(mConnection != null && mBound){
            Log.d(LOG_TAG, "CityDetailsActivity unbinding from the service");
            unbindService(mConnection);

        }

        //unregistering from broadcasts
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherReceiver);
        super.onDestroy();
    }

    //displaying city weather data
    private void displayWeatherData(CityWeatherData cityWeatherData){
        String tempString = String.format("%.1f", cityWeatherData.getTemperature()) + CELSIUS_UNICODE;
        String humidityString = cityWeatherData.getHumidity() + "%";
        String description = cityWeatherData.getWeatherDescription();

        temperatureTextView.setText(tempString);
        humidityTextView.setText(humidityString);
        descriptionTextView.setText(description);
    }

    //---------------------------------------------broadcast receiver
    private BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //received local broadcast from weather service
            Log.d(LOG_TAG,"CityDetailsActivity: received broadcast from weather service ");

            if(intent.getAction().equals(NEW_WEATHER_EVENT)){
                //broadcast informs about new weather data available
                //so get newest data and display it
                Log.d(LOG_TAG,"CityDetailsActivity: there is new weather data available");

                CityWeatherData cityWeatherData = mWeatherService.getCurrentWeather(cityName);
                displayWeatherData(cityWeatherData);

            }

        }
    };

    //------------------------------------------------handling connection with service
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG,"CityDetailsActivity onserviceconnected");
            //after binding to WeatherService
            //casting the IBinder
            WeatherService.LocalBinder binder = (WeatherService.LocalBinder)iBinder;
            //getting WeatherService instance to call its public methods
            mWeatherService = binder.getService();

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };
}
