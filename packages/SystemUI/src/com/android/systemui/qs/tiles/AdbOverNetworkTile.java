/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Secure;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.UsageTracker;

import java.net.InetAddress;

public class AdbOverNetworkTile extends QSTile<QSTile.BooleanState> {

    private final SecureSetting mSetting;
    private final UsageTracker mUsageTracker;

    private boolean mListening;

    public AdbOverNetworkTile(Host host) {
        super(host);
        mSetting = new SecureSetting(mContext, mHandler,
                Secure.ADB_PORT) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                if (value == 5555 || observedChange) {
                    mUsageTracker.trackUsage();
                }
                if (mListening) {
                    handleRefreshState(value);
                }
            }
        };
        mUsageTracker = new UsageTracker(host.getContext(), AdbOverNetworkTile.class,
                R.integer.days_to_show_adb_network_tile);
        if (mSetting.getValue() == 5555 && !mUsageTracker.isRecentlyUsed()) {
            mUsageTracker.trackUsage();
        }
        mUsageTracker.setListening(true);
        mSetting.setListening(true);
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mUsageTracker.setListening(false);
        mSetting.setListening(false);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        mSetting.setValue(mState.value ? -1 : 5555);
    }

    @Override
    protected void handleLongClick() {
        if (mState.value) return;  
        final String title = mContext.getString(R.string.quick_settings_reset_confirmation_title,
                mContext.getString(R.string.quick_settings_adb_network_label));
        mUsageTracker.showResetConfirmation(title, new Runnable() {
            @Override
            public void run() {
                refreshState();
            }
        });
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = isAdbEnabled()  && mUsageTracker.isRecentlyUsed();
        if (!state.visible) {
            return;
        }
        state.value = isAdbNetworkEnabled();
        if (state.value) {
            WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                // if wifiInfo is not null, set the label to "hostAddress"
                InetAddress address = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress());
                state.label = address.getHostAddress();
            } else {
                //if wifiInfo is null, set the enabled label without host address
                state.label = mContext.getString(R.string.quick_settings_network_adb_enabled_label);
            }
            state.icon = ResourceIcon.get(R.drawable.ic_qs_network_adb_on);
        } else {
            // Otherwise set the disabled label and icon
            state.label = mContext.getString(R.string.quick_settings_network_adb_disabled_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_network_adb_off);
        }
    }

    private boolean isAdbEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0) > 0;
    }

    private boolean isAdbNetworkEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADB_PORT, 0) > 0;
    }

    private ContentObserver mObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            refreshState();
        }
    };

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ADB_PORT),
                    false, mObserver);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Global.ADB_ENABLED),
                    false, mObserver);
        } else {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
    }

}
