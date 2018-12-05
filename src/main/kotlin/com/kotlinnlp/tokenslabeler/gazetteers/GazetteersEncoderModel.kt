/* Copyright 2017-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.gazetteers

import com.kotlinnlp.linguisticdescription.sentence.MorphoSentence
import com.kotlinnlp.linguisticdescription.sentence.token.FormToken
import com.kotlinnlp.simplednn.core.functionalities.activations.ActivationFunction
import com.kotlinnlp.simplednn.core.functionalities.initializers.GlorotInitializer
import com.kotlinnlp.simplednn.core.functionalities.initializers.Initializer
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.layers.LayerInterface
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.neuralnetwork.NeuralNetwork
import com.kotlinnlp.tokensencoder.TokensEncoderModel

/**
 * @property tokenEncodingSize the size of the token encoding vectors.
 * @param activation the activation function of the dense transformation
 * @param weightsInitializer the initializer of the weights (zeros if null, default: Glorot)
 * @param biasesInitializer the initializer of the biases (zeros if null, default: Glorot)
 */
class GazetteersEncoderModel(
  override val tokenEncodingSize: Int,
  activation: ActivationFunction?,
  weightsInitializer: Initializer? = GlorotInitializer(),
  biasesInitializer: Initializer? = null
) : TokensEncoderModel<FormToken, MorphoSentence<FormToken>> {

  companion object {

    /**
     * Private val used to serialize the class (needed by Serializable).
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L
  }

  /**
   * The input size of the [denseEncoder].
   */
  internal val inputSize: Int = 3 * 4 + 1 // (PER, LOC, ORG) * (B, I, E, U) + O

  /**
   * The model of the feed-forward Network used to transform the input from sparse to dense
   */
  val denseEncoder = NeuralNetwork (
    LayerInterface(
      size = this.inputSize,
      type = LayerType.Input.SparseBinary),
    LayerInterface(
      size = this.tokenEncodingSize,
      activationFunction = activation,
      connectionType = LayerType.Connection.Feedforward
    ),
    weightsInitializer = weightsInitializer,
    biasesInitializer = biasesInitializer
  )

  /**
   * @return the string representation of this model
   */
  override fun toString(): String = "encoding size %d".format(this.tokenEncodingSize)

  /**
   * @param useDropout whether to apply the dropout
   * @param id an identification number useful to track a specific encoder
   *
   * @return a new tokens encoder that uses this model
   */
  override fun buildEncoder(useDropout: Boolean, id: Int) = GazetteersEncoder(
    model = this,
    useDropout = useDropout,
    id = id
  )

  /**
   * @param updateMethod the update method helper (Learning Rate, ADAM, AdaGrad, ...)
   *
   * @return a new optimizer for this model
   */
  override fun buildOptimizer(updateMethod: UpdateMethod<*>) = GazetteersEncoderOptimizer(
    model = this,
    updateMethod = updateMethod
  )
}
