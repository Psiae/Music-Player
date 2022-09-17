package com.flammky.android.medialib.common.mediaitem

import android.net.Uri
import javax.annotation.concurrent.Immutable

@Immutable
public class MediaItem private constructor(
    val mediaId: String,
    val mediaUri: Uri
) {



    public class Builder(mediaId: String = "", uri: Uri = Uri.EMPTY) {

    }
}