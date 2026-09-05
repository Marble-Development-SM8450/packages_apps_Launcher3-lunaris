/*
 * Copyright (C) 2024-2026 The Lunaris AOSP Project
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
package com.android.launcher3.folder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Reorderable;
import com.android.launcher3.apppairs.AppPairIcon;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.model.data.AppPairInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.ViewCache;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FolderGridView extends CellLayout {

    private final ResizableFolderGridOrganizer mOrganizer;
    private final ViewCache mViewCache;

    private Folder mFolder;
    private boolean mViewsBound = false;

    private final int[] mLastCenterForRank = new int[2];
    private final java.util.Map<View, int[]> mLastCenterByView = new java.util.HashMap<>();

    public FolderGridView(Context context, AttributeSet attrs) {
        this(context, attrs, new ResizableFolderGridOrganizer(1, 1));
    }

    public FolderGridView(Context context, AttributeSet attrs,
            ResizableFolderGridOrganizer organizer) {
        super(context, attrs);
        mOrganizer = organizer;
        ActivityContext activityContext = ActivityContext.lookupContext(context);
        mViewCache = activityContext.getViewCache();
        getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        setInvertIfRtl(true);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    public ResizableFolderGridOrganizer getOrganizer() {
        return mOrganizer;
    }

    public void onLiveGridSizeChanged(int countX, int countY) {
        mOrganizer.setLiveGridSize(countX, countY);
        setGridSize(countX, countY);
        if (mViewsBound && mFolder != null) {
            arrangeChildren(new ArrayList<>(mFolder.getIconsInReadingOrder()), /* animate= */ true);
        }
    }

    public void bindItems(List<ItemInfo> items) {
        if (mViewsBound) {
            unbindItems();
        }
        arrangeChildren(items.stream().map(this::createNewView).collect(Collectors.toList()),
                /* animate= */ false);
        mViewsBound = true;
    }

    public void unbindItems() {
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        for (int j = container.getChildCount() - 1; j >= 0; j--) {
            View iconView = container.getChildAt(j);
            iconView.setVisibility(View.VISIBLE);
            if (iconView instanceof BubbleTextView) {
                mViewCache.recycleView(R.layout.folder_application, iconView);
            }
        }
        removeAllViewsInLayout();
        mLastCenterByView.clear();
        mViewsBound = false;
    }

    @SuppressLint("InflateParams")
    public View createNewView(ItemInfo item) {
        if (item == null) return null;

        final View icon;
        if (item instanceof AppPairInfo api) {
            icon = AppPairIcon.inflateIcon(R.layout.folder_app_pair,
                    ActivityContext.lookupContext(getContext()), null, api,
                    BubbleTextView.DISPLAY_FOLDER);
        } else {
            icon = mViewCache.getView(R.layout.folder_application, getContext(), null);
            ((BubbleTextView) icon).applyFromWorkspaceItem((WorkspaceItemInfo) item);
        }

        icon.setOnClickListener(mFolder.mActivityContext.getItemOnClickListener());
        icon.setOnLongClickListener(mFolder);

        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) icon.getLayoutParams();
        android.graphics.Point pos = mOrganizer.getPosForRank(item.rank);
        if (lp == null) {
            icon.setLayoutParams(new CellLayoutLayoutParams(pos.x, pos.y, 1, 1));
        } else {
            lp.setCellXY(pos);
            lp.cellHSpan = lp.cellVSpan = 1;
        }
        return icon;
    }

    public void arrangeChildren(List<View> list, boolean animate) {
        int itemCount = list.size();
        int[] oldCenter = new int[2];
        int[] newCenter = new int[2];

        for (int rank = 0; rank < itemCount; rank++) {
            View v = list.get(rank);
            if (v == null) continue;

            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            ItemInfo info = (ItemInfo) v.getTag();
            android.graphics.Point oldPos = new android.graphics.Point(lp.getCellX(), lp.getCellY());
            android.graphics.Point newPos = mOrganizer.getPosForRank(rank);

            boolean positionChanged = oldPos.x != newPos.x || oldPos.y != newPos.y;

            if (animate && positionChanged && v instanceof Reorderable
                    && v.getParent() != null) {
                regionToCenterPoint(oldPos.x, oldPos.y, 1, 1, oldCenter);
                regionToCenterPoint(newPos.x, newPos.y, 1, 1, newCenter);
                lp.setCellXY(newPos);

                FolderReflowSpring.INSTANCE.reflowTo((View & Reorderable) v,
                        (float) (oldCenter[0] - newCenter[0]),
                        (float) (oldCenter[1] - newCenter[1]),
                        rank);
            } else {
                lp.setCellXY(newPos);
            }

            addViewToCellLayout(v, -1, info.getViewId(), lp, true);
        }

        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        for (int i = container.getChildCount() - 1; i >= itemCount; i--) {
            container.removeViewAt(i);
        }
    }

    @Nullable
    public View getFirstItem() {
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        return container.getChildCount() > 0 ? container.getChildAt(0, 0) : null;
    }
}
