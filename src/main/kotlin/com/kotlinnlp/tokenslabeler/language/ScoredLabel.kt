/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

import com.kotlinnlp.utils.BeamManager

/**
 * A scored label.
 *
 * @param label the label
 * @property score the score
 */
class ScoredLabel(label: Label, override var score: Double) : BeamManager.Value, Label(label.type, label.value)
