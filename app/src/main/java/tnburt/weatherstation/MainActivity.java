package tnburt.weatherstation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    static final String API_KEY = ""; //Insert API key here
    static final double hPa_TO_mmHg = 0.029529983071445;
    static final double ms_TO_knots = 1.943844;

    int shortFlagPx;
    int longFlagPx;

    double latitude;
    double longitude;

    private TextView cityView;
    private TextView coordView;
    private TextView temperatureView;
    private TextView humidityView;
    private TextView pressureView;
    private TextView precipitationView;

    private ImageView cloudLineVerticalView;
    private ImageView cloudLineHorizontalView;
    private ImageView cloudWedge1View;
    private ImageView cloudWedge2View;
    private ImageView cloudWedge3View;
    private ImageView cloudWedge4View;
    private ImageView cloudHalfLeftView;
    private ImageView cloudHalfRightView;
    private ImageView windFlagView;
    private ImageView windFlag1View;
    private ImageView windFlag2View;
    private ImageView windFlag3View;
    private ImageView windFlag4View;
    private ImageView windFlag5View;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            latitude = location.getLatitude();
            longitude = location.getLongitude();

            Log.d("Latitude: ", String.format("%.2f", latitude));
            Log.d("Longitude: ", String.format("%.2f", longitude));
        }

        cityView            = (TextView) findViewById(R.id.text_city);
        coordView           = (TextView) findViewById(R.id.text_coord);

        temperatureView     = (TextView) findViewById(R.id.text_temperature);
        humidityView        = (TextView) findViewById(R.id.text_humidity);
        pressureView        = (TextView) findViewById(R.id.text_pressure);

        precipitationView           = (TextView) findViewById(R.id.text_precipitation);

        cloudLineVerticalView       = (ImageView) findViewById(R.id.image_cloudVertical);
        cloudLineHorizontalView     = (ImageView) findViewById(R.id.image_cloudHorizontal);
        cloudWedge1View             = (ImageView) findViewById(R.id.image_cloudWedge1);
        cloudWedge2View             = (ImageView) findViewById(R.id.image_cloudWedge2);
        cloudWedge3View             = (ImageView) findViewById(R.id.image_cloudWedge3);
        cloudWedge4View             = (ImageView) findViewById(R.id.image_cloudWedge4);
        cloudHalfLeftView           = (ImageView) findViewById(R.id.image_cloudHalf_left);
        cloudHalfRightView          = (ImageView) findViewById(R.id.image_cloudHalf_right);

        windFlagView                = (ImageView) findViewById(R.id.image_windFlag);
        windFlag1View               = (ImageView) findViewById(R.id.image_windFlag1);
        windFlag2View               = (ImageView) findViewById(R.id.image_windFlag2);
        windFlag3View               = (ImageView) findViewById(R.id.image_windFlag3);
        windFlag4View               = (ImageView) findViewById(R.id.image_windFlag4);
        windFlag5View               = (ImageView) findViewById(R.id.image_windFlag5);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float logicalDensity = displayMetrics.density;
        shortFlagPx = (int)Math.ceil(40 * logicalDensity);
        longFlagPx = (int)Math.ceil(80 * logicalDensity);

        new FetchWeather().execute();
    }

    private class FetchWeather extends AsyncTask<Void, Void, String>
    {
        @Override
        protected void onPreExecute() {
            temperatureView.setText(String.valueOf("--"));
            humidityView.setText(String.valueOf("--"));
            pressureView.setText(String.valueOf("--"));
        }

        @Override
        protected String doInBackground(Void... urls) {
            String units = "imperial";

            try{
                URL url = new URL("http://api.openweathermap.org/data/2.5/weather?lat="+latitude+"&lon="+longitude+"&units="+units+"&appid="+API_KEY);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try{
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while((line = bufferedReader.readLine()) != null){
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    urlConnection.disconnect();
                }
            }
            catch (Exception e){
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if(response == null){
                response = "Could not fetch weather!";
            }
            Log.i("JSON", response);

            try{
                JSONObject jsonResponse = new JSONObject(response);

                updateCity(jsonResponse);
                updateCoordinates(jsonResponse);
                updateWeather(jsonResponse);
                updateMain(jsonResponse);
                updateClouds(jsonResponse);
                updateWind(jsonResponse);

            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateCity(JSONObject jsonResponse){
            try{
                String city = jsonResponse.getString("name");
                cityView.setText(city);
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateCoordinates(JSONObject jsonResponse){
            try{
                JSONObject coord = jsonResponse.getJSONObject("coord");

                String coordinates = "Lat: " + coord.getString("lat") + ", Long: " + coord.getString("lon");

                coordView.setText(coordinates);
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateWeather(JSONObject jsonResponse){
            try{
                JSONArray weather = jsonResponse.getJSONArray("weather");

                for(int i=0; i<=weather.length(); i++){
                    JSONObject condition = weather.getJSONObject(i);

                    int conditionId = condition.getInt("id");

                    if(conditionId>=200 && conditionId<300){
                        //thunderstorm
                        precipitationView.setText("\u2608");
                    }
                    else if(conditionId>=300 && conditionId<400){
                        // drizzle
                        precipitationView.setText("\u002C");
                    }
                    else if(conditionId>=500 && conditionId<600){
                        // rain
                        precipitationView.setText("\u2022");
                        if(conditionId>=520){
                            precipitationView.setText("\u25BD"); //shower
                        }
                    }
                    else if(conditionId>=600 && conditionId<700){
                        // snow
                        precipitationView.setText("\u2731");

                        if(conditionId == 611){
                            precipitationView.setText("\u25EC"); //sleet
                        }
                    }
                    else if(conditionId>=700 && conditionId<800){
                        // atmosphere
                        if(conditionId == 701 || conditionId == 741){
                            precipitationView.setText("\u2262"); //fog
                        }
                        if(conditionId == 711){
                            //smoke
                        }
                        if(conditionId == 721){
                            precipitationView.setText("\u221E"); //haze
                        }
                    }
                    else if(conditionId>=800 && conditionId<900){
                        //clouds
                    }
                    else if(conditionId>=900){
                        //extreme
                        if(conditionId == 906){
                            precipitationView.setText("\u25B3"); //hail
                        }
                    }
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateMain(JSONObject jsonResponse){
            try{
                JSONObject main = jsonResponse.getJSONObject("main");

                int temperature = main.getInt("temp");
                int humidity = main.getInt("humidity");
                double barometer = main.getDouble("pressure") * hPa_TO_mmHg;

                temperatureView.setText(String.valueOf(temperature));
                humidityView.setText(String.valueOf(humidity));
                pressureView.setText(String.valueOf(barometer));
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateClouds(JSONObject jsonResponse){
            try{
                JSONObject clouds = jsonResponse.getJSONObject("clouds");
                int cloudCover = clouds.getInt("all");

                if(cloudCover >= 5 && cloudCover < 15){
                    cloudLineVerticalView.setAlpha((float)1);
                } else if(cloudCover >= 15 && cloudCover < 35){
                    cloudWedge1View.setAlpha((float)1);
                } else if(cloudCover >= 35 && cloudCover < 45){
                    cloudLineVerticalView.setAlpha((float)1);
                    cloudWedge1View.setAlpha((float)1);
                } else if(cloudCover >= 45 && cloudCover < 60){
                    cloudWedge1View.setAlpha((float)1);
                    cloudWedge2View.setAlpha((float)1);
                } else if(cloudCover >= 60 && cloudCover < 70){
                    cloudWedge1View.setAlpha((float)1);
                    cloudWedge2View.setAlpha((float)1);
                    cloudLineHorizontalView.setAlpha((float)0);
                } else if(cloudCover >= 70 && cloudCover < 80){
                    cloudWedge1View.setAlpha((float)1);
                    cloudWedge2View.setAlpha((float)1);
                    cloudWedge3View.setAlpha((float)1);
                    cloudLineVerticalView.setAlpha((float)1);
                    cloudLineHorizontalView.setAlpha((float)1);
                } else if(cloudCover >= 80 && cloudCover < 95){
                    cloudHalfLeftView.setAlpha((float)1);
                    cloudHalfRightView.setAlpha((float)1);
                } else if(cloudCover >= 95){
                    cloudLineVerticalView.setAlpha((float)1);
                    cloudLineHorizontalView.setAlpha((float)1);
                    cloudWedge1View.setAlpha((float)1);
                    cloudWedge2View.setAlpha((float)1);
                    cloudWedge3View.setAlpha((float)1);
                    cloudWedge4View.setAlpha((float)1);
                }
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateWind(JSONObject jsonResponse){
            try{
                JSONObject wind = jsonResponse.getJSONObject("wind");

                double windSpeed = wind.getDouble("speed") * ms_TO_knots;
                int windDirection = wind.getInt("deg");

                if(windSpeed>0){
                    windFlagView.setAlpha((float)1);
                }
                if(windSpeed>=3){
                    windFlag2View.setAlpha((float)1);
                }
                if(windSpeed>=7){
                    windFlag1View.setAlpha((float)1);
                    windFlag2View.setAlpha((float)0);
                }
                if(windSpeed>=13){
                    windFlag2View.setAlpha((float)1);
                }
                if(windSpeed>=17){
                    windFlag2View.getLayoutParams().width = longFlagPx;
                }
                if(windSpeed>=23){
                    windFlag3View.setAlpha((float)1);
                }
                if(windSpeed>=27){
                    windFlag3View.getLayoutParams().width = longFlagPx;
                }
                if(windSpeed>=33){
                    windFlag4View.setAlpha((float)1);
                }
                if(windSpeed>=37){
                    windFlag4View.getLayoutParams().width = longFlagPx;
                }
                if(windSpeed>=43){
                    windFlag5View.setAlpha((float)1);
                }

                windFlagView.setRotation((float)windDirection);
                windFlag1View.setRotation((float)windDirection);
                windFlag2View.setRotation((float)windDirection);
                windFlag3View.setRotation((float)windDirection);
                windFlag4View.setRotation((float)windDirection);
                windFlag5View.setRotation((float)windDirection);
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }
    }
}
