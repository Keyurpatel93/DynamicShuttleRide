package edu.msu.cse.patelke6.DynamicShuttleRide;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QRCodeActivity extends Activity {

    private String riderName;
    private TextView passengerCountView;
    private TextView destinationView;
    private TextView pickUpLocation;
    private TextView arrivalTimeView;
    private ImageButton removeRideBtn;
    private String rideID;
    private String jsonFilePath;
    private  JSONObject rideJson = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_qrcode);

        Bundle extras = getIntent().getExtras();
        jsonFilePath = extras.getString("filePath");

        try {
            rideJson = new JSONObject(extras.getString("jsonObject"));

            rideID = rideJson.getString(JSONKeyNames.RideID);
            riderName = rideJson.getString(JSONKeyNames.RiderName);
            passengerCountView = (TextView) findViewById(R.id.passengerCountView);
            passengerCountView.setText(rideJson.getString(JSONKeyNames.PassengerCount));
            destinationView = (TextView) findViewById(R.id.destinationView);
            destinationView.setText(rideJson.getString(JSONKeyNames.Destination));
            pickUpLocation = (TextView) findViewById(R.id.pickUpLocationView);
            pickUpLocation.setText(rideJson.getString(JSONKeyNames.PickUp));
            arrivalTimeView = (TextView) findViewById(R.id.etaTimeView);

            //todo change to boarding
            SimpleDateFormat df = new SimpleDateFormat("HH:mm aa");
            String formattedDate = df.format(rideJson.getLong(JSONKeyNames.BoardingTime));

            arrivalTimeView.setText(formattedDate);
            removeRideBtn = (ImageButton) findViewById(R.id.removeRideBtn);
            removeRideBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeRide();
                }
            });

            ImageView imageView = (ImageView) findViewById(R.id.QRCodeView);
            try {
                Bitmap bitmap = encodeAsBitmap(rideJson.toString());
                imageView.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Log.i("QR Activity", "WriterException " + e);
                onError();
            }


        }catch (JSONException ex){
            Log.i("QR Activity", "" + ex.toString());
            onError();
        }
}


    private void onError(){
        Toast.makeText(this,"Error, Please rebook the trip", Toast.LENGTH_SHORT).show();
        Intent myIntent = new Intent(this, MainActivity.class);
        this.startActivity(myIntent);
    }

    //Generates QR Code for the inputted string and return the bitmap.
    private Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, 400 , 400, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            Log.e("QR Code", "error" + iae);
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 400, 0, 0, w, h);
        return bitmap;
    }

    private void removeRide() {
        File ridesJsonFile = new File(jsonFilePath);
        String jsonString = readJsonFromFile(ridesJsonFile);

        if (jsonString != null) {
            try {
                JSONObject ridesJson = new JSONObject(jsonString);
                JSONArray ridesArray = ridesJson.getJSONArray(JSONKeyNames.RootKey);

                for (int i = 0; i < ridesArray.length(); i++) {
                    JSONObject rideJson = ridesArray.getJSONObject(i);
                    if (rideID.equals(rideJson.getString("RideID"))) {
                        ridesArray.remove(i);
                        break;
                    }
                }

                FileWriter file = new FileWriter(ridesJsonFile, false);
                file.write(ridesJson.toString());
                file.flush();
                file.close();

                //Todo when clicking back button do not display this activity
                Intent myIntent = new Intent(this, DynamicShuttleRideActivity.class);
                myIntent.putExtra("Name", riderName);
                this.startActivity(myIntent);
            } catch (Exception e) {
                Log.i("RemoveRide", e.toString());

            }
        }
    }

    private String readJsonFromFile(File file){
        String json = null;
        try {
            InputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Log.i("readJsonFromFile", ex.toString());
            return null;
        }
        return json;
    }

}

