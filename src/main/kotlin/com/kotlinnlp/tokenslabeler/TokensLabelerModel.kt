/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.linguisticdescription.language.Language
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.simplednn.core.functionalities.activations.ActivationFunction
import com.kotlinnlp.simplednn.core.functionalities.activations.Softmax
import com.kotlinnlp.simplednn.core.functionalities.initializers.GlorotInitializer
import com.kotlinnlp.simplednn.core.functionalities.initializers.Initializer
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.layers.models.merge.mergeconfig.ConcatFeedforwardMerge
import com.kotlinnlp.simplednn.deeplearning.birnn.BiRNN
import com.kotlinnlp.simplednn.deeplearning.birnn.deepbirnn.DeepBiRNN
import com.kotlinnlp.tokensencoder.TokensEncoderModel
import com.kotlinnlp.tokenslabeler.language.Label
import com.kotlinnlp.utils.DictionarySet
import com.kotlinnlp.utils.Serializer
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

/**
 * The serializable model of a [TokensLabeler].
 *
 * @property language the language within the system works
 * @property tokensEncoderModel the tokens encoder model
 * @param biRNNConnectionType type of recurrent neural network (e.g. LSTM, GRU, CFN, SimpleRNN)
 * @param biRNNActivation the activation function of the hidden layer
 * @param biRNNHiddenSize the size of the hidden layer of the recurrent network
 * @param numOfBiRNNLayers the number of stacked BiRNNs (default 1)
 * @param inputDropout the probability of the recurrent dropout (default 0.0)
 * @property outputLabels the set of output labels
 * @param labelerDropout the dropout of the final output layer
 * @param weightsInitializer the initializer of the weights (zeros if null, default: Glorot)
 * @param biasesInitializer the initializer of the biases (zeros if null, default: null)
 */
class TokensLabelerModel(
  val language: Language,
  val tokensEncoderModel: TokensEncoderModel<RealToken, RealSentence<RealToken>>,
  biRNNConnectionType: LayerType.Connection,
  biRNNActivation: ActivationFunction?,
  biRNNHiddenSize: Int,
  numOfBiRNNLayers: BiRNNLayersNumber = BiRNNLayersNumber.Single,
  inputDropout: Double = 0.0,
  val outputLabels: DictionarySet<Label>,
  labelerDropout: Double = 0.0,
  weightsInitializer: Initializer? = GlorotInitializer(),
  biasesInitializer: Initializer? = null
) : Serializable {

  /**
   * The number of stacked BiRNNs.
   */
  enum class BiRNNLayersNumber { Single, Double }

  companion object {

    /**
     * Private val used to serialize the class (needed by Serializable).
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L

    /**
     * Read a [TokensLabelerModel] (serialized) from an input stream and decode it.
     *
     * @param inputStream the [InputStream] from which to read the serialized [TokensLabelerModel]
     *
     * @return the [TokensLabelerModel] read from [inputStream] and decoded
     */
    fun load(inputStream: InputStream): TokensLabelerModel = Serializer.deserialize(inputStream)
  }

  /**
   * The [biRNN] output size coincides with the number of output labels
   */
  private val outputSize: Int = this.outputLabels.getElements().size

  /**
   * The BiRNN.
   */
  val biRNN = if (numOfBiRNNLayers == BiRNNLayersNumber.Single) {
    DeepBiRNN(
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = this.tokensEncoderModel.tokenEncodingSize,
        dropout = inputDropout,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        outputMergeConfiguration = ConcatFeedforwardMerge(
          outputSize = this.outputSize,
          activationFunction = Softmax(),
          dropout = labelerDropout),
        weightsInitializer = weightsInitializer,
        biasesInitializer = biasesInitializer))
  } else {
    DeepBiRNN(
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = this.tokensEncoderModel.tokenEncodingSize,
        dropout = inputDropout,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        weightsInitializer = weightsInitializer,
        biasesInitializer = biasesInitializer),
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = biRNNHiddenSize * 2,
        dropout = inputDropout,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        outputMergeConfiguration = ConcatFeedforwardMerge(
          outputSize = this.outputSize,
          activationFunction = Softmax(),
          dropout = labelerDropout),
        weightsInitializer = weightsInitializer,
        biasesInitializer = biasesInitializer))
  }

  /**
   * Serialize this [TokensLabelerModel] and write it to an output stream.
   *
   * @param outputStream the [OutputStream] in which to write this serialized [TokensLabelerModel]
   */
  fun dump(outputStream: OutputStream) = Serializer.serialize(this, outputStream)
}
