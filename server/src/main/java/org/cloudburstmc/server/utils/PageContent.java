package org.cloudburstmc.server.utils;

import com.nukkitx.nbt.NbtMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkNotNull;

@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PageContent {

    private static final int MAX_PAGE_LENGTH = 256;
    private static final String TAG_TEXT = "page";
    private static final String TAG_PHOTO_NAME = "photoname";

    private final String text;
    private final String photoName;

    public static PageContent from(NbtMap tag) {
        String text = tag.getString(TAG_TEXT, "");
        String photoName = tag.getString(TAG_PHOTO_NAME, "");
        return from(text, photoName);
    }

    public static PageContent from(String text) {
        return from(text, "");
    }

    public static PageContent from(String text, String photoName) {
        checkNotNull(text, "text");
        checkNotNull(photoName, "photoName");
        return new PageContent(text, photoName);
    }

    public String getText() {
        return text;
    }

    public String getPhotoName() {
        return photoName;
    }

    public NbtMap createTag() {
        return NbtMap.builder()
                .putString(TAG_TEXT, text)
                .putString(TAG_PHOTO_NAME, photoName)
                .build();
    }

    public boolean isValid() {
        return text.length() <= MAX_PAGE_LENGTH;
    }
}
