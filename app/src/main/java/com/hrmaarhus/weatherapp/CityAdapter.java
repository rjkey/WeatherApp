package com.hrmaarhus.weatherapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.hrmaarhus.weatherapp.utils.Globals.CELSIUS_UNICODE;
import static com.hrmaarhus.weatherapp.utils.Globals.LOG_TAG;


public class CityAdapter extends BaseAdapter {
    Context context;
    ArrayList<CityWeatherData> cities;
    CityWeatherData city;

    public CityAdapter(Context c, ArrayList<CityWeatherData> cities){
        this.context = c;
        this.cities = cities;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void setData(ArrayList<CityWeatherData> cities){
        this.cities = cities;
    }

    @Override
    public int getCount() {
        return cities.size();
    }

    @Override
    public Object getItem(int i) {
        return cities.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if(view == null){
            LayoutInflater demoInflater = (LayoutInflater)this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = demoInflater.inflate(R.layout.list_item, null);
        }
        city = cities.get(position);
        if(city!=null){
            setCityData(view);
        }

        return view;
    }

    private void setCityData(View view){
        TextView cityNameTextView = (TextView)view.findViewById(R.id.list_item_city_name);
        cityNameTextView.setText(city.getCityName());

        String tempStr = String.format("%.1f", city.getTemperature()) + CELSIUS_UNICODE;


        TextView cityTempTextView = (TextView)view.findViewById(R.id.list_item_temp);
        cityTempTextView.setText(tempStr);

        TextView cityHumidityTextView = (TextView)view.findViewById(R.id.list_item_humidity);
        cityHumidityTextView.setText(city.getHumidity().toString());

    }
}
