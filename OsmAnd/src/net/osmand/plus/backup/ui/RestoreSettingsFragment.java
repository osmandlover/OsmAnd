package net.osmand.plus.backup.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.ImportBackupTask;
import net.osmand.plus.backup.LocalFile;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.NetworkSettingsHelper.BackupCollectListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.ImportCompleteFragment;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class RestoreSettingsFragment extends ImportSettingsFragment {

	public static final String TAG = RestoreSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(RestoreSettingsFragment.class.getSimpleName());

	private NetworkSettingsHelper settingsHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		exportMode = false;
		settingsHelper = app.getNetworkSettingsHelper();

		ImportBackupTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getItems();
			}
			List<Object> duplicates = importTask.getDuplicates();
			List<SettingsItem> selectedItems = importTask.getSelectedItems();
			if (duplicates == null) {
				importTask.setDuplicatesListener(getDuplicatesListener());
			} else if (duplicates.isEmpty() && selectedItems != null) {
				settingsHelper.importSettings(selectedItems, "", 1, false, getImportListener());
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.restore_from_osmand_cloud);
		description.setText(R.string.choose_what_to_restore);

		collectItems();

		return view;
	}

	@Override
	protected void onContinueButtonClickAction() {
		importItems();
	}

	@Override
	protected void importFinished(boolean succeed, boolean needRestart, List<SettingsItem> items) {
		if (succeed) {
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				ImportCompleteFragment.showInstance(fm, items, getString(R.string.osmand_cloud), needRestart);
			}
		}
	}

	@Override
	protected void processDuplicates(List<Object> duplicates, List<SettingsItem> items) {
		FragmentManager fragmentManager = getFragmentManager();
		if (duplicates.isEmpty()) {
			if (isAdded()) {
				updateUi(R.string.shared_string_restore, R.string.receiving_data_from_server);
			}
			settingsHelper.importSettings(items, "", 1, false, getImportListener());
		} else if (fragmentManager != null && !isStateSaved()) {
			RestoreDuplicatesFragment.showInstance(fragmentManager, duplicates, items, this);
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		toolbarLayout.setTitle(getString(toolbarTitleRes));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(descriptionRes), getString(R.string.osmand_cloud)),
				new StyleSpan(Typeface.BOLD), getString(R.string.osmand_cloud)
		));
		buttonsContainer.setVisibility(View.GONE);
		progressBar.setVisibility(View.VISIBLE);
		adapter.clearSettingsList();
	}

	private void importItems() {
		List<SettingsItem> selectedItems = settingsHelper.prepareSettingsItems(adapter.getData(), settingsItems, false, false);
		if (settingsItems != null) {
			duplicateStartTime = System.currentTimeMillis();
			settingsHelper.checkDuplicates(settingsItems, selectedItems, getDuplicatesListener());
		}
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
	}

	private void collectItems() {
		settingsHelper.collectSettings("", 0, true, new BackupCollectListener() {
			@Override
			public void onBackupCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items, @NonNull List<RemoteFile> remoteFiles) {
				toolbarLayout.setTitle(getString(R.string.restore_from_osmand_cloud));
				description.setText(R.string.choose_what_to_restore);
				buttonsContainer.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				if (succeed) {
					PrepareBackupResult backup = app.getBackupHelper().getBackup();
					List<SettingsItem> itemsForRestore = new ArrayList<>();
					for (RemoteFile remoteFile : backup.getBackupInfo().filesToDownload) {
						int index = items.indexOf(remoteFile.item);
						if (index != -1) {
							itemsForRestore.add(items.get(index));
						}
					}
					for (Pair<LocalFile, RemoteFile> pair : backup.getBackupInfo().filesToMerge) {
						int index = items.indexOf(pair.second.item);
						if (index != -1) {
							itemsForRestore.add(items.get(index));
						}
					}
					setSettingsItems(itemsForRestore);
					dataList = SettingsHelper.getSettingsToOperateByCategory(settingsItems, false, false);
					adapter.updateSettingsItems(dataList, selectedItemsMap);
				}
			}
		});
		updateUi(R.string.shared_string_preparing, R.string.shared_string_preparing);
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull List<SettingsItem> settingsItems) {
		RestoreSettingsFragment fragment = new RestoreSettingsFragment();
		//fragment.setSettingsItems(settingsItems);
		fm.beginTransaction().
				replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}
}