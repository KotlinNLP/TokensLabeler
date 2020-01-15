/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.utils.DictionarySet
import java.io.Serializable

/**
 * The CorpusDictionary.
 */
class CorpusDictionary : Serializable {

  companion object {

    /**
     * Private val used to serialize the class (needed by Serializable).
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L

    /**
     * Create a new corpus populated with the information contained in the given [sentences].
     *
     * @param sentences a list of sentences
     *
     * @return a new corpus dictionary
     */
    operator fun invoke(sentences: List<RealSentence<AnnotatedToken>>): CorpusDictionary {

      val dictionary = CorpusDictionary()

      sentences.forEach { it.tokens.forEach { token -> dictionary.addInfo(token) } }

      return dictionary
    }
  }

  /**
   * The words normalized.
   */
  val normalizedWords = DictionarySet<String>()

  /**
   * The words.
   */
  val words = DictionarySet<String>()

  /**
   * The words.
   */
  val labels = DictionarySet<Label>()

  /**
   * Add the info of a given [token] into this dictionary.
   *
   * @param token the token of a sentence
   */
  private fun addInfo(token: AnnotatedToken) {
    
    this.words.add(token.form)
    this.normalizedWords.add(token.normalizedForm)
    this.labels.add(token.label)
  }
}
