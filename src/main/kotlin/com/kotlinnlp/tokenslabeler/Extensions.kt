/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.linguisticdescription.sentence.properties.AnnotatedSegment
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.tokenslabeler.language.IOBTag
import com.kotlinnlp.tokenslabeler.language.Label
import com.kotlinnlp.tokenslabeler.language.ScoredLabel
import kotlin.properties.Delegates

/**
 * A temporary segment used to build an [AnnotatedSegment]
 *
 * @property startToken the index of the first token of the segment
 * @property startChar the index of the first char of the segment
 * @property annotation the segment annotation
 * @param scoreInit the initial score of the segment
 */
private class Segment(val startToken: Int, val startChar: Int, val annotation: String, scoreInit: Double) {

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

/**
 * Transform a list of [Label]s into a list of [AnnotatedSegment]s.
 *
 * @param tokens the tokens with which the labels are associated
 *
 * @return a list of annotated segments
 */
fun List<ScoredLabel>.toSegments(tokens: List<RealToken>): List<AnnotatedSegment> {

  val segments: MutableList<Segment> = mutableListOf()

  (this + listOf(null)).zipWithNext().forEachIndexed { tokenIndex, (curLabel, nextLabel) ->

    curLabel!!

    if (curLabel.type == IOBTag.Beginning)
      segments.add(Segment(
        startToken = tokenIndex,
        startChar = tokens[tokenIndex].position.start,
        annotation = curLabel.value,
        scoreInit = curLabel.score))

    if (curLabel.type == IOBTag.Inside)
      segments.last().addScore(curLabel.score)

    if (curLabel.type != IOBTag.Outside && (nextLabel == null || nextLabel.type != IOBTag.Inside)) {
      segments.last().endToken = tokenIndex
      segments.last().endChar = tokens[tokenIndex].position.end
    }
  }

  return segments.map { it.toAnnotatedSegment() }
}
