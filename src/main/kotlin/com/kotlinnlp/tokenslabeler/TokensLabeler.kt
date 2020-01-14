/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.simplednn.core.neuralprocessor.NeuralProcessor
import com.kotlinnlp.simplednn.core.optimizer.ParamsErrorsList
import com.kotlinnlp.simplednn.deeplearning.birnn.deepbirnn.DeepBiRNNEncoder
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokenslabeler.helpers.LabelsDecoder
import com.kotlinnlp.tokenslabeler.language.ScoredLabel
import com.kotlinnlp.tokenslabeler.language.IOBTag
import com.kotlinnlp.tokenslabeler.language.BaseSentence

/**
 * The Tokens Labeler.
 *
 * @param model the model
 * @property id the id used for the pool (default 0)
 * @property useDropout whether to use the dropout or not
 */
class TokensLabeler(
  private val model: TokensLabelerModel,
  override val id: Int = 0,
  override val useDropout: Boolean = false
) : NeuralProcessor<
  BaseSentence, // InputType
  List<DenseNDArray>, // OutputType
  List<DenseNDArray>, // ErrorsType
  NeuralProcessor.NoInputErrors // InputErrorsType
  > {

  /**
   * Not used because the input is a sentence.
   */
  override val propagateToInput: Boolean = false

  /**
   * A new instance of the tokens encoder.
   */
  private val tokensEncoder = this.model.tokensEncoderModel.buildEncoder(useDropout = this.useDropout)

  /**
   * A new instance of the processor of the biRNN.
   */
  private val biRNNProcessor = DeepBiRNNEncoder<DenseNDArray>(
    network = this.model.biRNN,
    useDropout = this.useDropout,
    propagateToInput = true)

  /**
   * @param input the sentence
   *
   * @return a list of scored labels, one for each token of the sentence
   */
  fun predict(input: BaseSentence): List<ScoredLabel> {

    val output: List<DenseNDArray> = this.forward(input)

    val decoder = LabelsDecoder(
      predictions = output,
      model = this.model,
      maxBeamSize = 3,
      maxForkSize = 5,
      maxIterations = 10)

    return decoder.findBestConfiguration(onlyValid = true)
      ?.elements
      ?.asSequence()
      ?.sortedBy { it.id }
      ?.map { it.value }
      ?.toList()
      ?: greedyDecode(output)
  }

  /**
   * The Forward.
   *
   * @param input the sentence
   *
   * @return the result of the forward
   */
  override fun forward(input: BaseSentence): List<DenseNDArray> =
    this.biRNNProcessor.forward(this.tokensEncoder.forward(input))

  /**
   * The Backward.
   *
   * @param outputErrors the output errors
   */
  override fun backward(outputErrors: List<DenseNDArray>) {

    this.biRNNProcessor.backward(outputErrors)
    this.tokensEncoder.backward(this.biRNNProcessor.getInputErrors(copy = false))
  }

  /**
   * Return the input errors of the last backward.
   * Before calling this method make sure that [propagateToInput] is enabled.
   *
   * @param copy whether to return by value or by reference (default true)
   *
   * @return the input errors
   */
  override fun getInputErrors(copy: Boolean) = NeuralProcessor.NoInputErrors

  /**
   * Return the params errors of the last backward.
   *
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference (default true)
   *
   * @return the parameters errors
   */
  override fun getParamsErrors(copy: Boolean): ParamsErrorsList =
    this.tokensEncoder.getParamsErrors(copy) +
      this.biRNNProcessor.getParamsErrors(copy)

  /**
   * Validate this sequence of labels removing invalid sub-sequences of labels.
   *
   * @param predictions the predictions made for each token
   *
   * @return a valid sequence of labels
   */
  private fun greedyDecode(predictions: List<DenseNDArray>): List<ScoredLabel> {

    var prev: ScoredLabel? = null

    return predictions.mapIndexed { tokenIndex, prediction ->

      prev = prediction.argSorted(reverse = true)
        .asSequence()
        .map { i -> ScoredLabel(label = this.model.outputLabels.getElement(i)!!, score = prediction[i]) }
        .first {
          LabelsDecoder.canFollow(prevLabel = prev, curLabel = it) &&
            (tokenIndex != predictions.lastIndex || it.type == IOBTag.Outside)
        }

      prev!!
    }
  }
}
