package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.tokenslabeler.language.Label
import com.kotlinnlp.utils.BeamManager

/**
 * A scored label.
 *
 * @param label the label
 * @property score the score
 */
class ScoredLabel(label: Label, override var score: Double) : BeamManager.Value, Label(label.type, label.value)
