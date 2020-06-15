/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

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
 * @property name the model name
 * @property tokensEncoderModel the tokens encoder model
 * @param biRNNConnectionType type of recurrent neural network (e.g. LSTM, GRU, CFN, SimpleRNN)
 * @param biRNNActivation the activation function of the hidden layer
 * @param biRNNHiddenSize the size of the hidden layer of the recurrent network
 * @param biRNNType the type of BiRNN (1 or 2 layers)
 * @property outputLabels the set of output labels
 * @param weightsInitializer the initializer of the weights (zeros if null, default: Glorot)
 * @param biasesInitializer the initializer of the biases (zeros if null, default: null)
 */
class TokensLabelerModel(
  val name: String,
  val tokensEncoderModel: TokensEncoderModel<RealToken, RealSentence<RealToken>>,
  biRNNConnectionType: LayerType.Connection,
  biRNNActivation: ActivationFunction?,
  biRNNHiddenSize: Int,
  biRNNType: BiRNNType = BiRNNType.Single,
  val outputLabels: DictionarySet<Label>,
  weightsInitializer: Initializer? = GlorotInitializer(),
  biasesInitializer: Initializer? = null
) : Serializable {

  /**
   * The BiRNN type, with 1 or 2 stacked BiRNNs.
   */
  enum class BiRNNType { Single, Double }

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
   * The [biRNN] output size is equal to the number of output labels.
   */
  private val outputSize: Int = this.outputLabels.getElements().size

  /**
   * The BiRNN for the hidden encoding.
   */
  val biRNN: DeepBiRNN = when (biRNNType) {
    BiRNNType.Single -> DeepBiRNN(
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = this.tokensEncoderModel.tokenEncodingSize,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        outputMergeConfiguration = ConcatFeedforwardMerge(outputSize = this.outputSize, activationFunction = Softmax()),
        weightsInitializer = weightsInitializer,
        biasesInitializer = biasesInitializer))
    BiRNNType.Double -> DeepBiRNN(
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = this.tokensEncoderModel.tokenEncodingSize,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        weightsInitializer = weightsInitializer,
        biasesInitializer = biasesInitializer),
      BiRNN(
        inputType = LayerType.Input.Dense,
        inputSize = 2 * biRNNHiddenSize,
        recurrentConnectionType = biRNNConnectionType,
        hiddenSize = biRNNHiddenSize,
        hiddenActivation = biRNNActivation,
        outputMergeConfiguration = ConcatFeedforwardMerge(outputSize = this.outputSize, activationFunction = Softmax()),
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
