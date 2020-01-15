/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken

/**
 * @param labels a list of labels, one for each token
 *
 * @return a new sentence annotated with the given labels
 */
fun RealSentence<RealToken>.annotate(labels: List<Label>) = AnnotatedSentence(
  tokens = this.tokens.mapIndexed { tokenIndex, it ->
    AnnotatedToken(form = it.form, position = it.position, label = labels[tokenIndex]) },
  position = this.position
)