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

import java.util.Hashtable;

import microsoft.mappoint.TileSystem;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.util.GeoPoint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jasonrsurratt.opencellid.TowerCoverage.CellLocation;
import com.jasonrsurratt.opencellid.TowerCoverage.CellValues;

public class TowerTileProvider extends MapTileProviderBase {

	TileSystem _ts = new TileSystem();
	BitmapDrawable _empty = null;
	int[] _pixels = new int[256 * 256];

	public static class MyPoint {
		public double x, y;
	}

	public TowerTileProvider() {
		super(new TowerTileSource());
	}

	@Override
	public void detach() {
	}

	public Drawable getEmptyDrawable() {
		if (_empty == null) {
			Bitmap bm = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(bm);
			c.drawARGB(0, 0, 0, 0);
			Point p = TileSystem.LatLongToPixelXY(38, -104, 12, null);
			Log.w("TowerTileProvider", String.format("%d %d", p.x, p.y));
			_empty = new BitmapDrawable(bm);
		}

		return _empty;
	}

	private MyPoint convert(GeoPoint gp) {
		MyPoint result = new MyPoint();
		result.x = (double) gp.getLongitudeE6() / 1.0e6;
		result.y = (double) gp.getLatitudeE6() / 1.0e6;

		return result;
	}

	private final void _drawRect(int[] pixels, int x1, int y1, int x2, int y2,
			int color) {
		for (int px = x1; px <= x2; px++) {
			for (int py = y1; py <= y2; py++) {
				int i = py * 256 + px;
				if (i >= 0 && i < _pixels.length) {
					_pixels[i] = color;
				}
			}
		}
	}

	@Override
	public Drawable getMapTile(MapTile pTile) {
		boolean foundOne = false;

		if (TowerCoverage.getInstance().isLoaded()) {
			double cellWidth = 1.0 / 3600.0;
			int tileMinX = pTile.getX() * 256;
			int tileMinY = pTile.getY() * 256;
			MyPoint lowerLeft = convert(TileSystem.PixelXYToLatLong(tileMinX,
					tileMinY + 256, pTile.getZoomLevel(), null));
			lowerLeft.x -= cellWidth * 2.0;
			lowerLeft.y -= cellWidth * 2.0;
			MyPoint upperRight = convert(TileSystem.PixelXYToLatLong(
					tileMinX + 256, tileMinY, pTile.getZoomLevel(), null));
			upperRight.x += cellWidth * 2.0;
			upperRight.y += cellWidth * 2.0;

			Point p1 = new Point();
			Point p2 = new Point();
			Hashtable<CellLocation, CellValues> coverage = TowerCoverage
					.getInstance().getCoverage();
			for (CellValues cv : coverage.values()) {
				if (cv.x <= upperRight.x && cv.x >= lowerLeft.x
						&& cv.y <= upperRight.y && cv.y >= lowerLeft.y) {
					int color;
					double asu = Math.min(5, Math.max(0.0, cv.getAsu()));
					if (asu == 0.0) {
						color = 0x7F000000;
					} else {
						color = 0x7F000000 + (int) (asu / 5.0 * 255)
								* 0x100
								+ (int) ((5.0 - asu) / 5.0 * 0xFF)
								* 0x10000;
					}

					if (foundOne == false) {
						_pixels = new int[256 * 256];
						for (int i = 0; i < _pixels.length; i++) {
							_pixels[i] = 0x00000000;
						}
					}
					foundOne = true;
					TileSystem.LatLongToPixelXY(cv.y + 0.5 / 3600.0, cv.x - 0.5 / 3600.0,
							pTile.getZoomLevel(), p1);
					TileSystem.LatLongToPixelXY(cv.y - 0.5 / 3600.0,
							cv.x + 0.5 / 3600.0, pTile.getZoomLevel(), p2);
					int p1x = p1.x - tileMinX;
					int p1y = p1.y - tileMinY;
					int p2x = p2.x - tileMinX;
					int p2y = p2.y - tileMinY;
					_drawRect(_pixels, p1x, p1y, p2x, p2y, color);
				}
			}
		}

		if (foundOne) {
			Bitmap bm = Bitmap.createBitmap(_pixels, 256, 256,
					Bitmap.Config.ARGB_8888);
			return new BitmapDrawable(bm);
		} else {
			return getEmptyDrawable();
		}
	}

	@Override
	public int getMaximumZoomLevel() {
		return 16;
	}

	@Override
	public int getMinimumZoomLevel() {
		return 12;
	}

}
