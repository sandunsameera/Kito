package com.anagramsoftware.kito.extentions

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Created by udesh on 2/10/18.
 */

// The maximum side length of the image to detect, to keep the size of image less than 4MB.
// Resize the image if its side length is larger than the maximum.
private const val IMAGE_MAX_SIDE_LENGTH = 1280;

// Decode image from imageUri, and resize according to the expectedMaxImageSideLength
// If expectedMaxImageSideLength is
//     (1) less than or equal to 0,
//     (2) more than the actual max size length of the bitmap
//     then return the original bitmap
// Else, return the scaled bitmap
fun Uri.toSizeLimitedByteArray(
        contentResolver: ContentResolver
): ByteArray? {
    // Load the image into InputStream.
    var imageInputStream = contentResolver.openInputStream(this)

    // For saving memory, only decode the image meta and get the side length.
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    val outPadding = Rect()
    BitmapFactory.decodeStream(imageInputStream, outPadding, options)

    // Calculate shrink rate when loading the image into memory.
    var maxSideLength = if (options.outWidth > options.outHeight) options.outWidth else options.outHeight
    options.inSampleSize = 1
    options.inSampleSize = calculateSampleSize(maxSideLength, IMAGE_MAX_SIDE_LENGTH)
    options.inJustDecodeBounds = false
    imageInputStream.close()


    // Load the bitmap and resize it to the expected size length
    imageInputStream = contentResolver.openInputStream(this)
    var bitmap = BitmapFactory.decodeStream(imageInputStream, outPadding, options)
    maxSideLength = if (bitmap.width > bitmap.height) bitmap.width else bitmap.height
    val ratio = IMAGE_MAX_SIDE_LENGTH / maxSideLength.toDouble()
    if (ratio < 1) {
        bitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                false)
    }

//    bitmap = rotateBitmap(bitmap, getImageRotationAngle(this, contentResolver))
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    return baos.toByteArray()
}

// Return the number of times for the image to shrink when loading it into memory.
// The SampleSize can only be a final value based on powers of 2.
private fun calculateSampleSize(maxSideLength: Int, expectedMaxImageSideLength: Int): Int {
    var inSampleSize = 1
    var maxSideLengthMutable = maxSideLength

    while (maxSideLengthMutable > 2 * expectedMaxImageSideLength) {
        maxSideLengthMutable /= 2
        inSampleSize *= 2
    }

    return inSampleSize
}

//// Get the rotation angle of the image taken.
//private fun getImageRotationAngle(
//        imageUri: Uri, contentResolver: ContentResolver): Int {
//    var angle = 0
//    val cursor = contentResolver.query(imageUri,
//            arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)
//    if (cursor != null) {
//        if (cursor.count == 1) {
//            cursor.moveToFirst()
//            angle = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
//        }
//        cursor.close()
//    } else {
//        val exif = ExifInterface(imageUri.path)
//        val orientation = exif.getAttributeInt(
//                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
//
//        when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270
//            ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180
//            ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90
//        }
//    }
//    return angle
//}
//
//// Rotate the original bitmap according to the given orientation angle
//private fun rotateBitmap(bitmap: Bitmap, angle: Int): Bitmap {
//    // If the rotate angle is 0, then return the original image, else return the rotated image
//    return if (angle != 0) {
//        val matrix = Matrix()
//        matrix.postRotate(angle.toFloat())
//        Bitmap.createBitmap(
//                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//    } else {
//        bitmap
//    }
//}
