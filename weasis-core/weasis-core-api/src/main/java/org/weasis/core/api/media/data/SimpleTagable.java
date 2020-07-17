/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.weasis.core.api.media.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleTagable implements Tagable {
    private final Map<TagW, Object> tags;

    public SimpleTagable() {
        this.tags = new HashMap<>();
    }

    public SimpleTagable(Map<TagW, Object> tags) {
        this.tags = tags;
    }

    @Override
    public void setTag(TagW tag, Object value) {
        if (tag != null) {
            tags.put(tag, value);
        }
    }

    @Override
    public boolean containTagKey(TagW tag) {
        return tags.containsKey(tag);
    }

    @Override
    public void setTagNoNull(TagW tag, Object value) {
        if (tag != null && value != null) {
            tags.put(tag, value);
        }
    }

    @Override
    public Object getTagValue(TagW tag) {
        return tag == null ? null : tags.get(tag);
    }

    @Override
    public Iterator<Entry<TagW, Object>> getTagEntrySetIterator() {
        return tags.entrySet().iterator();
    }

}
