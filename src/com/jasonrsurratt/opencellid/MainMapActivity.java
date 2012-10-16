package com.jasonrsurratt.opencellid;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import android.app.Activity;
import android.os.Bundle;

public class MainMapActivity extends Activity {
	MapView _map;
	boolean _firstUpdate = true;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_map = new MapView(this, 256);
		_map.getController().setZoom(14);
		_map.setMultiTouchControls(true);
		TowerCoverage.getInstance(this);
	
		MyLocationOverlay myLocation = new MyLocationOverlay(this, _map);
		myLocation.enableFollowLocation();
		myLocation.enableMyLocation();
		_map.getOverlayManager().add(myLocation);
		_map.getOverlayManager().add(new TilesOverlay(new TowerTileProvider(), this));
		setContentView(_map);

	}
	
}
