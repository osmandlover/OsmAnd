package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.track.helpers.GpxParameter.*;

import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxReaderTask.GpxDbReaderCallback;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GpxDbHelper implements GpxDbReaderCallback {

	private final OsmandApplication app;
	private final GPXDatabase database;

	private final Map<File, GpxDataItem> dataItems = new ConcurrentHashMap<>();

	private final ConcurrentLinkedQueue<File> readingItems = new ConcurrentLinkedQueue<>();
	private final Map<File, GpxDataItem> readingItemsMap = new ConcurrentHashMap<>();
	private final Map<File, GpxDataItemCallback> readingItemsCallbacks = new ConcurrentHashMap<>();

	private GpxReaderTask readerTask;

	public interface GpxDataItemCallback {

		default boolean isCancelled() {
			return false;
		}

		void onGpxDataItemReady(@NonNull GpxDataItem item);
	}

	public GpxDbHelper(@NonNull OsmandApplication app) {
		this.app = app;
		database = new GPXDatabase(app);
	}

	public void loadGpxItems() {
		List<GpxDataItem> items = getItems();
		for (GpxDataItem item : items) {
			File file = item.getFile();
			if (file.exists()) {
				putToCache(item);
			} else {
				remove(file);
			}
		}
		loadNewGpxItems();
	}

	private void loadNewGpxItems() {
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		List<GPXInfo> gpxInfos = GpxUiHelper.getGPXFiles(gpxDir, true);
		for (GPXInfo gpxInfo : gpxInfos) {
			File file = new File(gpxInfo.getFileName());
			if (file.exists() && !file.isDirectory() && !hasItem(file)) {
				add(new GpxDataItem(file));
			}
		}
	}

	private void putToCache(@NonNull GpxDataItem item) {
		dataItems.put(item.getFile(), item);
	}

	private void removeFromCache(@NonNull File file) {
		dataItems.remove(file);
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		boolean success = database.rename(currentFile, newFile);
		if (success) {
			GpxDataItem newItem = new GpxDataItem(newFile);
			GpxDataItem oldItem = dataItems.get(currentFile);
			if (oldItem != null) {
				newItem.getGpxData().copyData(oldItem.getGpxData());
			}
			putToCache(newItem);
			removeFromCache(currentFile);
		}
		return success;
	}

	public boolean updateGpxParameter(@NonNull GpxDataItem item, @NonNull GpxParameter<?> parameter, @Nullable Object value) {
		boolean res = database.updateGpxParameter(item, parameter, value);
		putToCache(item);
		return res;
	}

	public boolean updateGpsFiltersConfig(@NonNull GpxDataItem item, double smoothingThreshold,
	                                      double minSpeed, double maxSpeed, double minAltitude,
	                                      double maxAltitude, double maxHdop) {
		Map<GpxParameter<?>, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_SMOOTHING_THRESHOLD, smoothingThreshold);
		map.put(GPX_COL_MIN_FILTER_SPEED, minSpeed);
		map.put(GPX_COL_MAX_FILTER_SPEED, maxSpeed);
		map.put(GPX_COL_MIN_FILTER_ALTITUDE, minAltitude);
		map.put(GPX_COL_MAX_FILTER_ALTITUDE, maxAltitude);
		map.put(GPX_COL_MAX_FILTER_HDOP, maxHdop);

		boolean res = database.updateGpxParameters(item, map);
		putToCache(item);
		return res;
	}

	public boolean updateAppearance(@NonNull GpxDataItem item, int color, @NonNull String width,
	                                boolean showArrows, boolean showStartFinish, int splitType,
	                                double splitInterval, @Nullable String coloringType) {
		Map<GpxParameter<?>, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_COLOR, color);
		map.put(GPX_COL_WIDTH, width);
		map.put(GPX_COL_SHOW_ARROWS, showArrows);
		map.put(GPX_COL_SHOW_START_FINISH, showStartFinish);
		map.put(GPX_COL_SPLIT_TYPE, splitType);
		map.put(GPX_COL_SPLIT_INTERVAL, splitInterval);
		map.put(GPX_COL_COLORING_TYPE, coloringType);

		boolean res = database.updateGpxParameters(item, map);
		putToCache(item);
		return res;
	}

	public boolean remove(@NonNull File file) {
		boolean res = database.remove(file);
		removeFromCache(file);
		return res;
	}

	public boolean remove(@NonNull GpxDataItem item) {
		File file = item.getFile();
		boolean res = database.remove(file);
		removeFromCache(file);
		return res;
	}

	public boolean add(@NonNull GpxDataItem item) {
		boolean res = database.add(item);
		putToCache(item);
		return res;
	}

	public boolean updateAnalysis(@NonNull GpxDataItem item, @Nullable GPXTrackAnalysis analysis) {
		boolean res = database.updateAnalysis(item, analysis);
		putToCache(item);
		return res;
	}

	public boolean clearAnalysis(@NonNull GpxDataItem item) {
		boolean res = database.updateAnalysis(item, null);
		removeFromCache(item.getFile());
		return res;
	}

	@NonNull
	public List<GpxDataItem> getItems() {
		return database.getItems();
	}

	public List<Pair<String, Integer>> getNearestCityList() {
		return database.getNearestCityCollection();
	}

	public List<Pair<String, Integer>> getTrackColorsList() {
		return database.getTrackColorsCollection();
	}

	public List<Pair<String, Integer>> getTrackWidthList() {
		return database.getTrackWidthCollection();
	}

	public long getTracksMinCreateDate() {
		return database.getTracksMinCreateDate();
	}

	public double getTracksMaxDuration() {
		return database.getTracksMaxDuration();
	}

	public List<Pair<String, Integer>> getTrackFolders() {
		return database.getTrackFolders();
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file) {
		return getItem(file, null);
	}

	@Nullable
	public GpxDataItem getItem(@NonNull File file, @Nullable GpxDataItemCallback callback) {
		GpxDataItem item = dataItems.get(file);
		if ((GpxDbUtils.isAnalyseNeeded(file, item) || GpxDbUtils.isCitySearchNeeded(item)) && !isGpxReading(file)) {
			readGpxItem(file, item, callback);
		}
		return item;
	}

	public boolean hasItem(@NonNull File file) {
		return dataItems.containsKey(file);
	}

	@NonNull
	public List<GpxDataItem> getSplitItems() {
		List<GpxDataItem> items = new ArrayList<>();
		for (GpxDataItem item : getItems()) {
			if (item.getGpxData().getValue(GPX_COL_SPLIT_TYPE) != 0) {
				items.add(item);
			}
		}
		return items;
	}

	public boolean isRead() {
		GpxReaderTask readerTask = this.readerTask;
		return readerTask == null || !readerTask.isReading();
	}

	private boolean isGpxReading(@NonNull File file) {
		GpxReaderTask analyser = this.readerTask;
		return readingItems.contains(file) || (analyser != null && file.equals(analyser.getFile()));
	}

	private void readGpxItem(@NonNull File file, @Nullable GpxDataItem item, @Nullable GpxDataItemCallback callback) {
		readingItemsMap.put(file, item != null ? item : new GpxDataItem(file));
		if (callback != null) {
			readingItemsCallbacks.put(file, callback);
		}
		readingItems.add(file);
		if (readerTask == null) {
			startReading();
		}
	}

	private void startReading() {
		readerTask = new GpxReaderTask(app, readingItems, readingItemsMap, this);
		readerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopReading() {
		if (readerTask != null) {
			readerTask.cancel(false);
			readerTask = null;
		}
	}

	@NonNull
	protected GPXDatabase getGPXDatabase() {
		return database;
	}

	@Override
	public void onGpxDataItemRead(@NonNull GpxDataItem item) {
		putToCache(item);
		putGpxDataItemToSmartFolder(item);
	}

	private void putGpxDataItemToSmartFolder(@NonNull GpxDataItem item) {
		TrackItem trackItem = new TrackItem(item.getFile());
		trackItem.setDataItem(item);
		app.getSmartFolderHelper().addTrackItemToSmartFolder(trackItem);
	}

	@Override
	public void onProgressUpdate(@NonNull GpxDataItem... dataItems) {
		for (GpxDataItem item : dataItems) {
			GpxDataItemCallback callback = readingItemsCallbacks.remove(item.getFile());
			if (callback != null) {
				if (callback.isCancelled()) {
					stopReading();
				} else {
					callback.onGpxDataItemReady(item);
				}
			}
		}
	}

	@Override
	public void onReadingCancelled() {
		readingItems.clear();
		readingItemsMap.clear();
		readingItemsCallbacks.clear();
	}

	@Override
	public void onReadingFinished(boolean cancelled) {
		if (!Algorithms.isEmpty(readingItems) && !cancelled) {
			startReading();
		} else {
			readerTask = null;
		}
	}
}