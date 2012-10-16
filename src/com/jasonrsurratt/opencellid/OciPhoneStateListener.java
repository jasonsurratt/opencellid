/*
 * Copyright (C) 2012 Jason R. Surratt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
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
