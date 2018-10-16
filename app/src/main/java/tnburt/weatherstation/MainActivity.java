package tnburt.weatherstation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

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

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private FusedLocationProviderClient fusedLocationClient;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if(checkPermission()){
            getLocation();
        }
        else{
            requestPermission();
        }

        cityView            = findViewById(R.id.text_city);
        coordView           = findViewById(R.id.text_coord);

        temperatureView     = findViewById(R.id.text_temperature);
        humidityView        = findViewById(R.id.text_humidity);
        pressureView        = findViewById(R.id.text_pressure);

        precipitationView           = findViewById(R.id.text_precipitation);

        cloudLineVerticalView       = findViewById(R.id.image_cloudVertical);
        cloudLineHorizontalView     = findViewById(R.id.image_cloudHorizontal);
        cloudWedge1View             = findViewById(R.id.image_cloudWedge1);
        cloudWedge2View             = findViewById(R.id.image_cloudWedge2);
        cloudWedge3View             = findViewById(R.id.image_cloudWedge3);
        cloudWedge4View             = findViewById(R.id.image_cloudWedge4);
        cloudHalfLeftView           = findViewById(R.id.image_cloudHalf_left);
        cloudHalfRightView          = findViewById(R.id.image_cloudHalf_right);

        windFlagView                = findViewById(R.id.image_windFlag);
        windFlag1View               = findViewById(R.id.image_windFlag1);
        windFlag2View               = findViewById(R.id.image_windFlag2);
        windFlag3View               = findViewById(R.id.image_windFlag3);
        windFlag4View               = findViewById(R.id.image_windFlag4);
        windFlag5View               = findViewById(R.id.image_windFlag5);
    }

    @Override
    protected void onStart() {
        super.onStart();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float logicalDensity = displayMetrics.density;
        shortFlagPx = (int)Math.ceil(40 * logicalDensity);
        longFlagPx = (int)Math.ceil(80 * logicalDensity);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(checkPermission()){
            getLocation();
        }
        else{
            requestPermission();
        }
    }

    private void getLocation(){
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            new FetchWeather().execute();
                        }
                    }
                });
    }

    private boolean checkPermission(){
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        Log.i("Permission", " request");
        if(requestCode == REQUEST_PERMISSIONS_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLocation();
            }
        }
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

                precipitationView.setText("");
                resetClouds();
                resetWinds();

                updateCity(jsonResponse);
                updateCoordinates(jsonResponse);
                updateSky(jsonResponse);
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

                for(int i=weather.length()-1; i>=0; i--){
                    JSONObject condition = weather.getJSONObject(i);

                    int conditionId = condition.getInt("id");

                    if(conditionId>=200 && conditionId<300){
                        //thunderstorm
                        precipitationView.setText("\u2608");
                    }
                    else if(conditionId>=300 && conditionId<400){
                        // drizzle
                        precipitationView.setText("\u275F");
                    }
                    else if(conditionId>=500 && conditionId<600){
                        // rain
                        precipitationView.setText("\u2022");
                        if(conditionId>=520){
                            precipitationView.setText("\u25BD"); //shower
                        }
                        if(conditionId==511){
                            precipitationView.setText("\u223E"); //freezing rain
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
                            precipitationView.setText("\u2261"); //fog
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

                temperatureView.bringToFront();
                humidityView.bringToFront();
                pressureView.bringToFront();
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }

        private void updateSky(JSONObject jsonResponse){
            try{
                JSONObject sys = jsonResponse.getJSONObject("sys");

                int sunriseTime = sys.getInt("sunrise");
                int sunsetTime = sys.getInt("sunset");
                long currentTime = System.currentTimeMillis()/1000;

                int topColor, bottomColor;
                if(Math.abs(sunriseTime-currentTime) < 3600){
                    topColor = Color.parseColor("#1673FF");
                    bottomColor = Color.parseColor("#EDFF8C");
                }
                else if(Math.abs(sunsetTime-currentTime) < 3600){
                    topColor = Color.parseColor("#7C5EB5");
                    bottomColor = Color.parseColor("#FFC97F");
                }
                else if(currentTime<sunsetTime && currentTime>sunriseTime){
                    topColor = Color.parseColor("#074B99");
                    bottomColor = Color.parseColor("#C5D8E5");
                }
                else{
                    topColor = Color.parseColor("#000F21");
                    bottomColor = Color.parseColor("#002051");
                }

                GradientDrawable skyGradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[] {bottomColor, topColor});
                skyGradient.setShape(GradientDrawable.RECTANGLE);
                skyGradient.setCornerRadius(0f);

                View skyBackground = findViewById(R.id.mainlayout);
                skyBackground.setBackground(skyGradient);

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

        private void resetClouds(){
            cloudLineVerticalView.setAlpha((float)0);
            cloudLineHorizontalView.setAlpha((float)0);
            cloudWedge1View.setAlpha((float)0);
            cloudWedge2View.setAlpha((float)0);
            cloudWedge3View.setAlpha((float)0);
            cloudWedge4View.setAlpha((float)0);
            cloudHalfLeftView.setAlpha((float)0);
            cloudHalfRightView.setAlpha((float)0);
        }

        private void resetWinds(){
            windFlagView.setAlpha((float)0);
            windFlag1View.setAlpha((float)0);
            windFlag2View.setAlpha((float)0);
            windFlag3View.setAlpha((float)0);
            windFlag4View.setAlpha((float)0);
            windFlag5View.setAlpha((float)0);

            windFlag2View.getLayoutParams().width = shortFlagPx;
            windFlag3View.getLayoutParams().width = shortFlagPx;
            windFlag4View.getLayoutParams().width = shortFlagPx;
            windFlag5View.getLayoutParams().width = shortFlagPx;
        }
    }
}
