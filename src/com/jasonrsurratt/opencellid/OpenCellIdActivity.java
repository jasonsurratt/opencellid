package com.jasonrsurratt.opencellid;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class OpenCellIdActivity extends Activity implements LocationListener,
		Updatable {
	public static String convertNetworkType(int nt) {
		switch (nt) {
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			return "1xRTT";
		case TelephonyManager.NETWORK_TYPE_CDMA:
			return "CDMA";
		case TelephonyManager.NETWORK_TYPE_EDGE:
			return "EDGE";
			// Doesn't work with Android 2.2
			// case TelephonyManager.NETWORK_TYPE_EHRPD:
		case 14:
			return "eHRPD";
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			return "EVDO revision 0";
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			return "EVDO revision A";
			// Doesn't work with Android 2.2
			// case TelephonyManager.NETWORK_TYPE_EVDO_B:
		case 12:
			return "EVDO revision B";
		case TelephonyManager.NETWORK_TYPE_GPRS:
			return "GPRS";
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSPA:
			return "HSPA";
			// Doesn't work with Android 2.2
			// case TelephonyManager.NETWORK_TYPE_HSPAP:
		case 15:
			return "HSPA+";
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			return "HSUPA";
		case TelephonyManager.NETWORK_TYPE_IDEN:
			return "iDen";
			// Doesn't work with Android 2.2
			// case TelephonyManager.NETWORK_TYPE_LTE:
		case 13:
			return "LTE";
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return "UMTS";
		case TelephonyManager.NETWORK_TYPE_UNKNOWN:
			return "Unknown";
		}
		return "Unexpected Value: " + Integer.toString(nt);
	}

	private String _text;
	private String _bestProvider;
	private String _debug;
	private Location _lastLocation = null;
	private long _lastLocationTime, _lastCollect = 0;
	private Location _lastCollectLocation;
	private TextView _tv;
	private TelephonyManager _tm;
	private OciPhoneStateListener _phoneState;
	private OpenCellIdLog _log;
	/**
	 * The maximum time difference on a location fix for a sample in
	 * milliseconds.
	 */
	private long MAX_STALE_LOCATION = 1000;
	private double MIN_ACCURACY = 100;
	/**
	 * The approximate cell size of the final map in meters.
	 */
	private double FINAL_CELL_SIZE = 30;
	/**
	 * The desired number of observations per sample
	 */
	private int DESIRED_OBSERVATIONS_PER_CELL = 10;
	/**
	 * How long have we been stopped in one location.
	 */
	private long _stopCount = 0;
	/**
	 * A variable time delta that adjusts to speed and other variables in
	 * milliseconds.
	 */
	private long _timeDelta = 1000;

	// collect every 5 minutes even if we don't move.
	private long MAX_TIME_DELTA = 60000 * 5;
	private LocationManager _lm;

	private void _updateText() {
		_text = "Hello, Android\n";
		_text += "debug: " + _debug + "\n";

		Observation observation = new Observation();
		Map<String, String> kvp = observation.getKvp();
		long now = System.currentTimeMillis();

		if (_lastLocation == null) {
			_text += "no location\n";
		} else {
			_text += _lastLocation.toString() + "\n";
			observation.setX(_lastLocation.getLongitude());
			observation.setY(_lastLocation.getLatitude());
			observation.setAccuracy(_lastLocation.getAccuracy());
			observation.setTime(System.currentTimeMillis());
			observation.setLocationTime(_lastLocationTime);
			kvp.put("Speed", Float.toString(_lastLocation.getSpeed()));
			kvp.put("Bearing", Float.toString(_lastLocation.getBearing()));
		}

		_text += "neighbors: \n";
		List<NeighboringCellInfo> neighbors = _tm.getNeighboringCellInfo();
		for (NeighboringCellInfo n : neighbors) {
			_text += n.toString() + "\n";
			Observation no = observation.clone();
			Map<String, String> noKvp = observation.getKvp();
			noKvp.put("Neighbor", "true");
			if (n.getRssi() != NeighboringCellInfo.UNKNOWN_RSSI) {
				noKvp.put("Rssi", Integer.toString(n.getRssi()));
			}
			if (n.getCid() != NeighboringCellInfo.UNKNOWN_CID) {
				noKvp.put("Cid", Integer.toString(n.getCid()));
			}
			if (n.getLac() != NeighboringCellInfo.UNKNOWN_CID) {
				noKvp.put("Lac", Integer.toString(n.getLac()));
			}
			if (n.getPsc() != NeighboringCellInfo.UNKNOWN_CID) {
				noKvp.put("Psc", Integer.toString(n.getPsc()));
			}
			noKvp.put("NetworkType", convertNetworkType(n.getNetworkType()));

			if (isGoodSample(_lastLocation, observation)) {
				_log.log(no.getX(), no.getY(), no.getAccuracy(),
						_lastLocationTime, no.getTime(), kvp);
			}
		}

		if (_phoneState.isRssiValid()) {
			kvp.put("Rssi", Double.toString(_phoneState.getRssi()));
		}
		if (_phoneState.isAsuValid()) {
			kvp.put("Asu", Double.toString(_phoneState.getAsu()));
		}
		kvp.put("NetworkOperator", _tm.getNetworkOperator());
		kvp.put("NetworkOperatorName", _tm.getNetworkOperatorName());
		kvp.put("NetworkType", Integer.toString(_tm.getNetworkType()));
		CellLocation cl = _tm.getCellLocation();
		if (cl instanceof CdmaCellLocation) {
			CdmaCellLocation ccl = (CdmaCellLocation) cl;
			double lon = (ccl.getBaseStationLongitude() * .25) / 3600.0;
			double lat = (ccl.getBaseStationLatitude() * .25) / 3600.0;
			kvp.put("CdmaCellLocationX", Double.toString(lon));
			kvp.put("CdmaCellLocationY", Double.toString(lat));
			kvp.put("CdmaBaseStationId",
					Integer.toString(ccl.getBaseStationId()));
			kvp.put("CdmaNetworkId", Integer.toString(ccl.getNetworkId()));
			kvp.put("CdmaSystemId", Integer.toString(ccl.getSystemId()));
		} else if (cl instanceof GsmCellLocation) {
			GsmCellLocation gcl = (GsmCellLocation) cl;
			kvp.put("GsmCid", Integer.toString(gcl.getCid()));
			kvp.put("GsmLac", Integer.toString(gcl.getLac()));
		}

		switch (_tm.getDataState()) {
		case TelephonyManager.DATA_CONNECTED:
			kvp.put("DataState", "Connected");
			break;
		case TelephonyManager.DATA_CONNECTING:
			kvp.put("DataState", "Connecting");
			break;
		case TelephonyManager.DATA_DISCONNECTED:
			kvp.put("DataState", "Disconnected");
			break;
		case TelephonyManager.DATA_SUSPENDED:
			kvp.put("DataState", "Suspended");
			break;
		}

		kvp.put("NetworkType", convertNetworkType(_tm.getNetworkType()));
		kvp.put("SimOperator", _tm.getSimOperator());
		kvp.put("NetworkRoaming", Boolean.toString(_tm.isNetworkRoaming()));

		for (Map.Entry<String, String> e : kvp.entrySet()) {
			_text += e.getKey() + ": " + e.getValue() + "\n";
		}

		// If the sample is good.
		if (isGoodSample(_lastLocation, observation)) {
			_log.log(observation.getX(), observation.getY(),
					observation.getAccuracy(), _lastLocationTime,
					observation.getTime(), kvp);
			_lastCollect = now;
			_lastCollectLocation = _lastLocation;
			_updateTimeDelta(_lastLocation, observation);
		}
		// if the location is stale, but everything else looks good, then
		// request an immediate location update.
		else if (now - _lastCollect >= _timeDelta
				&& _lastLocationTime - now > MAX_STALE_LOCATION) {
			_lm.requestLocationUpdates(_bestProvider, 0, 0, this);
		}

		_text += "log count: " + Long.toString(_log.countLogEntries()) + "\n";
		_text += "Time Delta: " + Long.toString(_timeDelta) + "\n";
		_tv.setText(_text);
	}

	private void _updateTimeDelta(Location l, Observation observation) {
		float speed = l.getSpeed();
		long td = _timeDelta;
		if (speed == 0.0) {
			td *= 1.5;
		} else {
			td = Math
					.round(1000
							* (FINAL_CELL_SIZE / DESIRED_OBSERVATIONS_PER_CELL)
							/ speed);
		}

		_setTimeDelta(td);
	}

	private void _setTimeDelta(long td) {
		_timeDelta = Math.min(MAX_TIME_DELTA, td);
		if (_bestProvider != null) {
			_lm.requestLocationUpdates(_bestProvider, _timeDelta, 0.0f, this);
		}
	}

	private void clear() {

	}

	private void emailData() {

		final File output = new File(Environment.getExternalStorageDirectory(),
				"celliddump.txt.gz");

		final ProgressDialog progress = ProgressDialog.show(this,
				"Exporting data...",
				"This may take several minutes with a long collect.", true);
		Thread thread = new Thread(new Runnable() {

			public void run() {
				try {
					_log.dump(output, progress);
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("text/plain");
					i.putExtra(Intent.EXTRA_EMAIL, new String[] {});
					i.putExtra(Intent.EXTRA_SUBJECT, "Cell ID Data");
					i.putExtra(Intent.EXTRA_TEXT, "Here is some Cell ID data");
					i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(output));
					try {
						startActivity(Intent.createChooser(i, "Send mail..."));
					} catch (android.content.ActivityNotFoundException ex) {
						Toast.makeText(OpenCellIdActivity.this,
								"There are no email clients installed.",
								Toast.LENGTH_SHORT).show();
					}
				} catch (IOException e) {
					Toast.makeText(OpenCellIdActivity.this,
							"Error writing file: " + e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
					return;
				} finally {
					progress.dismiss();
				}
			}
		});
		thread.start();
	}

	public boolean isGoodSample(Location l, Observation observation) {
		long now = System.currentTimeMillis();

		if (_lastLocation != null
				&& _lastLocationTime - now <= MAX_STALE_LOCATION
				&& observation.getAccuracy() <= MIN_ACCURACY
				&& (now - _lastCollect >= _timeDelta)) {
			return true;
		} else {
			return false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		_tv = new TextView(this);
		_lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		_tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		_phoneState = new OciPhoneStateListener(this, _tm);

		_tm.listen(_phoneState, PhoneStateListener.LISTEN_CELL_LOCATION
				| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
				| PhoneStateListener.LISTEN_SIGNAL_STRENGTH
				| PhoneStateListener.LISTEN_SERVICE_STATE);

		_log = OpenCellIdLog.getInstance(this);

		_debug = "";
		List<String> providers = _lm.getAllProviders();
		for (String p : providers) {
			_debug += p + " ";
			// lm.requestLocationUpdates(p, 1000, 0.0f, this);
		}

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		_bestProvider = _lm.getBestProvider(criteria, false);
		if (_bestProvider != null) {
			_lm.requestLocationUpdates(_bestProvider, _timeDelta, 0.0f, this);
		}

		_updateText();
		setContentView(_tv);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	public void onLocationChanged(Location l) {
		_lastLocation = l;
		_lastLocationTime = System.currentTimeMillis();
		_updateText();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.quit:
			quit();
			return true;
		case R.id.clear:
			clear();
			return true;
		case R.id.email_data:
			emailData();
			return true;
		case R.id.main_map:
			showMap();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

	public void quit() {
		System.runFinalizersOnExit(true);
		System.exit(0);
	}

	private void showMap() {
		Intent intent = new Intent(this, MainMapActivity.class);
		startActivity(intent);
	}

	public void update() {
		_updateText();
	}
}