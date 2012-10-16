package com.jasonrsurratt.opencellid;

import java.util.Map;
import java.util.TreeMap;

public class Observation implements Cloneable {
	private double x, y, accuracy;
	private long locationTime, time;
	private Map<String, String> kvp = new TreeMap<String, String>();
	
	@Override
	public Observation clone()
	{
		Observation result = new Observation();
		result.x = x;
		result.y = y;
		result.accuracy = accuracy;
		result.locationTime = locationTime;
		result.time = time;
		result.kvp = new TreeMap<String, String>();
		result.kvp.putAll(kvp);
		
		return result;
	}
	
	public boolean isNeighbor()
	{
		String n = kvp.get("Neighbor");
		return n != null && n.equals("true");
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public long getLocationTime() {
		return locationTime;
	}

	public void setLocationTime(long locationTime) {
		this.locationTime = locationTime;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Map<String, String> getKvp() {
		return kvp;
	}

	public void setKvp(Map<String, String> kvp) {
		this.kvp = kvp;
	}
}
