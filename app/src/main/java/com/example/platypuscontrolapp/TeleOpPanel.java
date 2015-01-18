package com.example.platypuscontrolapp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import robotutils.Pose3D;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.ImageListener;
import edu.cmu.ri.crw.PoseListener;
import edu.cmu.ri.crw.FunctionObserver.FunctionError;
import edu.cmu.ri.crw.VehicleServer.WaypointState;
import edu.cmu.ri.crw.VelocityListener;
import edu.cmu.ri.crw.WaypointListener;
import edu.cmu.ri.crw.data.Twist;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;

//import com.google.android.maps.GeoPoint;

public class TeleOpPanel extends Activity implements SensorEventListener {

	SeekBar thrust = null;
	SeekBar rudder = null;
	TextView ipAddressBox = null;
	TextView thrustProgress = null;
	TextView rudderProgress = null;
	LinearLayout linlay = null;
	CheckBox autonomous = null;
	Button mapButton = null;
	static TextView testIP = null;
	AsyncTask networkThread;
	TextView test = null;
	ToggleButton tiltButton = null;
	ToggleButton waypointButton = null;
	Button deleteWaypoint = null;
	TextView log = null;
	Handler network = new Handler();
	ImageView cameraStream = null;
	boolean checktest;
	int a = 0;

	double xValue;
	double yValue;
	double zValue;
	LatLong latlongloc;
	LatLng boatLocation;

	GoogleMap map;
	String zone;
	String rotation;

	TextView loca = null;
	Marker boat;
	Marker boat2;
	LatLng pHollowStartingPoint = new LatLng((float) 40.436871,
			(float) -79.948825);

	double lat = 10;
	double lon = 10;

	Handler handlerRudder = new Handler();
	int thrustCurrent;
	int rudderCurrent;
	double heading = Math.PI / 2.;
	int rudderTemp = 50;
	int thrustTemp = 0;
	double temp;
	double rot;
	String boatwaypoint;
	double tempThrustValue = 0; //used for abs value of thrust
	Twist twist = new Twist();

	float tempX = 0;
	float tempY = 0;

	Bitmap currentImage = null;
	
	SensorManager senSensorManager;
	Sensor senAccelerometer;

	private long lastUpdate = 0;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 600;

	public static final double THRUST_MIN = 0.0;
	public static final double THRUST_MAX = 1.0;
	public static final double RUDDER_MIN = 1.0;
	public static final double RUDDER_MAX = -1.0;

	List<LatLng> waypointList = new ArrayList(); //List of all upcoming waypoints
	List<Marker> markerList = new ArrayList(); //List of all the markers on the map corresponding to the given waypoints

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//this.setContentView(R.layout.teleoppanel);
        this.setContentView(R.layout.tabletlayout);
		// initBoat();
		ipAddressBox = (TextView) this.findViewById(R.id.printIpAddress);
		thrust = (SeekBar) this.findViewById(R.id.thrustBar);
		rudder = (SeekBar) this.findViewById(R.id.rudderBar);
		linlay = (LinearLayout) this.findViewById(R.id.linlay);
		thrustProgress = (TextView) this.findViewById(R.id.getThrustProgress);
		rudderProgress = (TextView) this.findViewById(R.id.getRudderProgress);
		// test = (TextView) this.findViewById(R.id.test12);
		tiltButton = (ToggleButton) this.findViewById(R.id.tiltButton);
		waypointButton = (ToggleButton) this.findViewById(R.id.waypointButton);
		deleteWaypoint = (Button) this.findViewById(R.id.waypointDeleteButton);
		log = (TextView) this.findViewById(R.id.log);
		//cameraStream = (ImageView) this.findViewById(R.id.cameraStream);
		// autonomous = (CheckBox) this.findViewById(R.id.Autonomous);


		senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		senAccelerometer = senSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		senSensorManager.registerListener(this, senAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		
		//cameraStream.setImageResource(R.drawable.streamnotfound);
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();


        /*
        * This gets called when a boat is connected
        * Note it has to draw the boat somewhere initially until it gets a gps loc so it draws it
        * on PantherHollow lake until it gets a new gps loc and will then update to the current
        * position
         */
		if (ConnectScreen.getBoatType() == true) {
			boat2 = map.addMarker(new MarkerOptions()
					.anchor(.5f, .5f)
					.flat(true)
					.rotation(270)
					.title("Boat 1")
					.snippet(ConnectScreen.boat.getIpAddress().toString())
					.position(pHollowStartingPoint) //draws at panther hollow initially
					//.icon(BitmapDescriptorFactory
					//		.fromResource(R.drawable.airboat))
					);
			
			//set camera to panther hollow until gps is found
			map.moveCamera(CameraUpdateFactory.newLatLngZoom( //moves the view to panther hollow
					pHollowStartingPoint, 14));
			map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

			//waypoint on click listener
            /*
             * if the add waypoint button is pressed and new marker where ever they click
             */
			map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
				@Override
				public void onMapClick(LatLng point) {
					// TODO Auto-generated method stub
					if (waypointButton.isChecked()) {
						waypointList.add(point);
						// UtmPose temp = convertLatLngUtm(point);
						// ConnectScreen.boat.addWaypoint(temp.pose,temp.origin);

						Marker tempMarker = map.addMarker(new MarkerOptions()
								.position(point));
						markerList.add(tempMarker);
						// map.addMarker(new MarkerOptions().position(point));

					}
				}
			});
            /*
             * If they press delete wayponts delete all markers off the map and delete waypoints
             */
			deleteWaypoint.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// ConnectScreen.boat.cancelWaypoint();
					for (Marker i : markerList) {
						i.remove();
					}
					waypointList.clear();
					// Perform action on click
				}
			});
			networkThread = new NetworkAsync().execute(); //launch networking asnyc task
		}

        /*
         * if its a simulated boat run the code for that
         */
		if (ConnectScreen.getBoatType() == false) {
			ipAddressBox.setText("Simulated Phone");
			simulatedBoat();
		}

		thrust.setProgress(0); //initially set thrust to 0
		rudder.setProgress(50); //initially set rudder to center (50)

		ipAddressBox.setText(ConnectScreen.boat.getIpAddress().toString());
		// setVehicle();
		// test.setText(ConnectScreen.boat.getPose());
		// connectScreen.boat.getPose();

	}

	@Override
	public void onPause() {
		super.onPause();
		// turns the thrust and rudder off when you pause the activity
		thrust.setProgress(0);
		rudder.setProgress(50);
		//networkThread.cancel(true);
	}

	 @Override 
	 public void onResume()
	 {
	 super.onResume();
	 //Intent intent = new Intent(this, TeleOpPanel.class);
	 //startActivity(intent);
	 if (networkThread.isCancelled()) //figure out how to resume asnyc task?
	 	{
	 	//	networkThread.execute();
	 	}
	 }
	
	public static boolean validIP(String ip) {
		if (ip == null || ip == "")
			return false;
		ip = ip.trim();
		if ((ip.length() < 6) & (ip.length() > 15))
			return false;

		try {
			Pattern pattern = Pattern
					.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
			Matcher matcher = pattern.matcher(ip);
			return matcher.matches();
		} catch (PatternSyntaxException ex) {
			return false;
		}
	}

	public void updateVelocity(Boat a) { //taken right from desktop client for updating velocity
		// ConnectScreen.boat.setVelocity(thrust.getProgress(),
		// rudder.getProgress());
		if (a.returnServer() != null) {
			//Twist twist = new Twist();
			twist.dx(fromProgressToRange(thrust.getProgress(), THRUST_MIN,
					THRUST_MAX));
			if(Math.abs((fromProgressToRange(rudder.getProgress(), RUDDER_MIN,RUDDER_MAX))-0) < .2)
			{
				tempThrustValue = 50;
				twist.drz(fromProgressToRange((int)tempThrustValue, RUDDER_MIN,
						RUDDER_MAX));
				
			}
			else
			{
			twist.drz(fromProgressToRange(rudder.getProgress(), RUDDER_MIN,
					RUDDER_MAX));
			}
			a.returnServer().setVelocity(twist, null);
		}
	}

    /*
     * this async task handles all of the networking on the boat since networking has to be done on
     * a different thread and since gui updates have to be updated on the main thread ....
     */
	private class NetworkAsync extends AsyncTask<String, Integer, String> {
		long oldTime = 0;
		String tester = "done";
		boolean connected = false;
		boolean firstTime = true;

		@Override
		protected String doInBackground(String... arg0) {
			PoseListener pl = new PoseListener() { //gets the location of the boat
				public void receivedPose(UtmPose upwcs) {
					UtmPose _pose = upwcs.clone();
					{
						xValue = _pose.pose.getX();
						yValue = _pose.pose.getY();
						zValue = _pose.pose.getZ();
						rotation = String.valueOf(Math.PI / 2
								- _pose.pose.getRotation().toYaw());
						rot = Math.PI / 2 - _pose.pose.getRotation().toYaw();

						zone = String.valueOf(_pose.origin.zone);

						latlongloc = UTM.utmToLatLong(UTM.valueOf(
								_pose.origin.zone, 'T', _pose.pose.getX(),
								_pose.pose.getY(), SI.METER),
								ReferenceEllipsoid.WGS84);

					}
				}
			};
            ConnectScreen.boat.returnServer().addPoseListener(pl, null);

			// waypoint checker || edit: idk what this is for?
			ConnectScreen.boat.returnServer().addWaypointListener(new WaypointListener() {
	            public void waypointUpdate(WaypointState ws) {
	            	boatwaypoint = ws.toString();
	                        }
	                    }, null);



			// setVelListener();
			while (true) { //constantly looping
				if (System.currentTimeMillis() % 100 == 0
						&& oldTime != System.currentTimeMillis()) {

					if (ConnectScreen.boat.isConnected() == true) {
						connected = true;
					} 
					if (ConnectScreen.boat.isConnected() == false) {
						connected = false;
					}

					if (thrust.getProgress() != thrustTemp) { //update velocity
						updateVelocity(ConnectScreen.boat);
					}

					if (rudder.getProgress() != rudderTemp) { //update rudder
						updateVelocity(ConnectScreen.boat);
					}
						
						//System.out.println("Waypoint list size: " + waypointList.size());
                    /*
                     * gets first waypoint in waypointlist array
                     * sends to boat
                     * deletes from array
                     */
						if (waypointList.size() >= 1)
						{
							checktest = true;
							UtmPose tempUtm = convertLatLngUtm(waypointList.get(0));
							ConnectScreen.boat.addWaypoint(tempUtm.pose, tempUtm.origin);
							UtmPose[] wpPose = new UtmPose[1];
							wpPose[0] = new UtmPose(tempUtm.pose, tempUtm.origin);
							ConnectScreen.boat.returnServer().startWaypoints(wpPose, "POINT_AND_SHOOT", null);
							waypointList.remove(0);
						}

						thrustTemp = thrust.getProgress();
						rudderTemp = rudder.getProgress();
						oldTime = System.currentTimeMillis();

					
					publishProgress();

				}
			}
		}

		@Override
		protected void onProgressUpdate(Integer... result) {
			
			//cameraStream.setImageBitmap(currentImage);

			try {
				//log.setText(a);
				//log.setText(String.valueOf(waypointList.get(0))+ "\n" + boatwaypoint +"\n Achieved Waypoint: "
						//+ (String.valueOf(ConnectScreen.boat.getCurrentWaypointStatus())));
				//log.setText(String.valueOf(waypointList.get(0))+"\n"+boatwaypoint);
				// log.setText(String.valueOf(ConnectScreen.boat.getCurrentWaypointStatus())
				// + "\n marker: " + waypointList.get(0).latitude + " " +
				// waypointList.get(1).longitude + "\n actual"+
				// latlongloc.toText());
				// log.setText(waypointList.toString());
				//a++;
								
				boat2.setPosition(new LatLng(latlongloc //draw the boat on the map!
						.latitudeValue(SI.RADIAN) * 57.2957795, latlongloc
						.longitudeValue(SI.RADIAN) * 57.2957795));
				// test.setText(String.valueOf(rot * 57.2957795));
				boat2.setRotation((float) (rot * 57.2957795)); //set boats rotation!
				if (firstTime == true) {
					//testCamera(); // does the camera even work!?!?!?!?

                    /*
                     * sets the view to where the boat is
                     * if there is not location it will crash (nullpointer)
                     */
					try {
						map.moveCamera(CameraUpdateFactory.newLatLngZoom(
								new LatLng(
										latlongloc.latitudeValue(SI.RADIAN) * 57.2957795,
										latlongloc.longitudeValue(SI.RADIAN) * 57.2957795),
								14));
						map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
						//////test camera
						
						a = 2;
						firstTime = false;
						ConnectScreen.boat.returnServer().addWaypointListener(new WaypointListener() {
				            public void waypointUpdate(WaypointState ws) {
				            	boatwaypoint = ws.toString();
				                        }
				                    }, null);
					} catch (Exception e) {
						firstTime = true;
					}
				}

				// boat2.setSnippet(String.valueOf(latlongloc.toText();
			} catch (Exception e) {
				// test.setText("x: " + xValue + "\n y: " +
				// yValue + "\n zone: " + zone + "\n rotation: "
				// + rotation + "\n"
				// + e.toString());

			}

			if (connected == true) {
				ipAddressBox.setBackgroundColor(Color.GREEN);
			} 
			if (connected == false){
				ipAddressBox.setBackgroundColor(Color.RED);
			}

			thrustProgress.setText(String.valueOf(fromProgressToRange(
					thrust.getProgress(), THRUST_MIN, THRUST_MAX)));
			rudderProgress.setText(String.valueOf(fromProgressToRange(
					rudder.getProgress(), RUDDER_MIN, RUDDER_MAX)));

		}
	}

	public void simulatedBoat() {
		boat2 = map.addMarker(new MarkerOptions().anchor(.5f, .5f) //add boat to panther hollow
				.rotation(270).title("Boat 1")
				.snippet("IP Address: 192.168.1.1")
				.position(pHollowStartingPoint).title("Boat 1")
				.snippet("127.0.0.1 (localhost)")
				//.icon(BitmapDescriptorFactory.fromResource(R.drawable.airboat))
				.flat(true));

		lat = pHollowStartingPoint.latitude;
		lon = pHollowStartingPoint.longitude;
		map.setMyLocationEnabled(true);
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(pHollowStartingPoint,
				15));
		map.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

		boat2.setRotation((float) (heading * (180 / Math.PI)));
		handlerRudder.post(new Runnable() { //control the boat
			@Override
			public void run() {
				if (thrust.getProgress() > 0) {
					lat += Math.cos(heading) * (thrust.getProgress() - 50)
							* .0000001;
					lon += Math.sin(heading) * (thrust.getProgress())
							* .0000001;
					heading -= (rudder.getProgress() - 50) * .001;
					boat2.setRotation((float) (heading * (180 / Math.PI)));
				}
				boat2.setPosition(new LatLng(lat, lon));
				handlerRudder.postDelayed(this, 200);
			}
		});
	}

	public void setVelListener() {
		ConnectScreen.boat.returnServer().addVelocityListener(
				new VelocityListener() {
					public void receivedVelocity(Twist twist) {
						thrust.setProgress(fromRangeToProgress(twist.dx(),
								THRUST_MIN, THRUST_MAX));
						rudder.setProgress(fromRangeToProgress(twist.drz(),
								RUDDER_MIN, RUDDER_MAX));
					}
				}, null);

	}

	// Converts from progress bar value to linear scaling between min and
	// max
	private double fromProgressToRange(int progress, double min, double max) {
		return (min + (max - min) * ((double) progress) / 100.0);
	}

	// Converts from progress bar value to linear scaling between min and
	// max
	private int fromRangeToProgress(double value, double min, double max) {
		return (int) (100.0 * (value - min) / (max - min));
	}

	/* accelerometer controls */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		Sensor mySensor = sensorEvent.sensor;
		if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = sensorEvent.values[0];
			float y = sensorEvent.values[1];
			float z = sensorEvent.values[2];

			long curTime = System.currentTimeMillis();

			if (tiltButton.isChecked()) {
				if ((curTime - lastUpdate) > 100) {
					long diffTime = (curTime - lastUpdate);
					lastUpdate = curTime;
					float speed = Math
							.abs(x + y + z - last_x - last_y - last_z)
							/ diffTime * 10000;

					if (speed > SHAKE_THRESHOLD) {
					}

					last_x = x; // rudder
					last_y = y;
					last_z = z; // thrust
					// test.setText("x: " + last_x + "y: " + last_y + "z: "
					// + last_z);

					updateViaAcceleration(last_x, last_y, last_z);
				}
			}
		}
	}

	public void updateViaAcceleration(float xval, float yval, float zval) { //update the thrust via accelerometers 
		if (Math.abs(tempX - last_x) > 2.5) {

			if (last_x > 2) {
				thrust.setProgress(thrust.getProgress() - 3);
			}
			if (last_x < 2) {
				thrust.setProgress(thrust.getProgress() + 3);
			}
		}
		if (Math.abs(tempY - last_y) > 1) {
			if (last_y > 2) {
				rudder.setProgress(rudder.getProgress() + 3);
			}
			if (last_y < -2) {
				rudder.setProgress(rudder.getProgress() - 3);
			}
		}
	}

	public void addWayPointFromMap() {
		// when you click you make utm pose... below is fake values
		Pose3D pose = new Pose3D(1, 1, 0, 0.0, 0.0, 10);
		Utm origin = new Utm(17, true);
		// ConnectScreen.boat.addWaypoint(pose, origin);
		UtmPose[] wpPose = new UtmPose[1];
		wpPose[0] = new UtmPose(pose, origin);
		ConnectScreen.boat.returnServer().startWaypoints(wpPose,
				"POINT_AND_SHOOT", new FunctionObserver<Void>() {
					public void completed(Void v) {
						log.setText("completed");
					}

					public void failed(FunctionError fe) {
						log.setText("failed");
					}
				});

		map.addMarker(new MarkerOptions().anchor(.5f, .5f).flat(true)
				.title("Boat 1").snippet("Waypoint")
				// .position(convertUtmLatLng(pose,origin))
				.position(pHollowStartingPoint).title("Current Waypoint")
				.snippet("127.0.0.1 (localhost)"));
	}

	public LatLng convertUtmLatLng(Pose3D _pose, Utm _origin) {
		LatLong temp = UTM
				.utmToLatLong(
						UTM.valueOf(_origin.zone, 'T', _pose.getX(),
								_pose.getY(), SI.METER),
						ReferenceEllipsoid.WGS84);
		return new LatLng(temp.latitudeValue(SI.RADIAN),
				temp.longitudeValue(SI.RADIAN));
	}

	public UtmPose convertLatLngUtm(LatLng point) {

		UTM utmLoc = UTM.latLongToUtm(LatLong.valueOf(point.latitude,
				point.longitude, NonSI.DEGREE_ANGLE), ReferenceEllipsoid.WGS84);

		// Convert to UTM data structure
		Pose3D pose = new Pose3D(utmLoc.eastingValue(SI.METER), utmLoc.northingValue(SI.METER), 0.0, 0, 0, 0);
		Utm origin = new Utm(utmLoc.longitudeZone(), utmLoc.latitudeZone() > 'O');
		UtmPose utm = new UtmPose(pose, origin);
		return utm;
	}
//	public void viewCamera()
//	{
//		ConnectScreen.boat.returnServer().addImageListener(new ImageListener() {
//			
//            public void receivedImage(byte[] imageData) {
//                // Take a picture, and put the resulting image into the panel
//                try {
//                	Bitmap image1 = BitmapFactory.decodeByteArray(imageData, 0, 15);
//                    BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
//                    if (image != null) {
//                        Image scaledImage = image.getScaledInstance(pictureLabel.getWidth(), pictureLabel.getHeight(), Image.SCALE_DEFAULT);
//                        pictureLabel.setIcon(new ImageIcon(scaledImage));
//                        CameraPanel.this.repaint();
//                    } else {
//                        System.err.println("Failed to decode image.");
//                    }
//                } catch (IOException ex) {
//                    System.err.println("Failed to decode image: " + ex);
//                }
//
//            }
//        }, null);
	public void testCamera()
	{
		//log.setText("test camera");
		ConnectScreen.boat.returnServer().addImageListener(new ImageListener() {
            public void receivedImage(byte[] imageData) {
                // Take a picture, and put the resulting image into the panel
            	//log.setText("image taken");	
            		
                try {
                	Bitmap image1 = BitmapFactory.decodeByteArray(imageData, 0, 15);
                    if (image1 != null)
                    {
                    	a++;
                    	//System.out.println("image made");
                    	currentImage = image1;
                    	
                    }
                }
                catch (Exception e)
                {
                log.setText(e.toString());
                	e.printStackTrace(); 
                }
            }
        }, null);
	}
}
// class