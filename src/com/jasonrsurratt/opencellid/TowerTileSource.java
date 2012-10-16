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

import java.io.InputStream;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import android.graphics.drawable.Drawable;

public class TowerTileSource implements ITileSource {

	public Drawable getDrawable(String aFilePath) throws LowMemoryException {
		
		// TODO Auto-generated method stub
		return null;
	}

	public Drawable getDrawable(InputStream aTileInputStream)
			throws LowMemoryException {
		throw new RuntimeException("I hope it doesn't get here.");
	}

	public int getMaximumZoomLevel() {
		return 16;
	}

	public int getMinimumZoomLevel() {
		// TODO Auto-generated method stub
		return 12;
	}

	public String getTileRelativeFilenameString(MapTile aTile) {
		return String.format("%d,%d,%d", aTile.getX(), aTile.getY(), aTile.getZoomLevel());
	}

	public int getTileSizePixels() {
		return 256;
	}

	public String localizedName(ResourceProxy proxy) {
		return name();
	}

	public String name() {
		// TODO Auto-generated method stub
		return "TowerTileSource";
	}

	public int ordinal() {
		return 42;
	}

}
