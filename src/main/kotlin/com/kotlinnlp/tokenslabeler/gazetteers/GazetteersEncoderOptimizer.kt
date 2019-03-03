/* Copyright 2017-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.gazetteers

import com.kotlinnlp.simplednn.core.functionalities.updatemethods.UpdateMethod
import com.kotlinnlp.simplednn.core.layers.StackedLayersParameters
import com.kotlinnlp.simplednn.core.optimizer.ParamsOptimizer
import com.kotlinnlp.tokensencoder.TokensEncoderOptimizer
import com.kotlinnlp.tokensencoder.TokensEncoderParameters

/**
 * @param updateMethod the update method helper (Learning Rate, ADAM, AdaGrad, ...)
 * @param model the model to optimize
 */
class GazetteersEncoderOptimizer(
  private val model: GazetteersEncoderModel,
  updateMethod: UpdateMethod<*>
) : TokensEncoderOptimizer(
  model = model,
  updateMethod = updateMethod
) {

  /**
   * The Optimizer of the outputEncoder parameters.
   */
  private val optimizer: ParamsOptimizer<StackedLayersParameters> =
    ParamsOptimizer(params = this.model.denseEncoder, updateMethod = this.updateMethod)

  /**
   * Update the parameters of the neural element associated to this optimizer.
   */
  override fun update() {
    this.optimizer.update()
  }

  /**
   * Accumulate the given params errors into the accumulator.
   *
   * @param paramsErrors the parameters errors to accumulate
   * @param copy a Boolean indicating if the params errors can be used as reference or must be copied. Set copy = false
   *             to optimize the accumulation when the amount of the errors to accumulate is 1. (default = true)
   */
  override fun accumulate(paramsErrors: TokensEncoderParameters, copy: Boolean) {

    paramsErrors as GazetteersEncoderParams

    this.optimizer.accumulate(paramsErrors.parameters, copy = copy)
  }
}