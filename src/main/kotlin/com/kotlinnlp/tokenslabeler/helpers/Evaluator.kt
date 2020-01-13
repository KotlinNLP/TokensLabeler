/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.simplednn.helpers.Evaluator
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.AnnotatedSentence
import com.kotlinnlp.tokenslabeler.language.BaseSentence

/**
 * The Validator.
 *
 * @param model the model of a TokensLabeler
 * @param testSentences the list of sentences annotated with gold tags
 * @param verbose whether to print info about the validation progress (default = true)
 */
class Evaluator(
  private val model: TokensLabelerModel,
  testSentences: List<AnnotatedSentence>,
  verbose: Boolean = true
) : Evaluator<AnnotatedSentence, LabelsStatistics>(
  examples = testSentences,
  verbose = verbose
) {

  /**
   * The tokens labeler initialized with the [model]-
   */
  private val labeler = TokensLabeler(this.model)

  /**
   * The evaluation statistics.
   */
  override val stats = LabelsStatistics(this.model.outputLabels.getElements().map { it.value })

  /**
   * Evaluate the model with a single example.
   *
   * @param example the example to validate the model with
   */
  override fun evaluate(example: AnnotatedSentence) {

    example.tokens
      .asSequence()
      .map { token -> token.label }
      .zip(this.labeler.predict(BaseSentence(example)).asSequence())
      .forEach { (goldLabel, predictedLabel) ->

        if (predictedLabel.value == goldLabel.value) {
          this.stats.metrics.getValue(goldLabel.value).truePos += 1
        } else {
          this.stats.metrics.getValue(goldLabel.value).falseNeg += 1
          this.stats.metrics.getValue(predictedLabel.value).falsePos += 1
        }
      }

    this.stats.accuracy = this.stats.metrics.values.sumByDouble { it.f1Score } / this.stats.metrics.size
  }
}
