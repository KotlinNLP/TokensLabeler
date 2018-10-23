/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.language

/**
 * The BIEOU schema.
 *
 * Starting from the this annotations it is possible to derive all the other main schemes (e.g. IOB, IOE).
 *
 * @property annotation the tag annotation
 */
enum class BIEOUTag(val annotation: String) {
  Beginning("B"),
  Inside("I"),
  End("E"),
  Outside("O"),
  Unit("U")
}