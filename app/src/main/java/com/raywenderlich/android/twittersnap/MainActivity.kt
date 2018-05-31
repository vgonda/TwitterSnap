/*
 *
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.twittersnap

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), MainActivityPresenter.View {

  private lateinit var presenter: MainActivityPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    this.presenter = MainActivityPresenter(this)

    setUpNewImageListener()
  }

  private fun setUpNewImageListener() {
    fab.setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      startActivityForResult(intent, 1)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, imageReturnedIntent: Intent?) {
    super.onActivityResult(requestCode, resultCode, imageReturnedIntent)
    when (requestCode) {
      1 -> if (resultCode == Activity.RESULT_OK) {
        imageReturnedIntent?.data?.let {
          val selectedImageBitmap = resizeImage(it)
          imageView.setImageBitmap(selectedImageBitmap)
          setUpCloudSearch(selectedImageBitmap)
          overlay.clear()
          presenter.runTextRecognition(selectedImageBitmap!!)
        }
      }
    }
  }

  private fun setUpCloudSearch(selectedImageBitmap: Bitmap?) {
    fab.setImageResource(R.drawable.ic_cloud_black_24dp)
    fab.setBackgroundColor(getColor(R.color.colorPrimaryDark))
    fab.setOnClickListener {
      overlay.clear()
      presenter.runCloudTextRecognition(selectedImageBitmap!!)
    }
  }

  private fun resizeImage(selectedImage: Uri): Bitmap? {
    return getBitmapFromUri(selectedImage)?.let {
      val scaleFactor = Math.max(
          it.width.toFloat() / imageView.width.toFloat(),
          it.height.toFloat() / imageView.height.toFloat())

      Bitmap.createScaledBitmap(it,
          (it.width / scaleFactor).toInt(),
          (it.height / scaleFactor).toInt(),
          true)
    }
  }

  private fun getBitmapFromUri(filePath: Uri): Bitmap? {
    return MediaStore.Images.Media.getBitmap(this.contentResolver, filePath)
  }


  override fun showHandle(text: String, boundingBox: Rect?) {
    overlay.addText(text, boundingBox)
  }

  override fun showBox(boundingBox: Rect?) {
    overlay.addBox(boundingBox)
  }

  override fun showNoTextMessage() {
    Toast.makeText(this, "No text detected", Toast.LENGTH_LONG).show()
  }

  override fun showProgress() {
    progressBar.visibility = View.VISIBLE
  }

  override fun hideProgress() {
    progressBar.visibility = View.GONE
  }
}

