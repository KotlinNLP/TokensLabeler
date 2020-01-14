/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.simplednn.helpers.Statistics
import com.kotlinnlp.utils.stats.MetricCounter

/**
 * The statistics of predicted labels.
 *
 * @param labels the possible labels
 */
class LabelsStatistics(labels: Set<String>) : Statistics() {

  /**
   * The metric counters per label.
   */
  val metrics: Map<String, MetricCounter> = labels.associate { it to MetricCounter() }

  /**
   * Reset the metrics.
   */
  override fun reset() {

    this.accuracy = 0.0

    this.metrics.values.forEach { it.reset() }
  }

  /**
   * @return the string representation of this statistics
   */
  override fun toString(): String = this.metrics.entries
    .sortedBy { it.key }
    .joinToString("\n") { (label, metric) -> "label: $label | $metric" }
}
