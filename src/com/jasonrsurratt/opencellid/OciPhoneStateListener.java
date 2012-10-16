package com.jasonrsurratt.opencellid;

import java.security.Provider.Service;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class OciPhoneStateListener extends PhoneStateListener {
	
	private Updatable _listener;
	
	private float _rssi = Float.MAX_VALUE;
	private int _asu = Integer.MAX_VALUE;
	private TelephonyManager _tm;
	
	
	OciPhoneStateListener(Updatable listener, TelephonyManager tm)
	{
		_tm = tm;
		_listener = listener;
	}
	
	public boolean isAsuValid()
	{
		return _asu != Integer.MAX_VALUE;
	}
	
	public int getAsu()
	{
		return _asu;
	}
	
	public boolean isRssiValid()
	{
		return _rssi != Float.MAX_VALUE;
	}
	
	public float getRssi()
	{
		return _rssi;
	}
	
	@Override
	public void onCellLocationChanged(CellLocation location)
	{
		_listener.update();
	}
	
	@Override
	public void onServiceStateChanged(ServiceState ss)
	{
		_listener.update();
	}
	
	@Override
	public void onSignalStrengthChanged(int asu)
	{
		_asu = asu;
		if (_tm.getCellLocation() instanceof GsmCellLocation)
		{
			_rssi = -113 + 2 * asu;
		}
		else
		{
			_rssi = Float.MAX_VALUE; 
		}
		_listener.update();
	}
}
