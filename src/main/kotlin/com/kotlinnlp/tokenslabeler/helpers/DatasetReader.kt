/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.tokenslabeler.language.*
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * Load sentences from a file.
 *
 * @param type the string that describes the type of sentences
 * @param filePath the file path
 * @param useOPlus whether to use the "O Plus" annotation
 * @param maxSentences the max number of sentences to load
 */
class DatasetReader(
  private val type: String,
  private val filePath: String,
  private val useOPlus: Boolean,
  private val maxSentences: Int? = null
) {

  /**
   * Load sentences from a file.
   *
   * @return the list of loaded sentences
   */
  fun loadSentences(): List<AnnotatedSentence> {

    println("Loading ${this.type} sentences from '%s'%s...".format(
      this.filePath,
      this.maxSentences?.let { " (max $it)" } ?: ""
    ))

    val sentences = mutableListOf<AnnotatedSentence>()
    val buffer = mutableListOf<String>()

    BufferedReader(InputStreamReader(FileInputStream(File(this.filePath)), Charsets.UTF_8)).lines().use { lines ->

      for (line in lines) {

        if (line.isNotEmpty()) {
          buffer.add(line)
        } else {
          sentences.add(buildSentence(buffer))
          buffer.clear()
        }

        if (this.maxSentences != null && sentences.size >= this.maxSentences) {
          break
        }
      }
    }

    return sentences
  }

  /**
   *
   */
  private fun buildSentence(lines: List<String>): AnnotatedSentence {

    val (forms: List<String>, annotations: List<String>) = lines.map { line ->
      line.split("\t").let { Pair(it[0], it[1]) }
    }.unzip()

    return BaseSentence(forms).toAnnotatedSentence(this.getLabels(annotations))
  }

  /**
   *
   */
  private fun getLabels(tags: List<String>): List<Label> {

    val labels = mutableListOf<Label>()

    tags.indices.forEach { i ->

      val value: String = tags[i].replace(Regex("^B-|^I-|^O"), "")

      labels.add(Label(
        type = convertTag(
          tag = tags[i].toTag()!!,
          nextTag = tags.getOrNull(i + 1).toTag()),
        value = if (value.isNotEmpty()) value else Label.EMPTY_VALUE))
    }

    return labels.let { if (this.useOPlus) it.setOPlus(); it }
  }

  /**
   * Enrich the annotation with the "O Plus".
   */
  private fun MutableList<Label>.setOPlus() = this.zipWithNext { a, b ->
    if (a.type == BIEOUTag.Outside && b.type != BIEOUTag.Outside) a.value = b.value
  }

  /**
   * @return the tag in the BIO format
   */
  private fun String?.toTag(): BIEOUTag? =
    if (this == null)
      null
    else when {
      this.startsWith("O") -> BIEOUTag.Outside
      this.startsWith("B-") -> BIEOUTag.Beginning
      this.startsWith("I-") -> BIEOUTag.Inside
      else -> throw IllegalArgumentException("Unexpected tag")
    }

  /**
   * Convert the [tag] from the BIO to the BIEOU format.
   *
   * @param tag the current tag
   * @param nextTag the next tag
   *
   * @return the converted tag
   */
  private fun convertTag(tag: BIEOUTag, nextTag: BIEOUTag?): BIEOUTag {
    return if (nextTag != null)
      when {
        tag == BIEOUTag.Outside -> BIEOUTag.Outside
        tag == BIEOUTag.Beginning && nextTag != BIEOUTag.Inside -> BIEOUTag.Unit
        tag == BIEOUTag.Beginning && nextTag == BIEOUTag.Inside -> BIEOUTag.Beginning
        tag == BIEOUTag.Inside && nextTag == BIEOUTag.Inside -> BIEOUTag.Inside
        tag == BIEOUTag.Inside && nextTag != BIEOUTag.Inside -> BIEOUTag.End
        else -> throw IllegalArgumentException("Unexpected tag")
      }
    else
      when (tag) {
        BIEOUTag.Outside -> BIEOUTag.Outside
        BIEOUTag.Inside -> BIEOUTag.End
        BIEOUTag.Beginning -> BIEOUTag.Unit
        else -> throw IllegalArgumentException("Unexpected tag")
      }
  }
}
