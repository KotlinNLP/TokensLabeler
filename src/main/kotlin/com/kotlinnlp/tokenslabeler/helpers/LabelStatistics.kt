package com.kotlinnlp.tokenslabeler.helpers

/**
 * The statistics of a single [Label].
 *
 * @property truePositive number of times where the model correctly predicts the positive class
 * @property falsePositive number of times where the model incorrectly predicts the positive class
 * @property falseNegative number of times where the model incorrectly predicts the negative class
 */
data class LabelStatistics(
  var label: String,
  var truePositive: Int = 0,
  var falsePositive: Int = 0,
  var falseNegative: Int = 0
) {

  /**
   * The precision.
   */
  @Suppress("unused")
  val precision: Double get() =
    this.truePositive.toDouble() / (this.truePositive + this.falsePositive)

  /**
   * The recall.
   */
  @Suppress("unused")
  val recall: Double get() =
    this.truePositive.toDouble() / (this.truePositive + this.falseNegative)

  /**
   * The F-measure (F-1) is the harmonic mean of the precision and the recall.
   */
  val f1: Double get() {

    val pr = this.precision + this.recall

    return if (pr > 0.0) 2.0 * (this.precision * this.recall) / (this.precision + this.recall) else 0.0
  }

  /**
   * @return the string representation
   */
  override fun toString() = "label: %s | precision: %.2f | recall: %.2f | f-measure: %.2f"
    .format(this.label, this.precision, this.recall, this.f1)
}