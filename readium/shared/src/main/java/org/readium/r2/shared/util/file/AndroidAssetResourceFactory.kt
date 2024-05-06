package org.readium.r2.shared.util.file

import android.content.res.AssetManager
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.resource.Resource
import org.readium.r2.shared.util.resource.ResourceFactory

/**
 * Creates [FileResource] instances granting access to `file:///android_asset/` URLs stored in
 * the app's assets.
 */
@ExperimentalReadiumApi
public class AndroidAssetResourceFactory(
    private val assetManager: AssetManager
) : ResourceFactory {

    override suspend fun create(
        url: AbsoluteUrl
    ): Try<Resource, ResourceFactory.Error> {
        val path = url.toFile()?.path
            ?.takeIf { it.startsWith("/android_asset/") }
            ?.removePrefix("/android_asset/")
            ?: return Try.failure(ResourceFactory.Error.SchemeNotSupported(url.scheme))

        val resource = AndroidAssetResource(path, assetManager)

        return Try.success(resource)
    }
}
