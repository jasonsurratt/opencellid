package com.jasonrsurratt.opencellid;

import java.util.HashMap;
import java.util.Hashtable;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class TowerCoverage {
	private static TowerCoverage _theInstance;
	// 1 arcsec.
	private final static double CELL_SIZE = 1.0 / 3600.0;
	private Context _context;

	private boolean _loaded = false;

	public static class CellValues {
		public double asuSum;
		public long count;
		public double x, y;

		public CellValues(double x, double y, double asuSum, long count) {
			this.x = x;
			this.y = y;
			this.asuSum = asuSum;
			this.count = count;
		}

		public double getAsu() {
			return asuSum / (double) count;
		}
	}

	public static class CellLocation {
		int cx, cy;

		@Override
		public boolean equals(Object o) {
			if (o instanceof CellLocation) {
				CellLocation other = (CellLocation) o;
				return cx == other.cx && cy == other.cy;
			} else {
				return false;
			}
		}

		public int hashCode() {
			return cx ^ cy;
		}
	}

	Hashtable<CellLocation, CellValues> _coverage = new Hashtable<CellLocation, CellValues>();

	private TowerCoverage(Context context) {
		_context = context;
		_loadObservations();
	}

	public Hashtable<CellLocation, CellValues> getCoverage() {
		return _coverage;
	}

	public boolean isLoaded() {
		return _loaded;
	}

	private void _loadObservations() {

		final ProgressDialog progress = ProgressDialog.show(_context,
				"Loading data...", "This may take several minutes.", true);

		Thread thread = new Thread(new Runnable() {

			public void run() {
				try {
					OpenCellIdLog log = OpenCellIdLog.getInstance(_context);

					for (Observation o : log.getAllObservations()) {
						onLogEvent(o);
					}

					_loaded = true;
				} catch (Exception e) {
					Toast.makeText(_context, "Error reading cell log",
							Toast.LENGTH_SHORT).show();
				} finally {
					progress.dismiss();
				}
			}
		});
		thread.start();

	}

	public static TowerCoverage getInstance(Context context) {
		if (_theInstance == null) {
			_theInstance = new TowerCoverage(context);
		}
		return _theInstance;
	}

	public static TowerCoverage getInstance() {
		if (_theInstance == null) {
			throw new RuntimeException(
					"Internal error. Must call with context first.");
		}
		return _theInstance;
	}

	public void onLogEvent(Observation o) {
		if (o.isNeighbor() == false) {
			CellLocation tmpCl = new CellLocation();
			tmpCl.cx = (int) Math.round((o.getX() + 180) / CELL_SIZE);
			tmpCl.cy = (int) Math.round((o.getY() + 90) / CELL_SIZE);

			String asuStr = o.getKvp().get("Asu");
			int asu = asuStr == null ? 0 : (int) Double.parseDouble(o.getKvp()
					.get("Asu"));
			CellValues l = _coverage.get(tmpCl);
			if (l == null) {
				l = new CellValues((double) tmpCl.cx * CELL_SIZE - 180,
						(double) tmpCl.cy * CELL_SIZE - 90, asu, 1);
				_coverage.put(tmpCl, l);
			} else {
				l.asuSum += asu;
				l.count += 1;
			}
		}
	}

}
