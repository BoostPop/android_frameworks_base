/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

/** Quick settings tile: Hotspot **/
public class HotspotTile extends QSTile<QSTile.BooleanState> {
    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_hotspot_enable_animation);
    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_hotspot_disable_animation);
    private static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName(
	"com.android.settings", "com.android.settings.TetherSettings"));
    private final HotspotController mController;
    private final Callback mCallback = new Callback();
    private final KeyguardMonitor mKeyguard;

    public HotspotTile(Host host) {
        super(host);
        mController = host.getHotspotController();
        mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addCallback(mCallback);
        } else {
            mController.removeCallback(mCallback);
        }
    }

    @Override
    protected void handleClick() {
        final boolean isEnabled = (Boolean) mState.value;
        mController.setHotspotEnabled(!isEnabled);
        mEnable.setAllowAnimation(true);
        mDisable.setAllowAnimation(true);
    }

    @Override
    public void handleLongClick() {
        mHost.startSettingsActivity(TETHER_SETTINGS);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = mController.isHotspotSupported();
        state.label = mContext.getString(R.string.quick_settings_hotspot_label);

        state.value = mController.isHotspotEnabled();
        state.icon = state.visible && state.value ? mEnable : mDisable;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_hotspot_changed_off);
        }
    }

    private final class Callback implements HotspotController.Callback {
        @Override
        public void onHotspotChanged(boolean enabled) {
            refreshState();
        }
    };

}
