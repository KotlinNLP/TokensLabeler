/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.BIEOUTag
import com.kotlinnlp.tokenslabeler.language.Label
import com.kotlinnlp.utils.BeamManager
import com.kotlinnlp.utils.DictionarySet
import com.kotlinnlp.utils.notEmptyOr

/**
 * @param predictions the predictions resulting from the neural model
 * @param model the neural model
 * @param maxBeamSize the max number of parallel states that the beam supports (-1 = infinite)
 * @param maxForkSize the max number of forks that can be generated from a state (-1 = infinite)
 * @param maxIterations the max number of iterations of solving steps (it is the depth of beam recursion, -1 = infinite)
 */
internal class BeamDecoder(
  predictions: List<DenseNDArray>,
  model: TokensLabelerModel,
  maxBeamSize: Int = 5,
  maxForkSize: Int = 3,
  maxIterations: Int = 10
) : BeamManager<BeamDecoder.ScoredLabel, BeamDecoder.LabeledState>(
  valuesMap = getValuesMap(predictions, model.outputLabels),
  maxBeamSize = maxBeamSize,
  maxForkSize = maxForkSize,
  maxIterations = maxIterations
) {

  companion object {

    /**
     * @return the map of element ids associated to their possible values, sorted by descending score
     */
    private fun getValuesMap(predictions: List<DenseNDArray>, outputLabels: DictionarySet<Label>): Map<Int, List<ScoredLabel>> {

      return predictions.indices.associate { tokenIndex ->

        val threshold: Double = 1.0 / predictions[tokenIndex].length // it is the distribution mean

        val sortedLabels = predictions[tokenIndex].argSorted(reverse = true).map {
          ScoredLabel(label = outputLabels.getElement(it)!!, score = predictions[tokenIndex][it])
        }

        tokenIndex to sortedLabels.filter { it.score >= threshold }.notEmptyOr { sortedLabels }
      }
    }

    /**
     * TODO: extend to other annotation schemas
     *
     * Map each tag with all the possible valid tags that can be found before it.
     */
    private val validSequences = mapOf(
      BIEOUTag.Inside to setOf(null, BIEOUTag.Outside, BIEOUTag.Inside, BIEOUTag.End),
      BIEOUTag.End to setOf(BIEOUTag.Inside),
      BIEOUTag.Outside to setOf(null, BIEOUTag.Outside, BIEOUTag.End))

    /**
     * @param prevLabel the previous label
     *
     * @return whether the label can follow the previous label
     */
    private fun canFollow(curLabel: Label, prevLabel: Label?): Boolean =
      validSequences.getValue(curLabel.type).contains(prevLabel?.type) &&
        (prevLabel == null || prevLabel.value.isEmpty() || prevLabel.value == curLabel.value)
  }

  /**
   * An label of a state
   *
   * @property score the score
   */
  internal data class ScoredLabel(val label: Label, override var score: Double) : Value()

  /**
   * The state that contains the configuration of a possible [ScoredLabel].
   *
   * @param elements the list of elements in this state, sorted by diff score
   */
  internal inner class LabeledState(elements: List<StateElement<ScoredLabel>>) : State(elements) {

    /**
     * The global score of the state.
     */
    override val score: Double get() = elements.map { it.value.score }.average()

    /**
     * Whether the state contains a correct sequence according to the constraints of the chosen BIEOU annotation schema.
     */
    override var isValid: Boolean = true

    /**
     * The sequence of labels.
     */
    val sequence: List<Label>

    /**
     * Initialize the sequence.
     */
    init {

      var prevLabel: Label? = null

      this.sequence = this.elements.indices.map { tokenIndex ->

        val curLabel = this.getElement(tokenIndex).value.label

        if (!canFollow(curLabel, prevLabel))
          this.isValid = false // TODO: break

        prevLabel = curLabel
        curLabel
      }
    }
  }

  /**
   * Build a new state with the given elements.
   *
   * @param elements the elements that compose the building state
   *
   * @return a new state with the given elements
   */
  override fun buildState(elements: List<StateElement<ScoredLabel>>): LabeledState = LabeledState(elements)
}
