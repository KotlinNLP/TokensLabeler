/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.simplednn.core.functionalities.losses.SoftmaxCrossEntropyCalculator
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.radam.RADAMMethod
import com.kotlinnlp.simplednn.core.optimizer.ParamsOptimizer
import com.kotlinnlp.simplednn.helpers.Trainer
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.AnnotatedToken
import com.kotlinnlp.tokenslabeler.language.asRealTokens
import com.kotlinnlp.utils.Shuffler
import java.io.File
import java.io.FileOutputStream

/**
 * A helper to train a [TokensLabelerModel].
 *
 * @param model the model to train
 * @param modelFilename the path of the file in which to save the serialized trained model
 * @param dataset the training sentences
 * @param epochs the number of training epochs
 * @param updateMethod the update method to optimize the model parameters
 * @param encoderDropout the dropout probability of the labeler encoder (default 0.0)
 * @param evaluator the helper for the evaluation
 * @param shuffler used to shuffle the examples before each epoch (with pseudo random by default)
 * @param verbose whether to print info about the training progress and timing (default = true)
 */
class Trainer(
  private val model: TokensLabelerModel,
  modelFilename: String,
  dataset: List<RealSentence<AnnotatedToken>>,
  epochs: Int,
  updateMethod: UpdateMethod<*> = RADAMMethod(stepSize = 0.001, beta1 = 0.9, beta2 = 0.999),
  encoderDropout: Double = 0.0,
  evaluator: Evaluator,
  shuffler: Shuffler = Shuffler(),
  verbose: Boolean = true
) : Trainer<RealSentence<AnnotatedToken>>(
  modelFilename = modelFilename,
  optimizers = listOf(ParamsOptimizer(updateMethod)),
  examples = dataset,
  epochs = epochs,
  batchSize = 1,
  evaluator = evaluator,
  shuffler = shuffler,
  verbose = verbose
) {

  /**
   * A tokens labeler.
   */
  private val labeler = TokensLabeler(model = this.model, encoderDropout = encoderDropout)

  /**
   * The optimizer of the [model] parameters.
   */
  private val optimizer: ParamsOptimizer = this.optimizers.single()

  /**
   * Learn from an example (forward + backward).
   *
   * @param example an example to train the model with
   */
  override fun learnFromExample(example: RealSentence<AnnotatedToken>) {

    val output: List<DenseNDArray> = this.labeler.forward(example.asRealTokens())

    val errors: List<DenseNDArray> = output.zip(example.tokens).map { (distribution, token) ->

      val goldIndex: Int = this.model.outputLabels.getId(token.label)!!

      SoftmaxCrossEntropyCalculator.calculateErrors(output = distribution, goldIndex = goldIndex)
    }

    this.labeler.backward(errors)
  }

  /**
   * Accumulate the errors of the model resulting after the call of [learnFromExample].
   */
  override fun accumulateErrors() {
    this.optimizer.accumulate(this.labeler.getParamsErrors(copy = false), copy = false)
  }

  /**
   * Dump the model to file.
   */
  override fun dumpModel() {
    this.model.dump(FileOutputStream(File(this.modelFilename)))
  }
}
