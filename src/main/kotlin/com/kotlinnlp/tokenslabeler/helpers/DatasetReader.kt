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
 * @param useOPlus whether to convert labels to the "O Plus" annotation
 * @param includes a set of labels to include in the dataset (others will be ignored), null to include all
 * @param maxSentences the max number of sentences to load
 */
class DatasetReader(
  private val type: String,
  private val filePath: String,
  private val useOPlus: Boolean,
  private val includes: Set<String>? = null,
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

    val labels = this.buildLabels(annotations).also {
      if (this.useOPlus) setOPlus(it)
    }

    return BaseSentence(forms).toAnnotatedSentence(labels)
  }

  /**
   * @param annotations the annotations of the labels of a sentence
   *
   * @return the labels defined with the given annotations
   */
  private fun buildLabels(annotations: List<String>): List<Label> = annotations.map {

    val value: String = it.replace(Regex("^B-|^I-|^O"), "")
    val include: Boolean = value.isNotEmpty() && (this.includes == null || value in this.includes)

    Label(
      type = if (include) it.getIOBTag() else IOBTag.Outside,
      value = if (include) value else Label.EMPTY_VALUE)
  }

  /**
   * @throws IllegalArgumentException if this string does not define a IOB tag
   *
   * @return the IOB tag defined in this string
   */
  private fun String.getIOBTag(): IOBTag =
    when {
      this.startsWith("O") -> IOBTag.Outside
      this.startsWith("B-") -> IOBTag.Beginning
      this.startsWith("I-") -> IOBTag.Inside
      else -> throw IllegalArgumentException("Unexpected tag")
    }

  /**
   * Enrich labels with the "O Plus" annotation.
   *
   * @param labels the list of labels of a sentence
   */
  private fun setOPlus(labels: List<Label>) = labels.zipWithNext { a, b ->
    if (a.type == IOBTag.Outside && b.type != IOBTag.Outside) a.value = b.value
  }
}
