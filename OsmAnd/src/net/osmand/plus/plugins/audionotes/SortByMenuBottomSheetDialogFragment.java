package net.osmand.plus.plugins.audionotes;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.plugins.OsmandPlugin;

public class SortByMenuBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SortByMenuBottomSheetDialogFragment";

	private SortFragmentListener listener;

	public void setListener(SortFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_sort)));

		BaseBottomSheetItem byTypeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_grouped_by_type))
				.setTitle(getString(R.string.by_type))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectSortByMode(NotesSortByMode.BY_TYPE);
					}
				})
				.create();
		items.add(byTypeItem);

		BaseBottomSheetItem byDateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_by_date))
				.setTitle(getString(R.string.by_date))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						selectSortByMode(NotesSortByMode.BY_DATE);
					}
				})
				.create();
		items.add(byDateItem);
	}

	private void selectSortByMode(NotesSortByMode mode) {
		AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null && plugin.NOTES_SORT_BY_MODE.get() != mode) {
			plugin.NOTES_SORT_BY_MODE.set(mode);
			if (listener != null) {
				listener.onSortModeChanged();
			}
		}
		dismiss();
	}

	interface SortFragmentListener {
		void onSortModeChanged();
	}
}
