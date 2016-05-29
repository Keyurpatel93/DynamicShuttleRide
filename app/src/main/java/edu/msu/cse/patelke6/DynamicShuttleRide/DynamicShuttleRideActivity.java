package edu.msu.cse.patelke6.DynamicShuttleRide;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DynamicShuttleRideActivity extends Activity {

    private Button minusBtn;
    private Button plusBtn;
    private Button myRidesBtn;
    private Button bookRideBtn;
    private TextView passengerCountView;
    private TextView destinationView;
    private TextView myLocationView;
    private TextView greetingView;
    private int passengerCount;
    private String riderName;
    private String filePath;
    private Dialog ridesDialog;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Bundle extras = getIntent().getExtras();
        riderName = extras.getString("Name");
        setContentView(R.layout.activity_dynamic_shuttle);
        passengerCount= 1;


        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "outStandingRides.txt";

        destinationView = (TextView) findViewById(R.id.destinationView);
        myLocationView = (TextView) findViewById(R.id.myLocationView);
        destinationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocations(v);
            }
        });
        myLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocations(v);
            }
        });
        passengerCountView = (TextView) findViewById(R.id.passengerCountView);
        greetingView = (TextView) findViewById(R.id.greetingView);
        greetingView.setText("Good Morning, " + riderName+ "!");

        findViewById(R.id.textFrameView).setBackgroundColor(Color.parseColor("#00BFFF"));
        ImageView passengerView = (ImageView) findViewById(R.id.passengerImg);

        //Resize BMP to save heap space
        //Todo resize all BMPs to speed up app
        Bitmap passengerBmp = BitmapFactory.decodeResource(getResources(), R.drawable.passenger);
        passengerView.setImageBitmap(Bitmap.createScaledBitmap(passengerBmp,50,50, false));

        minusBtn = (Button) findViewById(R.id.minusBtn);
        minusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(passengerCount> 0){
                    passengerCount--;
                    Log.i("PassengerCount", " " + passengerCount);
                    passengerCountView.setText(Integer.toString(passengerCount));
                }
            }
        });

        plusBtn = (Button) findViewById(R.id.plusBtn);
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(passengerCount <5) {
                    passengerCount++;
                    Log.i("PassengerCount", " " + passengerCount);
                    passengerCountView.setText(Integer.toString(passengerCount));
                }
            }
        });

        bookRideBtn = (Button) findViewById(R.id.bookRideBtn);
        bookRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("DynamicShuttleRide", "Book Ride Clicked");
                bookRide();
            }
        });

        myRidesBtn = (Button) findViewById(R.id.myRidesBtn);
        myRidesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayOutstandingRides();
            }
        });

    }

    private void displayOutstandingRides(){
        String jsonString = readJsonFromFile();
        if(jsonString!=null) {
            try {
                JSONObject ridesJson = new JSONObject(jsonString);
                JSONArray ridesArray = ridesJson.getJSONArray(JSONKeyNames.RootKey);
                ArrayList<JSONObject> ridesList = new ArrayList<>();
                for (int i = 0; i < ridesArray.length(); i++) {
                    JSONObject rideJson = ridesArray.getJSONObject(i);
                    if(rideJson.getString(JSONKeyNames.RiderName).equals(riderName)) {
                        ridesList.add(rideJson);
                    }
                }

                if(ridesList.size() == 0) {
                    Toast.makeText(this, "No Existing Rides Planned", Toast.LENGTH_SHORT).show();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Select A Ride");
                    ListView rideList = new ListView(this);
                    rideList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            JSONObject rideSelected = (JSONObject) parent.getItemAtPosition(position);
                            Log.i("Ride Selected", "" + rideSelected.toString());
                            startQRCodeActivity(rideSelected);

                            ridesDialog.dismiss();
                        }
                    });
                    rideList.setAdapter(new RideListAdapter(this, R.layout.row_rides, ridesList));
                    builder.setNegativeButton("cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });


                    builder.setView(rideList);
                    ridesDialog = builder.create();
                    ridesDialog.show();
                }

            }catch (Exception e){
                Log.e("DisplayRides", e.toString());
            }

        }
    }

    private String readJsonFromFile(){
        String json = null;
        try {
            InputStream is = new FileInputStream(getOutStandingRidesFile());
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

    //Book a new ride
    private void bookRide(){

        if(destinationView.getText().toString().trim().length() == 0 || myLocationView.getText().toString().trim().length() == 0) {
            Toast.makeText(this, "Please select both destination and pick-up location", Toast.LENGTH_SHORT).show();
        } else if (destinationView.getText().equals(myLocationView.getText())) {
            Toast.makeText(this, "Please select different destination and pick-up locations", Toast.LENGTH_SHORT).show();
        }
        else {
            String ridesJsonString = readJsonFromFile();

            if (ridesJsonString != null) {
                try {
                    String rideID = generateUniqueIdentifier();
                    Date currentTime = new Date();

                    JSONObject ridesJson = new JSONObject(ridesJsonString);
                    JSONArray ridesJsonArray = ridesJson.getJSONArray("Rides");

                    JSONObject newRide = new JSONObject();
                    newRide.put(JSONKeyNames.RideID, rideID);
                    newRide.put(JSONKeyNames.RiderName, riderName);
                    newRide.put(JSONKeyNames.Destination, destinationView.getText());
                    newRide.put(JSONKeyNames.PickUp, myLocationView.getText());

                    newRide.put(JSONKeyNames.BoardingTime, (currentTime.getTime() + 200000));
                    newRide.put(JSONKeyNames.PassengerCount, passengerCount);
                    //600000 milliseconds = 10 minutes
                    newRide.put(JSONKeyNames.ArrivalTime, (currentTime.getTime() + 600000));


                    ridesJsonArray.put(newRide);

                    //Append ride to the outstanding rides file so can be retrieved at a later time
                    FileWriter file = new FileWriter(getOutStandingRidesFile(), false);
                    file.append(ridesJson.toString());
                    file.flush();
                    file.close();

                    Toast.makeText(this, riderName+ ", Your Ride Has Been Booked", Toast.LENGTH_LONG).show();

                    //Start Activity to show QR Code
                    startQRCodeActivity(newRide);

                } catch (Exception ex) {
                    Log.e("ShuttleActivity", ex.toString());
                }

            } else
                Toast.makeText(this, "Unable to Book Ride, Please try again later.", Toast.LENGTH_LONG).show();
        }

    }

    private String generateUniqueIdentifier(){
        final String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        return uuid;
    }


    //Show destination and current location options.
    private void showLocations(final View view){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Select Location");


        //Todo read values from a server or allow users to specify a location via google maps
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.select_dialog_item);
        arrayAdapter.add("iTek West");
        arrayAdapter.add("HQ");
        arrayAdapter.add("R&D");
        arrayAdapter.add("Building 1");
        arrayAdapter.add("Building 2");
        arrayAdapter.add("Building 3");
        arrayAdapter.add("Building 4");
        arrayAdapter.add("Building 5");
        arrayAdapter.add("Building 6");
        arrayAdapter.add("Building 7");
        arrayAdapter.add("Building 8");
        arrayAdapter.add("Building 9");
        arrayAdapter.add("Building 10");


        builderSingle.setNegativeButton( "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String location = arrayAdapter.getItem(which);
                        TextView textView = (TextView) view;
                        textView.setText(location);
                    }
                });
        builderSingle.show();

    }

    private File  getOutStandingRidesFile(){
        File ridesJson = new File(filePath);
        try{
            if (!ridesJson.exists()) {
                ridesJson.createNewFile();
                JSONObject jsonObject = new JSONObject();
                JSONArray jsonArray = new JSONArray();
                jsonObject.put(JSONKeyNames.RootKey, jsonArray);

                FileWriter file = new FileWriter(ridesJson, false);
                file.append(jsonObject.toString());
                file.flush();
                file.close();
                Log.i("Create File", "outStandingRides.json created");
            }

        }catch (Exception ex){
            Log.e("DynamicShuttleRide","Error Creating File: " + ex.toString());
            return null;
        }
        return ridesJson;
    }



    //Used to propagate the myRides dialog.
    private class RideListAdapter extends ArrayAdapter<JSONObject> {

        private List<JSONObject> mRides;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public RideListAdapter(Context context, int textViewResourceId,
                               ArrayList<JSONObject> objects) {
            super(context, textViewResourceId, objects);
            mRides = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_rides, null);
            }
            JSONObject ride = mRides.get(position);
            if (ride != null) {
                TextView pickUpView = (TextView) v.findViewById(R.id.pickUPLocationView);
                TextView destView = (TextView) v.findViewById(R.id.destinationView);
                TextView arrivalTime = (TextView) v.findViewById(R.id.arrivalTimeView);
                try{
                    if (pickUpView != null && destView != null && arrivalTime != null) {
                        pickUpView.setText(ride.getString(JSONKeyNames.PickUp));
                        destView.setText(ride.getString(JSONKeyNames.Destination));

                        SimpleDateFormat df = new SimpleDateFormat("HH:mm aa");
                        String formattedDate = df.format(ride.getLong(JSONKeyNames.BoardingTime));
                        arrivalTime.setText(formattedDate);
                    }
                } catch (Exception ex){
                    Log.e("UserListAdapter",ex.toString());
                }
            }

            return v;

        }
    }

    private void startQRCodeActivity(JSONObject ride){
        try {
            Intent myIntent = new Intent(this, QRCodeActivity.class);
            myIntent.putExtra("filePath", filePath);
            myIntent.putExtra("jsonObject", ride.toString());
            this.startActivity(myIntent);
        } catch (Exception e){
            Log.e("Start QR Activity",e.toString());
        }


    }
}


