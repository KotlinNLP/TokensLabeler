/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.optimizer.Optimizer
import com.kotlinnlp.simplednn.core.optimizer.ParamsOptimizer

/**
 * The optimizer of the [TokensLabelerModel].
 *
 * @param model the model to optimize
 * @param updateMethod the update method helper (Learning Rate, ADAM, AdaGrad, ...)
 */
class TokensLabelerOptimizer(
  private val model: TokensLabelerModel,
  updateMethod: UpdateMethod<*>
) : Optimizer<TokensLabelerParameters>(updateMethod = updateMethod) {

  /**
   * The optimizer of the tokens encoder.
   */
  private val tokensEncoderOptimizer = this.model.tokensEncoderModel.buildOptimizer(updateMethod)

  /**
   * The optimizer of the BiRNN.
   */
  private val biRNNOptimizer = ParamsOptimizer(params = this.model.biRNN.model, updateMethod = this.updateMethod)

  /**
   * Update the parameters of the neural element associated to this optimizer.
   */
  override fun update() {

    this.biRNNOptimizer.update()
    this.tokensEncoderOptimizer.update()
  }

  /**
   * Accumulate the given [paramsErrors] into the accumulator.
   *
   * @param paramsErrors the parameters errors to accumulate
   * @param copy a Boolean indicating if the [paramsErrors] can be used as reference or must be copied. Set copy = false
   *             to optimize the accumulation when the amount of the errors to accumulate is 1. (default = true)
   */
  override fun accumulate(paramsErrors: TokensLabelerParameters, copy: Boolean) {

    this.biRNNOptimizer.accumulate(paramsErrors.biRNNParams, copy = copy)
    this.tokensEncoderOptimizer.accumulate(paramsErrors.tokensEncoderParams, copy = copy)
  }
}
