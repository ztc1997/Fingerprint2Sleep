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

import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BaseListAdapterFilter<T extends IBaseListAdapterItem> extends Filter {
    private IBaseListAdapterFilterable<T> mTarget;

    public interface IBaseListAdapterFilterable<T> {
        public List<T> getOriginalData();

        public List<T> getFilteredData();

        public void onFilterPublishResults(List<T> results);
    }

    public BaseListAdapterFilter(IBaseListAdapterFilterable<T> target) {
        mTarget = target;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        String search = constraint.toString().toLowerCase(Locale.getDefault());
        ArrayList<T> original = new ArrayList<T>(mTarget.getOriginalData());

        if (search == null || search.length() == 0) {
            results.values = original;
            results.count = original.size();
        } else {
            final ArrayList<T> nlist = new ArrayList<T>();
            for (int i = 0; i < original.size(); i++) {
                final T item = original.get(i);
                final String val = item.getText().toLowerCase(Locale.getDefault());

                if (val.contains(search))
                    nlist.add(item);
            }

            results.values = nlist;
            results.count = nlist.size();
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        mTarget.onFilterPublishResults((ArrayList<T>) results.values);
    }
}
