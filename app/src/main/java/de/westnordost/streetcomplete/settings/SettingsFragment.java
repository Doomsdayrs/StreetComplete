package de.westnordost.streetcomplete.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import javax.inject.Inject;
import javax.inject.Provider;

import de.westnordost.osmapi.map.data.OsmLatLon;
import de.westnordost.streetcomplete.ApplicationConstants;
import de.westnordost.streetcomplete.FragmentContainerActivity;
import de.westnordost.streetcomplete.Injector;
import de.westnordost.streetcomplete.IntentListener;
import de.westnordost.streetcomplete.Prefs;
import de.westnordost.streetcomplete.data.tiles.DownloadedTilesDao;
import de.westnordost.streetcomplete.oauth.OAuthPrefs;
import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.oauth.OsmOAuthDialogFragment;
import de.westnordost.streetcomplete.tangram.MapFragment;
import de.westnordost.streetcomplete.util.SlippyMapMath;
import de.westnordost.streetcomplete.view.dialogs.AlertDialogBuilder;

public class SettingsFragment extends PreferenceFragmentCompat
		implements SharedPreferences.OnSharedPreferenceChangeListener, IntentListener
{
	public static final String ARG_LAUNCH_AUTH = "de.westnordost.streetcomplete.settings.launch_auth";

	@Inject SharedPreferences prefs;
	@Inject OAuthPrefs oAuth;
	@Inject Provider<ApplyNoteVisibilityChangedTask> applyNoteVisibilityChangedTask;
	@Inject DownloadedTilesDao downloadedTilesDao;

	@Override public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
	{
		Injector.instance.getApplicationComponent().inject(this);
		PreferenceManager.setDefaultValues(getContext(), R.xml.preferences, false);
		addPreferencesFromResource(R.xml.preferences);

		Preference oauth = getPreferenceScreen().findPreference("oauth");
		oauth.setOnPreferenceClickListener(preference ->
		{
			new OsmOAuthDialogFragment().show(getFragmentManager(), OsmOAuthDialogFragment.TAG);
			return true;
		});

		Preference quests = getPreferenceScreen().findPreference("quests");
		quests.setOnPreferenceClickListener(preference ->
		{
			getFragmentActivity().setCurrentFragment(new QuestSelectionFragment());
			return true;
		});

		Preference questsInvalidation = getPreferenceScreen().findPreference("quests.invalidation");
		questsInvalidation.setOnPreferenceClickListener(preference ->
		{
			AlertDialog alertDialog = new AlertDialogBuilder(getContext())
					.setTitle(R.string.invalidation_dialog_title)
					.setMessage(R.string.invalidation_dialog_message)
					.setPositiveButton(R.string.invalidate_here, (dialog, which) -> {
						downloadedTilesDao.remove(getShownTile());
					})
					.setNeutralButton(R.string.invalidate_everywhere, (dialog, which) -> {
						downloadedTilesDao.removeAll();
					})
					.setNegativeButton(android.R.string.cancel, (dialog, which) -> {})
					.create();
			alertDialog.setOnShowListener((dialog) ->
			{
				if (getShownTile() == null)
				{
					((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				}
			});
			alertDialog.show();

			return true;
		});
	}

	@Override
	public void onStart()
	{
		super.onStart();
		updateOsmAuthSummary();
		getActivity().setTitle(R.string.action_settings);
	}

	private Point getShownTile()
	{
		SharedPreferences preferences = getActivity().getSharedPreferences(MapFragment.PREF_NAME, Context.MODE_PRIVATE);
		if(preferences.contains(MapFragment.PREF_LAT) && preferences.contains(MapFragment.PREF_LON))
		{
			OsmLatLon pos = new OsmLatLon(
					Double.longBitsToDouble(preferences.getLong(MapFragment.PREF_LON,0)),
					Double.longBitsToDouble(preferences.getLong(MapFragment.PREF_LAT,0)));
			return SlippyMapMath.enclosingTile(pos, ApplicationConstants.QUEST_TILE_ZOOM);

		}
		return null;
	}

	private void updateOsmAuthSummary()
	{
		Preference oauth = getPreferenceScreen().findPreference("oauth");
		String username = prefs.getString(Prefs.OSM_USER_NAME, null);
		if (oAuth.isAuthorized())
		{
			if(username != null)
			{
				oauth.setSummary(String.format(getResources().getString(R.string.pref_title_authorized_username_summary), username));
			}
			else
			{
				oauth.setSummary(R.string.pref_title_authorized_summary);
			}
		}
		else
		{
			oauth.setSummary(R.string.pref_title_not_authorized_summary2);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.equals(Prefs.OAUTH_ACCESS_TOKEN_SECRET))
		{
			updateOsmAuthSummary();
		}
		else if(key.equals(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS))
		{
			ApplyNoteVisibilityChangedTask task = applyNoteVisibilityChangedTask.get();
			task.setPreference(getPreferenceScreen().findPreference(Prefs.SHOW_NOTES_NOT_PHRASED_AS_QUESTIONS));
			task.execute();
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		DialogFragment fragment;
		if (preference instanceof DialogPreferenceCompat) {
			fragment = ((DialogPreferenceCompat)preference).createDialog();
			Bundle bundle = new Bundle(1);
			bundle.putString("key", preference.getKey());
			fragment.setArguments(bundle);
			fragment.setTargetFragment(this, 0);
			fragment.show(getFragmentManager(),
					"android.support.v7.preference.PreferenceFragment.DIALOG");
		} else super.onDisplayPreferenceDialog(preference);
	}

	private FragmentContainerActivity getFragmentActivity()
	{
		return (FragmentContainerActivity) getActivity();
	}

	@Override public void onNewIntent(Intent intent)
	{
		OsmOAuthDialogFragment oauthFragment = (OsmOAuthDialogFragment) getFragmentManager()
				.findFragmentByTag(OsmOAuthDialogFragment.TAG);
		if(oauthFragment != null)
		{
			oauthFragment.onNewIntent(intent);
		}
	}
}
