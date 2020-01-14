/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import java.io.Serializable

/**
 * The Label.
 *
 * @property type the IOB tag
 * @property value the name of the label (e.g. "PER", "LOC", "ORG")
 */
open class Label(var type: IOBTag, var value: String = EMPTY_VALUE) : Serializable {

  companion object {

    /**
     * The empty value.
     */
    const val EMPTY_VALUE = ""

    /**
     * Private val used to serialize the class (needed by Serializable).
     */
    @Suppress("unused")
    private const val serialVersionUID: Long = 1L
  }

  /**
   * @return the string representation of this [Label]
   */
  override fun toString(): String = if (this.value.isNotEmpty())
    "${this.type.annotation}-${this.value}"
  else
    this.type.annotation

  override fun hashCode(): Int = this.type.hashCode() + 31 * this.value.hashCode()

  override fun equals(other: Any?): Boolean {

    if (other !is Label) return false

    return this.type == other.type && this.value == other.value
  }
}
