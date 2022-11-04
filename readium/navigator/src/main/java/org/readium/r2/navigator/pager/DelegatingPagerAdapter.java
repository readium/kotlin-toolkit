package org.readium.r2.navigator.pager;

/*
 * Copyright 2016–2020 Duolingo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.database.DataSetObserver;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class DelegatingPagerAdapter extends PagerAdapter {

    private final PagerAdapter mDelegate;

    DelegatingPagerAdapter(@NonNull final PagerAdapter delegate) {
        this.mDelegate = delegate;
        delegate.registerDataSetObserver(new MyDataSetObserver(this));
    }

    PagerAdapter getDelegate() {
        return mDelegate;
    }

    @Override
    public int getCount() {
        return mDelegate.getCount();
    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
        mDelegate.startUpdate(container);
    }

    @Override
    public @NonNull
    Object instantiateItem(@NonNull ViewGroup container, int position) {
        return mDelegate.instantiateItem(container, position);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mDelegate.destroyItem(container, position, object);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        mDelegate.setPrimaryItem(container, position, object);
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        mDelegate.finishUpdate(container);
    }

    @Deprecated
    @Override
    public void startUpdate(@NonNull View container) {
        mDelegate.startUpdate(container);
    }

    @Deprecated
    @Override
    public @NonNull
    Object instantiateItem(@NonNull View container, int position) {
        return mDelegate.instantiateItem(container, position);
    }

    @Deprecated
    @Override
    public void destroyItem(@NonNull View container, int position, @NonNull Object object) {
        mDelegate.destroyItem(container, position, object);
    }

    @Deprecated
    @Override
    public void setPrimaryItem(@NonNull View container, int position, @NonNull Object object) {
        mDelegate.setPrimaryItem(container, position, object);
    }

    @Deprecated
    @Override
    public void finishUpdate(@NonNull View container) {
        mDelegate.finishUpdate(container);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return mDelegate.isViewFromObject(view, object);
    }

    @Override
    public Parcelable saveState() {
        return mDelegate.saveState();
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        mDelegate.restoreState(state, loader);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return mDelegate.getItemPosition(object);
    }

    @Override
    public void notifyDataSetChanged() {
        mDelegate.notifyDataSetChanged();
    }

    @Override
    public void registerDataSetObserver(@NonNull DataSetObserver observer) {
        mDelegate.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
        mDelegate.unregisterDataSetObserver(observer);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mDelegate.getPageTitle(position);
    }

    @Override
    public float getPageWidth(int position) {
        return mDelegate.getPageWidth(position);
    }

    private void superNotifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    private static class MyDataSetObserver extends DataSetObserver {

        final DelegatingPagerAdapter mParent;

        private MyDataSetObserver(DelegatingPagerAdapter mParent) {
            this.mParent = mParent;
        }

        @Override
        public void onChanged() {
            if (mParent != null) {
                mParent.superNotifyDataSetChanged();
            }
        }

        @Override
        public void onInvalidated() {
            onChanged();
        }

    }

}
