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
