package com.beemdevelopment.aegis.ui.fragments.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.beemdevelopment.aegis.AccountNamePosition;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.ui.GroupManagerActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;

public class AppearancePreferencesFragment extends PreferencesFragment {
    private Preference _groupsPreference;
    private Preference _resetUsageCountPreference;
    private Preference _currentAccountNamePositionPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_appearance);

        _groupsPreference = requirePreference("pref_groups");
        _groupsPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(requireContext(), GroupManagerActivity.class);
            startActivity(intent);
            return true;
        });

        _resetUsageCountPreference = requirePreference("pref_reset_usage_count");
        _resetUsageCountPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSecureDialog(new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.preference_reset_usage_count)
                    .setMessage(R.string.preference_reset_usage_count_dialog)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> _prefs.clearUsageCount())
                    .setNegativeButton(android.R.string.no, null)
                    .create());
            return true;
        });

        int currentTheme = _prefs.getCurrentTheme().ordinal();
        Preference darkModePreference = requirePreference("pref_dark_mode");
        darkModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.theme_titles)[currentTheme]));
        darkModePreference.setOnPreferenceClickListener(preference -> {
            int currentTheme1 = _prefs.getCurrentTheme().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.choose_theme)
                    .setSingleChoiceItems(R.array.theme_titles, currentTheme1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentTheme(Theme.fromInteger(i));

                        dialog.dismiss();

                        getResult().putExtra("needsRecreate", true);
                        requireActivity().recreate();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference langPreference = requirePreference("pref_lang");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            langPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                getResult().putExtra("needsRecreate", true);
                requireActivity().recreate();
                return true;
            });
        } else {
            // Setting locale doesn't work on Marshmallow or below
            langPreference.setVisible(false);
        }

        int currentViewMode = _prefs.getCurrentViewMode().ordinal();
        Preference viewModePreference = requirePreference("pref_view_mode");
        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[currentViewMode]));
        viewModePreference.setOnPreferenceClickListener(preference -> {
            int currentViewMode1 = _prefs.getCurrentViewMode().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.choose_view_mode)
                    .setSingleChoiceItems(R.array.view_mode_titles, currentViewMode1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setCurrentViewMode(ViewMode.fromInteger(i));
                        viewModePreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.view_mode_titles)[i]));
                        getResult().putExtra("needsRefresh", true);
                        overrideAccountNamePosition(ViewMode.fromInteger(i) == ViewMode.TILES);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference codeDigitGroupingPreference = requirePreference("pref_code_group_size_string");
        codeDigitGroupingPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        Preference onlyShowNecessaryAccountNames = requirePreference("pref_shared_issuer_account_name");
        onlyShowNecessaryAccountNames.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        int currentAccountNamePosition = _prefs.getAccountNamePosition().ordinal();
        _currentAccountNamePositionPreference = requirePreference("pref_account_name_position");
        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[currentAccountNamePosition]));
        _currentAccountNamePositionPreference.setOnPreferenceClickListener(preference -> {
            int currentAccountNamePosition1 = _prefs.getAccountNamePosition().ordinal();

            Dialogs.showSecureDialog(new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.choose_account_name_position))
                    .setSingleChoiceItems(R.array.account_name_position_titles, currentAccountNamePosition1, (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        _prefs.setAccountNamePosition(AccountNamePosition.fromInteger(i));
                        _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[i]));
                        getResult().putExtra("needsRefresh", true);
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create());

            return true;
        });

        Preference showIconsPreference = requirePreference("pref_show_icons");
        showIconsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        overrideAccountNamePosition(_prefs.getCurrentViewMode() == ViewMode.TILES);
    }

    private void overrideAccountNamePosition(boolean override) {
        if (override) {
            _currentAccountNamePositionPreference.setEnabled(false);
            _currentAccountNamePositionPreference.setSummary("This setting is overridden by the tiles view mode. Account name will always be shown below the issuer.");
        } else {
            _currentAccountNamePositionPreference.setEnabled(true);
            _currentAccountNamePositionPreference.setSummary(String.format("%s: %s", getString(R.string.selected), getResources().getStringArray(R.array.account_name_position_titles)[_prefs.getAccountNamePosition().ordinal()]));
        }
    }
}
