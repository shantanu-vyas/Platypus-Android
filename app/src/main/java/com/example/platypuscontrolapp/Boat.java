package com.example.platypuscontrolapp;

import java.net.InetSocketAddress;

import edu.cmu.ri.crw.FunctionObserver;
import edu.cmu.ri.crw.PoseListener;
import edu.cmu.ri.crw.SensorListener;
import edu.cmu.ri.crw.WaypointListener;
import edu.cmu.ri.crw.data.Twist;
import edu.cmu.ri.crw.data.Utm;
import edu.cmu.ri.crw.data.UtmPose;
import edu.cmu.ri.crw.udp.UdpVehicleServer;

import org.jscience.geography.coordinates.*;

import robotutils.Pose3D;

import com.google.android.gms.maps.model.LatLng;

public class Boat
	{
        UdpVehicleServer server = null;
		private String name;
		private InetSocketAddress ipAddress;
		private Twist tw = null;
		private PoseListener pl;
		private UtmPose pose;
		private double xValue;
		private double yValue;
		private double zValue;
		private boolean connected;
		private WaypointListener waypointListen;
		private UtmPose _waypoint = new UtmPose();
		private final Object _waypointLock = new Object();
		private String boatLog = "";

		public Boat()
			{
                server = new UdpVehicleServer();
			}

		public Boat(InetSocketAddress _ipAddress)
			{
				ipAddress = _ipAddress;
				server = new UdpVehicleServer();
				server.setVehicleService(ipAddress);
				tw = new Twist();
			}

		public void setAddress(InetSocketAddress a)
			{
				server.setVehicleService(a);
			}

		public InetSocketAddress getIpAddress() {
            return (InetSocketAddress) server.getVehicleService();
        }

		public void getPose()
			{
				PoseListener pl = new PoseListener()
					{

						public void receivedPose(UtmPose upwcs)
							{
								UtmPose _pose = upwcs.clone();
									{
										// random = "" + _pose.pose.getX() +
										// "\n" + _pose.pose.getY() + "\n" +
										// _pose.pose.getZ();
										xValue = _pose.pose.getX();
										yValue = _pose.pose.getY();
										zValue = _pose.pose.getZ();
										// _pose.origin.

									}
							}
					};
				ConnectScreen.boat.returnServer().addPoseListener(pl, null);

			}

		public LatLng getLatLngLocation()
			{
				return null;
			}

		public double getRotation()
			{
				return (Double) null;
			}

		public double getThrust()
			{
				return tw.dx() / .010;
			}

		public double getRudder()
			{
				return tw.drz() / .010;
			}

		public void setVelocity(int thrust, int rudder)
			{
				tw.dx(thrust * .010);
				tw.drz(rudder * .010);
				server.setVelocity(tw, null);
			}

		public UdpVehicleServer returnServer()
			{
				return server;
			}

		public double getPoseX()
			{
				return xValue;
			}

		public double getPoseY()
			{
				return yValue;
			}

		public double getPoseZ()
			{
				return zValue;
			}

		public boolean isConnected()
			{
				server.isConnected(new FunctionObserver<Boolean>()
					{
						public void completed(Boolean v)
							{
								connected = true;
							}

						public void failed(FunctionError fe)
							{
								connected = false;
							}
					});
				return connected;

			}

		public void initWaypointListener()
			{
				// waypointListen = new WaypointListener();
			}

		public void addWaypoint(Pose3D _pose, Utm _origin)
			{
				if (server == null)
					return;

				UtmPose[] wpPose = new UtmPose[1];
				// synchronized (_waypointLock)
				// {
				// wpPose[0] = _waypoint;
				// }
				//

				wpPose[0] = new UtmPose(_pose, _origin);
				server.startWaypoints(wpPose, "POINT_AND_SHOOT", new FunctionObserver<Void>()
					{
						public void completed(Void v)
							{
							}

						public void failed(FunctionError fe)
							{
							}
					});
			}

		public void moveWaypoint()
			{
			}

		public void cancelWaypoint()
			{
				server.stopWaypoints(new FunctionObserver<Void>()
							{
								public void completed(Void v)
									{
										
									}
								public void failed(FunctionError fe)
									{
										
									}
							});
			}
		public void addToBoatLog(String s)
			{
				boatLog = boatLog + s  + "\n";
			}
	}
