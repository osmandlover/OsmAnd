package net.osmand.plus.osmedit;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiBottomSheetFragment;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.Map;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

public class EditPOIMenuController extends MenuController {

	private OsmPoint osmPoint;
	private OsmEditingPlugin plugin;
	private String categoryDescr;
	private String actionStr;

	public EditPOIMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(mapActivity, osmPoint), pointDescription, mapActivity);
		this.osmPoint = osmPoint;
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmPoint instanceof OsmNotesPoint) {
			builder.setShowTitleIfTruncated(false);
		}

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (plugin != null && activity != null) {
					OsmPoint point = getOsmPoint();
					OsmandApplication app = activity.getMyApplication();
					OsmandSettings settings = app.getSettings();
					OsmOAuthAuthorizationAdapter client = new OsmOAuthAuthorizationAdapter(app);
					boolean isLogged = client.isValidToken()
							|| !Algorithms.isEmpty(settings.USER_NAME.get())
							&& !Algorithms.isEmpty(settings.USER_PASSWORD.get());

					if (point instanceof OpenstreetmapPoint) {
						if (isLogged) {
							SendPoiBottomSheetFragment.showInstance(activity.getSupportFragmentManager(),
									new OsmPoint[]{getOsmPoint()});
						} else {
							LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), null);
						}
					} else if (point instanceof OsmNotesPoint) {
						SendOsmNoteBottomSheetFragment.showInstance(activity.getSupportFragmentManager(),
								new OsmPoint[]{getOsmPoint()});
					}
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_upload);
		leftTitleButtonController.startIconId = R.drawable.ic_action_export;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					AlertDialog.Builder bld = new AlertDialog.Builder(activity);
					bld.setMessage(R.string.recording_delete_confirm);
					bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							MapActivity a = getMapActivity();
							if (plugin != null && a != null) {
								boolean deleted = false;
								OsmPoint point = getOsmPoint();
								if (point instanceof OsmNotesPoint) {
									deleted = plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) point);
								} else if (point instanceof OpenstreetmapPoint) {
									deleted = plugin.getDBPOI().deletePOI((OpenstreetmapPoint) point);
								}
								if (deleted) {
									a.getContextMenu().close();
								}
							}
						}
					});
					bld.setNegativeButton(R.string.shared_string_no, null);
					bld.show();
				}
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.shared_string_delete);
		rightTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;

		categoryDescr = getCategoryDescr();

		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			if (osmPoint.getAction() == Action.DELETE) {
				actionStr = mapActivity.getString(R.string.osm_edit_deleted_poi);
			} else if (osmPoint.getAction() == Action.MODIFY) {
				actionStr = mapActivity.getString(R.string.osm_edit_modified_poi);
			} else/* if(osmPoint.getAction() == Action.CREATE) */ {
				actionStr = mapActivity.getString(R.string.osm_edit_created_poi);
			}
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			if (osmPoint.getAction() == Action.DELETE) {
				actionStr = mapActivity.getString(R.string.osm_edit_closed_note);
			} else if (osmPoint.getAction() == Action.MODIFY) {
				actionStr = mapActivity.getString(R.string.osm_edit_commented_note);
			} else if (osmPoint.getAction() == Action.REOPEN) {
				actionStr = mapActivity.getString(R.string.osm_edit_reopened_note);
			} else/* if(osmPoint.getAction() == Action.CREATE) */ {
				actionStr = mapActivity.getString(R.string.osm_edit_created_note);
			}
		} else {
			actionStr = "";
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return categoryDescr;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof OsmPoint) {
			this.osmPoint = (OsmPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return osmPoint;
	}

	public OsmPoint getOsmPoint() {
		return osmPoint;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(categoryDescr);
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		return actionStr;
	}

	@Override
	public int getAdditionalInfoColorId() {
		if (osmPoint.getAction() == Action.DELETE) {
			return R.color.color_osm_edit_delete;
		} else if (osmPoint.getAction() == Action.MODIFY || osmPoint.getAction() == Action.REOPEN) {
			return R.color.color_osm_edit_modify;
		} else {
			return R.color.color_osm_edit_create;
		}
	}

	@Override
	public int getRightIconId() {
		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			OpenstreetmapPoint osmP = (OpenstreetmapPoint) osmPoint;
			int iconResId = 0;
			String poiTranslation = osmP.getEntity().getTag(POI_TYPE_TAG);
			MapActivity mapActivity = getMapActivity();
			if (poiTranslation != null && mapActivity != null) {
				Map<String, PoiType> poiTypeMap = mapActivity.getMyApplication().getPoiTypes().getAllTranslatedNames(false);
				PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
				if (poiType != null) {
					String id = null;
					if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
						id = poiType.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
						id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
					}
					if (id != null) {
						iconResId = RenderingIcons.getBigIconResourceId(id);
					}
				}
			}
			if (iconResId == 0) {
				iconResId = R.drawable.ic_action_info_dark;
			}
			return iconResId;
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			return R.drawable.ic_action_osm_note_add;
		} else {
			return 0;
		}
	}

	@Override
	public Drawable getRightIcon() {
		int iconResId = getRightIconId();
		if (iconResId != 0) {
			return getIcon(iconResId, getAdditionalInfoColorId());
		} else {
			return null;
		}
	}

	@Override
	public int getAdditionalInfoIconRes() {
		if (osmPoint.getAction() == Action.DELETE) {
			return R.drawable.ic_action_type_delete_16;
		} else if (osmPoint.getAction() == Action.MODIFY || osmPoint.getAction() == Action.REOPEN) {
			return R.drawable.ic_action_type_edit_16;
		} else {
			return R.drawable.ic_action_type_add_16;
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	private String getCategoryDescr() {
		return OsmEditingPlugin.getDescription(osmPoint, getMapActivity(), false);
	}
}
