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
