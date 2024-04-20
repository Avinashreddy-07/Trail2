package com.example.trail2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import android.webkit.WebViewClient;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.trail2.R;

import java.io.InputStream;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.trail2.NominatimService;
import com.example.trail2.NominatimResponse;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 1;
    private static final int PERMISSIONS_REQUEST_READ_MEDIA_IMAGES = 101;
    private TextView metadataTextView;
    private WebView mapView;
    private Button openMapButton;
    private String currentLatitude = "0.0"; // Placeholder for actual latitude
    private String currentLongitude = "0.0"; // Placeholder for actual longitude

    private Button hospitalsButton; // Added button for hospitals

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      Button selectImageButton = findViewById(R.id.select_image_button);
        metadataTextView = findViewById(R.id.metadata_text_view);
        mapView = findViewById(R.id.map_view);
        openMapButton = findViewById(R.id.open_map_button);
        hospitalsButton = findViewById(R.id.hospitals_button); // Initialize hospitals button

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
                } else {
                    openGallery();
                }
            }
        });

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLatitude != null && currentLongitude != null) {
                    mapView.setVisibility(View.VISIBLE);
                    WebSettings webSettings = mapView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setDomStorageEnabled(true);
                    webSettings.setAllowFileAccess(true);

                    mapView.loadUrl("file:///android_asset/map.html");
                    mapView.setWebChromeClient(new WebChromeClient());
                    mapView.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            Toast.makeText(MainActivity.this, "Error loading map: " + description, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            mapView.loadUrl("javascript:updateMapView(" + currentLatitude + ", " + currentLongitude + ")");
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "No GPS coordinates found to show on map.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        openMapButton.setOnClickListener(view -> {
            if (currentLatitude != null && currentLongitude != null) {
                mapView.setVisibility(View.VISIBLE);
                WebSettings webSettings = mapView.getSettings();
                webSettings.setJavaScriptEnabled(true);

                mapView.setWebChromeClient(new WebChromeClient());
                mapView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Toast.makeText(MainActivity.this, "Error loading map: " + description, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        mapView.loadUrl("javascript:updateMapView(" + currentLatitude + ", " + currentLongitude + ")");
                    }
                });

                mapView.loadUrl("file:///android_asset/map.html");
            } else {
                Toast.makeText(MainActivity.this, "No GPS coordinates found to show on map.", Toast.LENGTH_SHORT).show();
            }
        });
        hospitalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Call function to fetch nearby hospitals
                fetchNearbyHospitals();
            }
        });


    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                metadataTextView.setText("Permission denied to access your media images");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            displayMetadata(selectedImage);
        }
    }

    private String convertToDegree(String stringDMS, String ref) {
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        double degrees = Double.parseDouble(stringD[0]) / Double.parseDouble(stringD[1]);

        String[] stringM = DMS[1].split("/", 2);
        double minutes = Double.parseDouble(stringM[0]) / Double.parseDouble(stringM[1]);

        String[] stringS = DMS[2].split("/", 2);
        double seconds = Double.parseDouble(stringS[0]) / Double.parseDouble(stringS[1]);

        double result = degrees + (minutes / 60) + (seconds / 3600);
        if (ref.equals("S") || ref.equals("W")) {
            result = -result;
        }

        return String.valueOf(result);
    }
    private void displayMetadata(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (latitude != null && longitude != null && latitudeRef != null && longitudeRef != null) {
                currentLatitude = convertToDegree(latitude, latitudeRef);
                currentLongitude = convertToDegree(longitude, longitudeRef);
                metadataTextView.setText("Latitude: " + currentLatitude + "\nLongitude: " + currentLongitude);
            } else {
                metadataTextView.setText("GPS coordinates not found");
            }
        } catch (Exception e) {
            metadataTextView.setText("Failed to load metadata");
            e.printStackTrace();
        }
    }
    private void openMaps() {
        if (currentLatitude != null && currentLongitude != null) {
            // Open Maps with the current coordinates
            String geoUri = "geo:" + currentLatitude + "," + currentLongitude + "?q=" + currentLatitude + "," + currentLongitude;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } else {
            Toast.makeText(MainActivity.this, "No GPS coordinates found to open in Maps.", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchNearbyHospitals() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NominatimService service = retrofit.create(NominatimService.class);

        Call<NominatimResponse> call = service.searchNearbyHospitals(
                "hospitals",
                "json",
                1,
                10,
                "hospital"
        );

        call.enqueue(new Callback<NominatimResponse>() {
            @Override
            public void onResponse(Call<NominatimResponse> call, Response<NominatimResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NominatimResponse nominatimResponse = response.body();
                    String hospitalName = nominatimResponse.getHospitalName();
                    // Display hospital name or handle the response as needed
                    Toast.makeText(MainActivity.this, "Nearby Hospitals: " + hospitalName, Toast.LENGTH_SHORT).show();
                } else {
                    // Handle unsuccessful response
                    Toast.makeText(MainActivity.this, "Failed to fetch hospitals", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NominatimResponse> call, Throwable t) {
                // Handle failure
                Toast.makeText(MainActivity.this, "Failed to fetch hospitals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
