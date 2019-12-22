/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.simplednn.core.functionalities.losses.SoftmaxCrossEntropyCalculator
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.adam.ADAMMethod
import com.kotlinnlp.simplednn.core.optimizer.ParamsOptimizer
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.utils.scheduling.BatchScheduling
import com.kotlinnlp.simplednn.utils.scheduling.EpochScheduling
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.AnnotatedSentence
import com.kotlinnlp.tokenslabeler.language.BaseSentence
import com.kotlinnlp.utils.ExamplesIndices
import com.kotlinnlp.utils.Shuffler
import com.kotlinnlp.utils.Timer
import com.kotlinnlp.utils.progressindicator.ProgressIndicatorBar
import java.io.File
import java.io.FileOutputStream

/**
 * A helper to train a [TokensLabelerModel].
 *
 * @param model the model to train
 * @param modelFilename the path of the file in which to save the serialized trained model
 * @param epochs the number of training epochs
 * @param updateMethod the update method to optimize the model parameters
 * @param validator a helper for the validation of the model
 * @param verbose whether to print info about the training progress and timing (default = true)
 */
class Trainer(
  private val model: TokensLabelerModel,
  private val modelFilename: String,
  private val epochs: Int,
  private val updateMethod: UpdateMethod<*> = ADAMMethod(stepSize = 0.001, beta1 = 0.9, beta2 = 0.999),
  private val validator: Validator,
  private val verbose: Boolean = true
) {

  /**
   * The epoch counter.
   */
  private var epochCount: Int = 0

  /**
   * A timer to track the elapsed time.
   */
  private var timer = Timer()

  /**
   * The best accuracy reached during the training.
   */
  private var bestAccuracy: Double = 0.0

  /**
   * A frame extractor built with the given [model].
   */
  private val annotator = TokensLabeler(this.model)

  /**
   * The optimizer of the [model] parameters.
   */
  private val optimizer = ParamsOptimizer(this.updateMethod)

  /**
   * Check requirements.
   */
  init {
    require(this.epochs > 0) { "The number of epochs must be > 0" }
  }

  /**
   * Train the [model] with the given training dataset over more epochs.
   *
   * @param dataset the training dataset
   * @param shuffler a shuffle to shuffle the sentences at each epoch (can be null)
   */
  fun train(dataset: List<AnnotatedSentence>, shuffler: Shuffler? = Shuffler(enablePseudoRandom = true, seed = 743)) {

    (0 until this.epochs).forEach { i ->

      this.logTrainingStart(epochIndex = i)

      this.newEpoch()
      this.trainEpoch(dataset = dataset, shuffler = shuffler)

      this.logTrainingEnd()

      this.logValidationStart()
      this.validateAndSaveModel()
      this.logValidationEnd()
    }
  }

  /**
   * Train the [model] with all the examples of the dataset.
   *
   * @param dataset the training dataset
   * @param shuffler a shuffle to shuffle the sentences at each epoch (can be null)
   */
  private fun trainEpoch(dataset: List<AnnotatedSentence>, shuffler: Shuffler?) {

    val progress = ProgressIndicatorBar(dataset.size)

    ExamplesIndices(dataset.size, shuffler = shuffler).forEach { i ->

      if (this.verbose) progress.tick()

      this.newBatch()

      this.trainExample(example = dataset[i])

      this.optimizer.update()
    }
  }

  /**
   * Train the [model] with a single example.
   *
   * @param example a training example
   */
  private fun trainExample(example: AnnotatedSentence) {

    val output = this.annotator.forward(BaseSentence(example))

    val errors: List<DenseNDArray> = output.zip(example.tokens).map { (distribution, token) ->

      val goldIndex: Int = this.model.outputLabels.getId(token.label)!!

      SoftmaxCrossEntropyCalculator().calculateErrors(output = distribution, goldIndex = goldIndex)
    }

    this.annotator.backward(errors)
    this.optimizer.accumulate(this.annotator.getParamsErrors(copy = false), copy = false)
  }

  /**
   * Beat the occurrence of a new batch.
   */
  private fun newBatch() {

    if (this.updateMethod is BatchScheduling) {
      this.updateMethod.newBatch()
    }
  }

  /**
   * Beat the occurrence of a new epoch.
   */
  private fun newEpoch() {

    if (this.updateMethod is EpochScheduling) {
      this.updateMethod.newEpoch()
    }

    this.epochCount++
  }

  /**
   * Validate the [model] and save it to file if a new best accuracy has been reached.
   */
  private fun validateAndSaveModel() {

    val statistics: Map<String, LabelStatistics> = this.validator.evaluate()
    val accuracy: Double = statistics.values.sumByDouble { it.f1 } / statistics.size

    if (this.verbose) {
      println()
      statistics.values.forEach { println(it) }
    }

    if (accuracy > this.bestAccuracy) {

      println("\nNEW BEST ACCURACY (%.2f %%)! Saving model to \"${this.modelFilename}\"...".format(100.0 * accuracy))
      this.model.dump(FileOutputStream(File(this.modelFilename)))

      this.bestAccuracy = accuracy
    }
  }

  /**
   * Log when training starts.
   *
   * @param epochIndex the current epoch index
   */
  private fun logTrainingStart(epochIndex: Int) {

    if (this.verbose) {

      this.timer.reset()

      println("\nEpoch ${epochIndex + 1} of ${this.epochs}")
      println("\nStart training...")
    }
  }

  /**
   * Log when training ends.
   */
  private fun logTrainingEnd() {

    if (this.verbose) {
      println("Elapsed time: %s".format(this.timer.formatElapsedTime()))
    }
  }

  /**
   * Log when validation starts.
   */
  private fun logValidationStart() {

    if (this.verbose) {

      this.timer.reset()

      println("\nStart validation...")
    }
  }

  /**
   * Log when validation ends.
   */
  private fun logValidationEnd() {

    if (this.verbose) {
      println("Elapsed time: %s".format(this.timer.formatElapsedTime()))
    }
  }
}
