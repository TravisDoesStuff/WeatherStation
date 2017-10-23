package tnburt.weatherstation;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    static final String API_KEY = ""; //Insert API key here
    static final double hPa_TO_mmHg = 0.029529983071445;

    private TextView cityView;
    private TextView coordView;
    private TextView temperatureView;
    private TextView humidityView;
    private TextView pressureView;

    private ImageView cloudLineVerticalView;
    private ImageView cloudLineHorizontalView;
    private ImageView cloudWedge1View;
    private ImageView cloudWedge2View;
    private ImageView cloudWedge3View;
    private ImageView cloudWedge4View;
    private ImageView cloudHalfLeftView;
    private ImageView cloudHalfRightView;

    String coordinates;
    int temperature;
    int humidity;
    double barometer;
    double windSpeed;
    int windDirection;
    int cloudCover;

    String city = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityView            = (TextView) findViewById(R.id.text_city);
        coordView           = (TextView) findViewById(R.id.text_coord);
        temperatureView     = (TextView) findViewById(R.id.text_temperature);
        humidityView        = (TextView) findViewById(R.id.text_humidity);
        pressureView        = (TextView) findViewById(R.id.text_pressure);

        cloudLineVerticalView       = (ImageView) findViewById(R.id.image_cloudVertical);
        cloudLineHorizontalView     = (ImageView) findViewById(R.id.image_cloudHorizontal);
        cloudWedge1View             = (ImageView) findViewById(R.id.image_cloudWedge1);
        cloudWedge2View             = (ImageView) findViewById(R.id.image_cloudWedge2);
        cloudWedge3View             = (ImageView) findViewById(R.id.image_cloudWedge3);
        cloudWedge4View             = (ImageView) findViewById(R.id.image_cloudWedge4);
        cloudHalfLeftView           = (ImageView) findViewById(R.id.image_cloudHalf_left);
        cloudHalfRightView          = (ImageView) findViewById(R.id.image_cloudHalf_right);

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
            String lat = "";
            String lon = "";

            try{
                URL url = new URL("http://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&units="+units+"&appid="+API_KEY);
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
                JSONObject coord = new JSONObject(response).getJSONObject("coord");
                JSONObject main = new JSONObject(response).getJSONObject("main");
                JSONObject wind = new JSONObject(response).getJSONObject("wind");
                JSONObject clouds = new JSONObject(response).getJSONObject("clouds");

                coordinates = "Lat: " + coord.getString("lat") + ", Long: " + coord.getString("lon");

                temperature = main.getInt("temp");
                humidity = main.getInt("humidity");
                barometer = main.getDouble("pressure") * hPa_TO_mmHg;

                windSpeed = wind.getDouble("speed");
                windDirection = wind.getInt("deg");

                cloudCover = clouds.getInt("all");

                cloudLineVerticalView.setAlpha((float)0);
                cloudLineHorizontalView.setAlpha((float)0);
                cloudWedge1View.setAlpha((float)0);
                cloudWedge2View.setAlpha((float)0);
                cloudWedge3View.setAlpha((float)0);
                cloudWedge4View.setAlpha((float)0);
                cloudHalfLeftView.setAlpha((float)0);
                cloudHalfRightView.setAlpha((float)0);

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
                } else if(cloudCover >= 80 && cloudCover < 95){
                    cloudHalfLeftView.setAlpha((float)1);
                    cloudHalfRightView.setAlpha((float)1);
                } else if(cloudCover >= 95){
                    cloudWedge1View.setAlpha((float)1);
                    cloudWedge2View.setAlpha((float)1);
                    cloudWedge3View.setAlpha((float)1);
                    cloudWedge4View.setAlpha((float)1);
                }

                cityView.setText(city);
                coordView.setText(coordinates);
                temperatureView.setText(String.valueOf(temperature));
                humidityView.setText(String.valueOf(humidity));
                pressureView.setText(String.valueOf(barometer));
            }
            catch(JSONException e){
                e.printStackTrace();
            }

        }
    }
}
