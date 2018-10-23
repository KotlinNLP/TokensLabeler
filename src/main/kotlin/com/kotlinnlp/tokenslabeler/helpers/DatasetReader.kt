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
 * @param schemeConverter the tags coding scheme (BIEOU, BIO, ...)
 * @param maxSentences the max number of sentences to load
 */
class DatasetReader(
  private val type: String,
  private val filePath: String,
  private val schemeConverter: SchemeConverter,
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

    return AnnotatedSentence(forms.zip(getLabels(annotations)).map { (form, label) ->
      AnnotatedToken(form, label)
    })
  }

  /**
   *
   */
  private fun getLabels(tags: List<String>): List<Label> {

    val labels = mutableListOf<Label>()

    tags.indices.forEach { i ->

      labels.add(Label(
        type = this.schemeConverter.convertTag(
          tag = tags[i],
          prevTag = tags.getOrNull(i - 1),
          nextTag = tags.getOrNull(i + 1)),
        value = tags[i].replace(Regex("^B-|^I-|^O"), "")))
    }

    return labels
  }
}