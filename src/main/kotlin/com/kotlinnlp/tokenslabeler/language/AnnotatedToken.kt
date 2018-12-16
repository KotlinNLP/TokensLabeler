/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.linguisticdescription.sentence.token.properties.Position

/**
 * A base form token.
 *
 * @property form the form of the token
 * @property position the position of the token in the original text
 */
data class AnnotatedToken(
  override val form: String,
  override val position: Position,
  val label: Label
) : RealToken {

  /**
   * @return the string representation of this token
   */
  override fun toString(): String = "${this.form}\t${this.label}"
}

