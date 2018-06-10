package com.kodukootud.kaarelkaine.teekilomeeter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    //UI jaoks vajalik
    private Button buttonGPS;
    private Button buttonPildista;
    private TextView textView;
    //GPSi leidmiseks vajalik
    private LocationManager locationManager;
    private LocationListener listener;

    //Koordinaadid
    private Double myLatitude = 58.29250;
    private Double myLongitude = 26.72306;

    //HTMLi laadimiseks vajalik
    private String htmlPageUrl = "https://teed.jairus.ee/teed.php?k=58.292502,26.723066";
    private Document htmlDocument;
    private TextView parsedHtmlNode;
    private String htmlContentInStringFormat;

    Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Ülemise bar-i peitmine
        getSupportActionBar().hide();
        //Ainult landscape mode
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        //Tekstiväli
        textView = (TextView) findViewById(R.id.coordinates);
        parsedHtmlNode = (TextView)findViewById(R.id.roadData);
        //Nupp:s
        buttonGPS = (Button) findViewById(R.id.buttonGPS);
        //GPS:
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //Aja formaat:
                String fDate = getDate();
                //Koordinaadid olemas, nüüd oleks vaja tee asukoht leida siin peale seda
                //Longitude=26.6....
                //Latitude=58.3....
                myLatitude=roundCoordinate(location.getLatitude());
                myLongitude=roundCoordinate(location.getLongitude());
                htmlPageUrl = "https://teed.jairus.ee/teed.php?k="+ myLatitude +","+myLongitude;
                JsoupAsyncTask jsoupAsyncTask = new JsoupAsyncTask();
                jsoupAsyncTask.execute();
                textView.setText("Koordinaadid: " +myLatitude+ " " +myLongitude+" "+ fDate);

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        configure_button();

        //Paneme GPSi asukohta otsima
        //locationManager.requestLocationUpdates("gps", 5000, 0, listener);

    }

    //ÄPPI pausile panekul: suletakse gps ja kaamera
    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(listener);
//        mCamera.stopPreview();
    }

    //ÄPPI kinni panekul: sulgeda gps ja kaamera
    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(listener);
//        mCamera.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                configure_button();
                break;
            default:
                break;
        }
    }

    void configure_button() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            return;
        }
        // this code won'textView execute IF permissions are not allowed, because in the line above there is return statement.
        buttonGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection MissingPermission
                locationManager.requestLocationUpdates("gps", 0, 0, listener);
            }
        });
    }

    private class JsoupAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                htmlDocument = Jsoup.connect(htmlPageUrl).get();
                String htmlDocString = htmlDocument.toString();

                //Asendame <br>id mingi muud märgiga
                String temp = htmlDocString.replace("<br>", "$$$");
                Document doc1 = Jsoup.parse(temp);
                //Asendame märgid reavahetusega
                String text = doc1.body().text().replace("$$$", "\n").toString();
                //Jupitame stringi vastavalt ridade vahetusele ära
                String lines[] = text.split("\\r?\\n");

                //Meetrid->number->kilomeetriteks
                String b = lines[1].replaceAll("Meeter: ", "");
                float number = Float.parseFloat(b);
                float kilomeeter=number/1000;

                String kilomeeterString=Float.toString(kilomeeter);
                String kilomeeterPunktiga=kilomeeterString.replace('.', ',');

                //Kogu data ühte lausesse kokku
                //String roadData=lines[0]+"km "+kilomeeter+" "+lines[3];

                String roadData=lines[0]+"\nkm "+kilomeeterPunktiga;
                //Float.toString(kilomeeter)
                htmlContentInStringFormat=roadData;

                //htmlContentInStringFormat=lines[1];

                System.out.println(text);


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            parsedHtmlNode.setText(htmlContentInStringFormat);
        }
    }

    //Aeg stringina
    public static String getDate() {
        Date cDate = new Date();
        String fDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(cDate);
        return fDate;
    }

    //Koordinaatide ümardamine, annad pika sisse ja välja laseb ümardatud 5 kohalise
    public static double roundCoordinate(double coordinate) {
        double longCoordinate=coordinate*100000;
        double roundedLongCoordinate=Math.round(longCoordinate);
        double roundedCoordinate=roundedLongCoordinate/100000;
        return roundedCoordinate;
    }

}