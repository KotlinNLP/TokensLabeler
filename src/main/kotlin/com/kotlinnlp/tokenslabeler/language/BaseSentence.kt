/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.linguisticdescription.sentence.token.properties.Position

/**
 * A base sentence.
 *
 * @property tokens the list of tokens that compose the sentence
 * @property position the position of this sentence in the original text
 */
data class BaseSentence(
  override val tokens: List<BaseToken>,
  override val position: Position
) : RealSentence<BaseToken> {

  /**
   * @param sentence an annotated sentence
   */
  constructor(sentence: AnnotatedSentence) : this(
    tokens = sentence.tokens.map { BaseToken(form = it.form, position = it.position) },
    position = sentence.position
  )

  /**
   * @param sentence a sentence of RealTokens
   */
  constructor(sentence: RealSentence<RealToken>) : this(
    tokens = sentence.tokens.map { BaseToken(form = it.form, position = it.position) },
    position = sentence.position
  )

  companion object {

    /**
     * Construct a [BaseSentence] from a list of tokens forms.
     *
     * @param forms list of tokens forms
     *
     * @return a new [BaseSentence]
     */
    operator fun invoke(forms: List<String>): BaseSentence {

      var end = -2

      val tokens = forms.mapIndexed { i, it ->

        val start = end + 2 // each couple of consecutive tokens is separated by a spacing char
        end = start + it.length - 1

        BaseToken(
          form = it,
          position = Position(index = i, start = start, end = end)
        )
      }

      return BaseSentence(
        tokens = tokens,
        position = Position(
          index = 0,
          start = tokens.first().position.start,
          end = tokens.last().position.end))
    }
  }

  /**
   * @param labels list of labels, one for each token
   */
  fun toAnnotatedSentence(labels: List<Label>) = AnnotatedSentence(
    tokens = this.tokens.mapIndexed { tokenIndex, it ->
      AnnotatedToken(form = it.form, position = it.position, label = labels[tokenIndex]) },
    position = this.position
  )
}
