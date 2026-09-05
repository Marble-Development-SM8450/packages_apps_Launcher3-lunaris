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

public class ResizableFolderGridOrganizer extends FolderGridOrganizer {
    private boolean mLiveSizeActive = false;
    private int mLiveCountX = 0;
    private int mLiveCountY = 0;
    public ResizableFolderGridOrganizer(int maxCountX, int maxCountY) {
        super(maxCountX, maxCountY);
    }
    public static ResizableFolderGridOrganizer createForWidgetSpan(int spanX, int spanY,
            int maxCountX, int maxCountY) {
        ResizableFolderGridOrganizer organizer =
                new ResizableFolderGridOrganizer(maxCountX, maxCountY);
        organizer.setLiveGridSize(spanX, spanY);
        return organizer;
    }
    public void setLiveGridSize(int countX, int countY) {
        mLiveSizeActive = true;
        mLiveCountX = Math.max(1, countX);
        mLiveCountY = Math.max(1, countY);
    }
    public void releaseLiveGridSize() {
        mLiveSizeActive = false;
    }
    public boolean isLiveGridSizeActive() {
        return mLiveSizeActive;
    }
    @Override
    public int getCountX() {
        return mLiveSizeActive ? mLiveCountX : super.getCountX();
    }
    @Override
    public int getCountY() {
        return mLiveSizeActive ? mLiveCountY : super.getCountY();
    }
    @Override
    public int getMaxItemsPerPage() {
        return mLiveSizeActive ? (mLiveCountX * mLiveCountY) : super.getMaxItemsPerPage();
    }
    @Override
    public android.graphics.Point getPosForRank(int rank) {
        if (!mLiveSizeActive) {
            return super.getPosForRank(rank);
        }
        int itemsPerPage = mLiveCountX * mLiveCountY;
        int pagePos = rank % Math.max(1, itemsPerPage);
        android.graphics.Point p = new android.graphics.Point();
        p.x = pagePos % mLiveCountX;
        p.y = pagePos / mLiveCountX;
        return p;
    }
}
