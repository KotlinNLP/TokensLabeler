/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.simplednn.deeplearning.birnn.deepbirnn.DeepBiRNNParameters
import com.kotlinnlp.tokensencoder.TokensEncoderParameters

/**
 * The parameters of the [TokensLabelerModel].
 *
 * @param tokensEncoderParams the params of the tokens encoder
 * @param biRNNParams the params of the biRNN
 */
data class TokensLabelerParameters(
  val tokensEncoderParams: TokensEncoderParameters,
  val biRNNParams: DeepBiRNNParameters
)