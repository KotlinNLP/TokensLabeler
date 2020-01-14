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
import com.kotlinnlp.tokenslabeler.language.IOBTag
import com.kotlinnlp.tokenslabeler.language.BaseSentence
import com.kotlinnlp.tokenslabeler.language.Label

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
  override val stats = LabelsStatistics(
    labels = this.model.outputLabels.getElements().filter { it.type != IOBTag.Outside }.map { it.value }.toSet())

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
        this.evaluatePrediction(predictedLabel = predictedLabel, goldLabel = goldLabel)
      }

    this.stats.accuracy = this.stats.metrics.values.sumByDouble { it.f1Score } / this.stats.metrics.size
  }

  /**
   * Evaluate the prediction of a single label.
   *
   * @param predictedLabel the predicted label
   * @param goldLabel the target label
   */
  private fun evaluatePrediction(predictedLabel: Label, goldLabel: Label) {

    if (predictedLabel.value == goldLabel.value) {

      if (goldLabel.type != IOBTag.Outside)
        this.stats.metrics.getValue(goldLabel.value).truePos += 1

    } else {

      if (goldLabel.type != IOBTag.Outside)
        this.stats.metrics.getValue(goldLabel.value).falseNeg += 1

      if (predictedLabel.type != IOBTag.Outside)
        this.stats.metrics.getValue(predictedLabel.value).falsePos += 1
    }
  }
}
