/*
* Copyright 2014-2015 The Euphoria-OS Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.SlimSeekBarPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.internal.util.cm.ScreenType;
import com.android.internal.util.xd.Action;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class ButtonSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "ButtonSettings";

    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_VOLUME = "volume_keys";

    private static final String KEY_NAVIGATION_BAR_ENABLED = "navigation_bar_show";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String KEY_POWER_MENU = "power_menu";
    private static final String KEY_VOLUME_WAKE_DEVICE = "volume_wake_screen";
    private static final String KEY_VOLUME_MEDIA_CONTROLS = "volbtn_music_controls";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";
    private static final String KEY_VOLUME_CONTROL_RING_STREAM = "volume_keys_control_ring_stream";

    private static final String DIM_NAV_BUTTONS = "dim_nav_buttons";
    private static final String DIM_NAV_BUTTONS_TIMEOUT = "dim_nav_buttons_timeout";
    private static final String DIM_NAV_BUTTONS_ALPHA = "dim_nav_buttons_alpha";
    private static final String DIM_NAV_BUTTONS_ANIMATE = "dim_nav_buttons_animate";
    private static final String DIM_NAV_BUTTONS_ANIMATE_DURATION = "dim_nav_buttons_animate_duration";

    private SwitchPreference mNavbarLeft;
    private SwitchPreference mPowerEndCall;
    private Preference mPowerMenu;
    private SwitchPreference mVolumeKeyWakeControl;
    private SwitchPreference mVolumeKeyMediaControl;
    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mSwapVolumeButtons;
    private SwitchPreference mVolumeControlRingStream;

    private SwitchPreference mDimNavButtons;
    private SlimSeekBarPreference mDimNavButtonsTimeout;
    private SlimSeekBarPreference mDimNavButtonsAlpha;
    private SwitchPreference mDimNavButtonsAnimate;
    private SlimSeekBarPreference mDimNavButtonsAnimateDuration;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        final PreferenceCategory navbarCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_NAVBAR);
        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);

        mNavbarLeft = (SwitchPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);
        if (!ScreenType.isPhone(getActivity())) {
            navbarCategory.removePreference(mNavbarLeft);
            mNavbarLeft = null;
        }

        // SlimDim
        mDimNavButtons = (SwitchPreference) findPreference(DIM_NAV_BUTTONS);
        mDimNavButtons.setOnPreferenceChangeListener(this);

        mDimNavButtonsTimeout = (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_TIMEOUT);
        mDimNavButtonsTimeout.setDefault(3000);
        mDimNavButtonsTimeout.isMilliseconds(true);
        mDimNavButtonsTimeout.setInterval(1);
        mDimNavButtonsTimeout.minimumValue(100);
        mDimNavButtonsTimeout.multiplyValue(100);
        mDimNavButtonsTimeout.setOnPreferenceChangeListener(this);

        mDimNavButtonsAlpha = (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_ALPHA);
        mDimNavButtonsAlpha.setDefault(50);
        mDimNavButtonsAlpha.setInterval(1);
        mDimNavButtonsAlpha.setOnPreferenceChangeListener(this);

        mDimNavButtonsAnimate = (SwitchPreference) findPreference(DIM_NAV_BUTTONS_ANIMATE);
        mDimNavButtonsAnimate.setOnPreferenceChangeListener(this);

        mDimNavButtonsAnimateDuration = (SlimSeekBarPreference) findPreference(DIM_NAV_BUTTONS_ANIMATE_DURATION);
        mDimNavButtonsAnimateDuration.setDefault(2000);
        mDimNavButtonsAnimateDuration.isMilliseconds(true);
        mDimNavButtonsAnimateDuration.setInterval(1);
        mDimNavButtonsAnimateDuration.minimumValue(100);
        mDimNavButtonsAnimateDuration.multiplyValue(100);
        mDimNavButtonsAnimateDuration.setOnPreferenceChangeListener(this);
        updateSettings();

        // Power button ends calls.
        mPowerEndCall = (SwitchPreference) findPreference(KEY_POWER_END_CALL);
        mPowerMenu = (Preference) findPreference(KEY_POWER_MENU);
        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        if (mPowerEndCall == null && mPowerMenu == null) {
            prefScreen.removePreference(powerCategory);
        }

        if (Utils.hasVolumeRocker(getActivity()) && volumeCategory != null) {
            mVolumeKeyWakeControl = (SwitchPreference) findPreference(KEY_VOLUME_WAKE_DEVICE);
            mVolumeKeyMediaControl = (SwitchPreference) findPreference(KEY_VOLUME_MEDIA_CONTROLS);
            if (!res.getBoolean(R.bool.config_supports_volumeKeyScreenOffOptions)) {
                volumeCategory.removePreference(mVolumeKeyWakeControl);
                volumeCategory.removePreference(mVolumeKeyMediaControl);
            } else {
                int wakeControlAction = Settings.System.getInt(resolver,
                        Settings.System.VOLUME_WAKE_SCREEN, 0);
                mVolumeKeyWakeControl = initSwitch(KEY_VOLUME_WAKE_DEVICE, (wakeControlAction == 1));
            }

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (SwitchPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            mVolumeControlRingStream = (SwitchPreference)
                    findPreference(KEY_VOLUME_CONTROL_RING_STREAM);
            int volumeControlRingtone = Settings.System.getInt(getContentResolver(),
                    Settings.System.VOLUME_KEYS_CONTROL_RING_STREAM, 1);
            if (mVolumeControlRingStream != null) {
                mVolumeControlRingStream.setChecked(volumeControlRingtone > 0);
            }
        } else {
            if (volumeCategory != null) {
                prefScreen.removePreference(volumeCategory);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                      (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);
        }
    }

    private void updateSettings() {
        boolean enableNavigationBar = Action.isNavBarEnabled(getActivity());

        if (mDimNavButtons != null) {
            mDimNavButtons.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.DIM_NAV_BUTTONS, 0) == 1);
        }

        if (mDimNavButtonsTimeout != null) {
            final int dimTimeout = Settings.System.getInt(getContentResolver(),
                    Settings.System.DIM_NAV_BUTTONS_TIMEOUT, 3000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDimNavButtonsTimeout.setInitValue((dimTimeout / 100) - 1);
        }

        if (mDimNavButtonsAlpha != null) {
            int alphaScale = Settings.System.getInt(getContentResolver(),
                    Settings.System.DIM_NAV_BUTTONS_ALPHA, 50);
            mDimNavButtonsAlpha.setInitValue(alphaScale);
        }

        if (mDimNavButtonsAnimate != null) {
            mDimNavButtonsAnimate.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.DIM_NAV_BUTTONS_ANIMATE, 0) == 1);
        }

        if (mDimNavButtonsAnimateDuration != null) {
            final int animateDuration = Settings.System.getInt(getContentResolver(),
                    Settings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION, 2000);
            // minimum 100 is 1 interval of the 100 multiplier
            mDimNavButtonsAnimateDuration.setInitValue((animateDuration / 100) - 1);
        }

        updateNavbarPreferences(enableNavigationBar);
    }

    private void updateNavbarPreferences(boolean show) {
        mDimNavButtons.setEnabled(show);
        mDimNavButtonsTimeout.setEnabled(show);
        mDimNavButtonsAlpha.setEnabled(show);
        mDimNavButtonsAnimate.setEnabled(show);
        mDimNavButtonsAnimateDuration.setEnabled(show);
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    private SwitchPreference initSwitch(String key, boolean checked) {
        SwitchPreference switchPreference = (SwitchPreference) getPreferenceManager()
                .findPreference(key);
        if (switchPreference != null) {
            switchPreference.setChecked(checked);
            switchPreference.setOnPreferenceChangeListener(this);
        }
        return switchPreference;
    }

    private void handleSwitchChange(SwitchPreference pref, Object newValue, String setting) {
        Boolean value = (Boolean) newValue;
        int intValue = (value) ? 1 : 0;
        Settings.System.putInt(getContentResolver(), setting, intValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mVolumeKeyWakeControl) {
            handleSwitchChange(mVolumeKeyWakeControl, newValue,
                    Settings.System.VOLUME_WAKE_SCREEN);
            return true;
        } else if (preference == mDimNavButtons) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.DIM_NAV_BUTTONS,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (preference == mDimNavButtonsTimeout) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.DIM_NAV_BUTTONS_TIMEOUT, Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mDimNavButtonsAlpha) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.DIM_NAV_BUTTONS_ALPHA, Integer.parseInt((String) newValue));
            return true;
        } else if (preference == mDimNavButtonsAnimate) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.DIM_NAV_BUTTONS_ANIMATE,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (preference == mDimNavButtonsAnimateDuration) {
            Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.DIM_NAV_BUTTONS_ANIMATE_DURATION,
                Integer.parseInt((String) newValue));
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (ScreenType.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
            return true;
        } else if (preference == mVolumeControlRingStream) {
            int value = mVolumeControlRingStream.isChecked() ? 1 : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VOLUME_KEYS_CONTROL_RING_STREAM, value);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.button_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return new ArrayList<String>();
                }
            };
}
