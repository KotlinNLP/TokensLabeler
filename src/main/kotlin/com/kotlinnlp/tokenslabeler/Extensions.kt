/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler

import com.kotlinnlp.linguisticdescription.sentence.properties.AnnotatedSegment
import com.kotlinnlp.tokenslabeler.language.BIEOUTag
import com.kotlinnlp.tokenslabeler.language.Label

/**
 * Transform a list of [Label]s into a list of [AnnotatedSegment]s.
 *
 * @return a list of annotated segments
 */
fun List<Label>.toSegments(): List<AnnotatedSegment> {

  val starts = mutableListOf<Int>()
  val ends = mutableListOf<Int>()
  val annotations = mutableListOf<String>()

  this.forEachIndexed { tokenIndex, label ->
    when {
      label.type == BIEOUTag.Unit -> { starts.add(tokenIndex); ends.add(tokenIndex); annotations.add(label.value) }
      label.type == BIEOUTag.Beginning -> { starts.add(tokenIndex); annotations.add(label.value) }
      label.type == BIEOUTag.End -> ends.add(tokenIndex)
    }
  }

  require(starts.size == ends.size)

  return starts.zip(ends).zip(annotations) { (start, end), annotation ->
    AnnotatedSegment(startToken = start, endToken = end, annotation = annotation)
  }
}