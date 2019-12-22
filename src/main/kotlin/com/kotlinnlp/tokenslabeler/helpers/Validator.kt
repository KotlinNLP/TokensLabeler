/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.AnnotatedSentence
import com.kotlinnlp.tokenslabeler.language.BaseSentence
import com.kotlinnlp.utils.progressindicator.ProgressIndicatorBar

/**
 * The Validator.
 *
 * @param model the model of a TokensLabeler
 * @param testSentences the list of sentences annotated with gold tags
 */
class Validator(
  private val model: TokensLabelerModel,
  private val testSentences: List<AnnotatedSentence>
) {

  /**
   * The tokens labeler initialized with the [model]-
   */
  private val labeler = TokensLabeler(this.model)

  /**
   *
   */
  fun evaluate(): Map<String, LabelStatistics> {

    val progress = ProgressIndicatorBar(this.testSentences.size)
    val out = mutableMapOf(*this.model.outputLabels.getElements()
      .map { Pair(it.value, LabelStatistics(it.value)) }.toTypedArray())

    this.testSentences.forEach { sentence ->

      progress.tick()

      sentence.tokens
        .asSequence()
        .map { token -> token.label }
        .zip(this.labeler.predict(BaseSentence(sentence)).asSequence())
        .forEach { (goldLabel, predictedLabel) ->

          if (predictedLabel.value == goldLabel.value) {
            out.getValue(goldLabel.value).truePositive += 1
          } else {
            out.getValue(goldLabel.value).falseNegative += 1
            out.getValue(predictedLabel.value).falsePositive += 1
          }
        }
    }

    return out.filterNot { it.value.label.isEmpty() }
  }
}
