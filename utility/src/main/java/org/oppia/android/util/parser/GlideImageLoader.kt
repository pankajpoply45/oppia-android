package org.oppia.android.util.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import org.oppia.android.util.caching.AssetRepository
import org.oppia.android.util.caching.CacheAssetsLocally
import org.oppia.android.util.caching.LoadImagesFromAssets
import javax.inject.Inject

/** An [ImageLoader] that uses Glide. */
class GlideImageLoader @Inject constructor(
  private val context: Context,
  @CacheAssetsLocally private val cacheAssetsLocally: Boolean,
  @LoadImagesFromAssets private val loadImagesFromAssets: Boolean,
  private val assetRepository: AssetRepository
) : ImageLoader {

  override fun loadBitmap(
      imageUrl: String,
      target: ImageTarget<Bitmap>,
      transformations: List<ImageTransformation>
  ) {
    Glide.with(context)
        .asBitmap()
        .load(loadImage(imageUrl))
        .transform(*transformations.toGlideTransformations())
        .intoTarget(target)
  }

  override fun loadSvg(
      imageUrl: String,
      target: ImageTarget<ScalablePictureDrawable>,
      transformations: List<ImageTransformation>
  ) {
    // TODO(#45): Ensure the image caching flow is properly hooked up.
    Glide.with(context)
      .`as`(ScalablePictureDrawable::class.java)
      .fitCenter()
      .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
      .load(loadImage(imageUrl))
        .transform(*transformations.toGlideTransformations())
      .intoTarget(target)
  }

  override fun loadDrawable(
    imageDrawableResId: Int,
    target: ImageTarget<Drawable>,
    transformations: List<ImageTransformation>
  ) {
    Glide.with(context)
      .asDrawable()
      .load(imageDrawableResId)
      .transform(*transformations.toGlideTransformations())
      .intoTarget(target)
  }

  private fun loadImage(imageUrl: String): Any {
    return when {
      cacheAssetsLocally -> object : ImageAssetFetcher {
        override fun fetchImage(): ByteArray = assetRepository.loadRemoteBinaryAsset(imageUrl)()

        override fun getImageIdentifier(): String = imageUrl
      }
      loadImagesFromAssets -> object : ImageAssetFetcher {
        override fun fetchImage(): ByteArray =
            assetRepository.loadImageAssetFromLocalAssets(imageUrl)()

        override fun getImageIdentifier(): String = imageUrl
      }
      else -> imageUrl
    }
  }

  private fun <T> RequestBuilder<T>.intoTarget(target: ImageTarget<T>) {
    when (target) {
      is CustomImageTarget -> into(target.customTarget)
      is ImageViewTarget -> into(target.imageView)
    }
  }

  private fun List<ImageTransformation>.toGlideTransformations(): Array<Transformation<Bitmap>> {
    return map {
      when (it) {
        ImageTransformation.BLUR -> BlurTransformation(context)
      }
    }.toTypedArray()
  }
}
