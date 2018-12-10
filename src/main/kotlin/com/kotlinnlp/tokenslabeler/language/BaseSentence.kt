/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.Sentence

/**
 * A base sentence.
 *
 * @property tokens the list of tokens that compose the sentence
 */
data class BaseSentence(override val tokens: List<BaseToken>) : Sentence<BaseToken> {

  /**
   * @param sentence an annotated sentence
   */
  constructor(sentence: AnnotatedSentence) : this(
    sentence.tokens.map { BaseToken(form = it.form, position = it.position) }
  )
}