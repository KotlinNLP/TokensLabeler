/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.properties.AnnotatedSegment
import kotlin.properties.Delegates

/**
 * A temporary segment used to build an [AnnotatedSegment]
 *
 * @property startToken the index of the first token of the segment
 * @property startChar the index of the first char of the segment
 * @property annotation the segment annotation
 * @param scoreInit the initial score of the segment
 */
internal class Segment(val startToken: Int, val startChar: Int, val annotation: String, scoreInit: Double) {

  /**
   * The index of the last token of the segment.
   */
  var endToken: Int by Delegates.notNull()

  /**
   * The index of the last char of the segment.
   */
  var endChar: Int by Delegates.notNull()

  /**
   * The score accumulated.
   */
  private var scoreAcc: Double = scoreInit

  /**
   * The number of scores accumulated.
   */
  private var scoreCount = 1

  /**
   * Accumulate a score.
   *
   * @param value the value to accumulate
   */
  fun addScore(value: Double) {
    this.scoreAcc += value
    this.scoreCount++
  }

  /**
   * Build an [AnnotatedSegment] from this segment.
   *
   * @return a new annotated segment
   */
  fun toAnnotatedSegment() = AnnotatedSegment(
    startToken = this.startToken,
    endToken = this.endToken,
    startChar = this.startChar,
    endChar = this.endChar,
    annotation = this.annotation,
    score = this.scoreAcc / this.scoreCount)
}
