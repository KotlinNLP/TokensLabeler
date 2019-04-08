/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.properties.Position

/**
 * The annotated sentence.
 *
 * @property tokens the list of tokens that compose the sentence
 */
data class AnnotatedSentence(
  override val tokens: List<AnnotatedToken>,
  override val position: Position
) : RealSentence<AnnotatedToken> {

  /**
   * @return the string representation of this sentence
   */
  override fun toString(): String = this.tokens.joinToString("\n")
}