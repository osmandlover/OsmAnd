package net.osmand.plus.dialogs;

import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.map.ITileSource;
import net.osmand.map.ParameterType;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.OnMapSelectedCallback;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin.RasterMapType;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapTileLayer;

import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.OVERLAY;
import static net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode.UNDERLAY;


public class RasterMapMenu {
	private static final String TAG = "RasterMapMenu";
	public static ContextMenuAdapter createListAdapter(final MapActivity mapActivity,
													   final RasterMapType type) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		ContextMenuAdapter adapter = new ContextMenuAdapter(mapActivity.getMyApplication());
		adapter.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		adapter.setProfileDependent(true);
		adapter.setNightMode(nightMode);
		createLayersItems(adapter, mapActivity, type);
		return adapter;
	}

	private static void createLayersItems(final ContextMenuAdapter contextMenuAdapter,
										  final MapActivity mapActivity,
										  final RasterMapType type) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final OsmandSettings settings = app.getSettings();
		final OsmandRasterMapsPlugin plugin = OsmandPlugin.getPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;
		final CommonPreference<Integer> mapTransparencyPreference;
		final CommonPreference<String> mapTypePreference;
		final CommonPreference<String> exMapTypePreference;
		final LayerTransparencySeekbarMode currentMode = type == RasterMapType.OVERLAY ? OVERLAY : UNDERLAY;
		@StringRes final int mapTypeString;
		@StringRes final int mapTypeStringTransparency;
		if (type == RasterMapType.OVERLAY) {
			mapTransparencyPreference = plugin.MAP_OVERLAY_TRANSPARENCY;
			mapTypePreference = plugin.MAP_OVERLAY;
			exMapTypePreference = plugin.MAP_OVERLAY_PREVIOUS;
			mapTypeString = R.string.map_overlay;
			mapTypeStringTransparency = R.string.overlay_transparency;
		} else if (type == RasterMapType.UNDERLAY) {
			mapTransparencyPreference = plugin.MAP_TRANSPARENCY;
			mapTypePreference = plugin.MAP_UNDERLAY;
			exMapTypePreference = plugin.MAP_UNDERLAY_PREVIOUS;
			mapTypeString = R.string.map_underlay;
			mapTypeStringTransparency = R.string.map_transparency;
		} else {
			throw new RuntimeException("Unexpected raster map type");
		}

		CommonPreference<Boolean> hidePolygonsPref = settings.getCustomRenderBooleanProperty("noPolygons");
		CommonPreference<Boolean> hideWaterPolygonsPref = settings.getCustomRenderBooleanProperty("hideWaterPolygons");

		String mapTypeDescr = mapTypePreference.get();
		if (mapTypeDescr!=null && mapTypeDescr.contains(".sqlitedb")) {
			mapTypeDescr = mapTypeDescr.replaceFirst(".sqlitedb", "");
		}

		final boolean mapSelected = mapTypeDescr != null;
		final int toggleActionStringId = mapSelected ? R.string.shared_string_on
				: R.string.shared_string_off;

		final OnMapSelectedCallback onMapSelectedCallback =
				new OnMapSelectedCallback() {
					@Override
					public void onMapSelected(boolean canceled) {
						mapActivity.getDashboard().refreshContent(true);
						boolean refreshToHidePolygons = type == RasterMapType.UNDERLAY;
						if (refreshToHidePolygons) {
							mapActivity.refreshMapComplete();
						}
					}
				};
		final MapLayers mapLayers = mapActivity.getMapLayers();
		ContextMenuAdapter.OnRowItemClick l = new ContextMenuAdapter.OnRowItemClick() {
			@Override
			public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter,
										  View view, int itemId, int pos) {
				if (itemId == mapTypeString) {
					if (mapSelected) {
						plugin.selectMapOverlayLayer(mapTypePreference,
								exMapTypePreference, true, mapActivity, onMapSelectedCallback);
					}
					return false;
				}
				return super.onRowItemClick(adapter, view, itemId, pos);
			}

			@Override
			public boolean onContextMenuClick(final ArrayAdapter<ContextMenuItem> adapter,
											  final int itemId, final int pos, final boolean isChecked, int[] viewCoordinates) {
				if (itemId == toggleActionStringId) {
					app.runInUIThread(() -> {
						plugin.toggleUnderlayState(mapActivity, type, onMapSelectedCallback);
						mapActivity.refreshMapComplete();
					});
				} else if (itemId == R.string.show_polygons) {
					hidePolygonsPref.set(!isChecked);
					hideWaterPolygonsPref.set(!isChecked);
					mapActivity.refreshMapComplete();
				} else if (itemId == R.string.show_transparency_seekbar) {
					updateTransparencyBarVisibility(isChecked);
				} else if (itemId == R.string.show_parameter_seekbar) {
					if (isChecked) {
						plugin.SHOW_MAP_LAYER_PARAMETER.set(true);
						MapTileLayer overlayLayer = plugin.getOverlayLayer();
						if (overlayLayer != null) {
							mapLayers.getMapControlsLayer().showParameterBar(overlayLayer);
						}
					} else {
						plugin.SHOW_MAP_LAYER_PARAMETER.set(false);
						mapLayers.getMapControlsLayer().hideParameterBar();
						updateTransparencyBarVisibility(isSeekbarVisible(RasterMapType.OVERLAY));
					}
				}
				return false;
			}

			private void updateTransparencyBarVisibility(boolean visible) {
				if (visible) {
					plugin.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(currentMode);
					mapLayers.getMapControlsLayer().showTransparencyBar(mapTransparencyPreference);
				} else // if(settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get() == currentMode)
				{
					plugin.LAYER_TRANSPARENCY_SEEKBAR_MODE.set(LayerTransparencySeekbarMode.OFF);
					mapLayers.getMapControlsLayer().hideTransparencyBar();
				}
			}
		};

		mapTypeDescr = mapSelected ? mapTypeDescr : mapActivity.getString(R.string.shared_string_none);
		contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(toggleActionStringId, mapActivity)
				.hideDivider(true)
				.setListener(l)
				.setSelected(mapSelected).createItem());
		if (mapSelected) {
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(mapTypeString, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setLayout(R.layout.list_item_icon_and_menu_wide)
					.setDescription(mapTypeDescr).createItem());
			ContextMenuAdapter.OnIntegerValueChangedListener integerListener =
					new ContextMenuAdapter.OnIntegerValueChangedListener() {
						@Override
						public boolean onIntegerValueChangedListener(int newValue) {
							mapTransparencyPreference.set(newValue);
							mapActivity.getMapLayers().getMapControlsLayer().updateTransparencySliderValue();
							mapActivity.refreshMap();
							return false;
						}
					};
			// android:max="255" in layout is expected
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(mapTypeStringTransparency, mapActivity)
					.hideDivider(true)
					.setLayout(R.layout.list_item_progress)
					.setIcon(R.drawable.ic_action_opacity)
					.setProgress(mapTransparencyPreference.get())
					.setListener(l)
					.setIntegerListener(integerListener).createItem());
			if (type == RasterMapType.UNDERLAY) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.show_polygons, mapActivity)
						.hideDivider(true)
						.setListener(l)
						.setSelected(!hidePolygonsPref.get()).createItem());
			}
			Boolean transparencySwitchState = isSeekbarVisible(type);
			contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
					.setTitleId(R.string.show_transparency_seekbar, mapActivity)
					.hideDivider(true)
					.setListener(l)
					.setSelected(transparencySwitchState).createItem());
			ITileSource oveplayMap = plugin.getOverlayLayer().getMap();
			if (type == RasterMapType.OVERLAY && oveplayMap != null && oveplayMap.getParamType() != ParameterType.UNDEFINED) {
				contextMenuAdapter.addItem(new ContextMenuItem.ItemBuilder()
						.setTitleId(R.string.show_parameter_seekbar, mapActivity)
						.hideDivider(true)
						.setListener(l)
						.setSelected(plugin.SHOW_MAP_LAYER_PARAMETER.get()).createItem());
			}
		}
	}

	@NonNull
	public static Boolean isSeekbarVisible(RasterMapType type) {
		OsmandRasterMapsPlugin plugin = OsmandPlugin.getPlugin(OsmandRasterMapsPlugin.class);
		assert plugin != null;

		LayerTransparencySeekbarMode currentMode = type == RasterMapType.OVERLAY ? OVERLAY : UNDERLAY;
		LayerTransparencySeekbarMode seekbarMode = plugin.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		return seekbarMode == LayerTransparencySeekbarMode.UNDEFINED || seekbarMode == currentMode;
	}
}
