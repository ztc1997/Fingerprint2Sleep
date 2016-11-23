/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.marshmallow.gravitybox.adapters;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class BasicIconListItem extends BasicListItem
        implements IIconListAdapterItem {
    private Drawable mIconLeft;
    private Drawable mIconRight;
    private int mIconLeftId;
    private int mIconRightId;

    public BasicIconListItem(String text, String subText, Drawable iconLeft, Drawable iconRight) {
        super(text, subText);

        mIconLeft = iconLeft;
        mIconRight = iconRight;
    }

    public BasicIconListItem(String text, String subText, int iconLeftId, int iconRightId, Resources res) {
        super(text, subText);

        mIconLeftId = iconLeftId;
        mIconRightId = iconRightId;

        if (mIconLeftId != 0) {
            mIconLeft = res.getDrawable(mIconLeftId);
        }
        if (mIconRightId != 0) {
            mIconRight = res.getDrawable(mIconRightId);
        }
    }

    public BasicIconListItem(String text, String subText) {
        this(text, subText, null, null);
    }

    @Override
    public Drawable getIconLeft() {
        return mIconLeft;
    }

    @Override
    public Drawable getIconRight() {
        return mIconRight;
    }

    public void setIconIdLeft(Drawable icon) {
        mIconLeft = icon;
    }

    public void setIconRight(Drawable icon) {
        mIconRight = icon;
    }

    public int getIconLeftId() {
        return mIconLeftId;
    }

    public int getIconRightId() {
        return mIconRightId;
    }

    public void setIcons(Drawable left, Drawable right) {
        mIconLeft = left;
        mIconRight = right;
    }
}