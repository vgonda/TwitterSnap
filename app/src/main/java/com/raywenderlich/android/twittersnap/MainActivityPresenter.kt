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

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText

class MainActivityPresenter(val view: View) {

    fun runTextRecognition(selectedImage: Bitmap) {
        view.showProgress()
        val image = FirebaseVisionImage.fromBitmap(selectedImage)
        val detector = FirebaseVision.getInstance().visionTextDetector
        detector.detectInImage(image)
                .addOnSuccessListener { texts ->
                    processTextRecognitionResult(texts)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    e.printStackTrace()
                }
    }

    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        view.hideProgress()
        val blocks = texts.blocks
        if (blocks.size == 0) {
            view.showNoTextMessage()
            return
        }
        blocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    if (looksLikeHandle(element.text)) {
                        view.showHandle(element.text, element.boundingBox)
                    }
                }
            }
        }
    }

    fun runCloudTextRecognition(selectedImage: Bitmap) {
        view.showProgress()
        val options = FirebaseVisionCloudDetectorOptions.Builder()
                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                .setMaxResults(15)
                .build()
        val image = FirebaseVisionImage.fromBitmap(selectedImage)
        val detector = FirebaseVision.getInstance()
                .getVisionCloudDocumentTextDetector(options)
        detector.detectInImage(image)
                .addOnSuccessListener { texts ->
                    processCloudTextRecognitionResult(texts)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
    }

    class WordPair(val word: String, val handle: FirebaseVisionCloudText.Word)

    private fun processCloudTextRecognitionResult(text: FirebaseVisionCloudText?) {
        view.hideProgress()
        if (text == null) {
            view.showNoTextMessage()
            return
        }
        text.pages.forEach { page ->
            page.blocks.forEach { block ->
                block.paragraphs.forEach { paragraph ->
                    paragraph.words
                            .zipWithNext { a, b ->
                                val word = wordToString(a) + wordToString(b)
                                WordPair(word, b)
                            }
                            .filter { looksLikeHandle(it.word) }
                            .forEach { view.showHandle(it.word, it.handle.boundingBox) }
               }
            }
        }
    }

    private fun wordToString(
            word: FirebaseVisionCloudText.Word): String =
            word.symbols.joinToString("") { it.text }

    private fun looksLikeHandle(text: String) =
            text.matches(Regex("@(\\w+)"))

    interface View {
        fun showNoTextMessage()
        fun showHandle(text: String, boundingBox: Rect?)
        fun showBox(boundingBox: Rect?)
        fun showProgress()
        fun hideProgress()
    }
}
