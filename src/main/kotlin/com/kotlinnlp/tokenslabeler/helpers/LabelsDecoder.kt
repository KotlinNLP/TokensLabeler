/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.IOBTag
import com.kotlinnlp.tokenslabeler.language.Label
import com.kotlinnlp.tokenslabeler.language.ScoredLabel
import com.kotlinnlp.utils.BeamManager
import com.kotlinnlp.utils.DictionarySet
import com.kotlinnlp.utils.notEmptyOr

/**
 * The decoder of predictions made by the Tokens Labeler.
 * It finds the best sequence of labels base on the given predictions.
 *
 * @param predictions predictions made by the Tokens Labeler
 * @param model the neural model
 * @param maxBeamSize the max number of parallel states that the beam supports (-1 = infinite)
 * @param maxForkSize the max number of forks that can be generated from a state (-1 = infinite)
 * @param maxIterations the max number of iterations of solving steps (it is the depth of beam recursion, -1 = infinite)
 */
internal class LabelsDecoder(
  predictions: List<DenseNDArray>,
  model: TokensLabelerModel,
  maxBeamSize: Int = 5,
  maxForkSize: Int = 3,
  maxIterations: Int = 10
) : BeamManager<ScoredLabel, LabelsDecoder.LabeledState>(
  valuesMap = getValuesMap(predictions, model.outputLabels),
  maxBeamSize = maxBeamSize,
  maxForkSize = maxForkSize,
  maxIterations = maxIterations
) {

  companion object {

    /**
     * @return the map of element ids associated to their possible values, sorted by descending score
     */
    private fun getValuesMap(predictions: List<DenseNDArray>,
                             outputLabels: DictionarySet<Label>): Map<Int, List<ScoredLabel>> =

      predictions.indices.associate { tokenIndex ->

        val threshold: Double = 1.0 / predictions[tokenIndex].length // it is the distribution mean

        val sortedLabels = predictions[tokenIndex].argSorted(reverse = true).map {
          ScoredLabel(label = outputLabels.getElement(it)!!, score = predictions[tokenIndex][it])
        }

        tokenIndex to sortedLabels.filter { it.score >= threshold }.notEmptyOr { sortedLabels }
      }

    /**
     * Map each tag with all the tags that it can follow.
     */
    private val validPrevious = mapOf(
      IOBTag.Beginning to setOf(null, IOBTag.Outside, IOBTag.Inside, IOBTag.Beginning),
      IOBTag.Outside to setOf(null, IOBTag.Outside, IOBTag.Inside, IOBTag.Beginning),
      IOBTag.Inside to setOf(IOBTag.Beginning, IOBTag.Inside),
      null to setOf(IOBTag.Outside, IOBTag.Inside, IOBTag.Beginning))

    /**
     * @param curLabel the current label
     * @param prevLabel the previous label
     *
     * @return whether the current label can follow the previous label
     */
    fun canFollow(curLabel: Label?, prevLabel: Label?): Boolean =
      prevLabel?.type in this.validPrevious.getValue(curLabel?.type) &&
        (prevLabel?.value.isNullOrEmpty() || curLabel?.value.isNullOrEmpty() || prevLabel!!.value == curLabel!!.value)
  }

  /**
   * The state that contains the configuration of a possible [ScoredLabel].
   *
   * @param elements the list of elements in this state, sorted by diff score
   */
  internal inner class LabeledState(elements: List<StateElement<ScoredLabel>>) : State(elements) {

    /**
     * The global score of the state.
     */
    override val score: Double = elements.asSequence().map { it.value.score }.average()

    /**
     * Whether the state contains a correct sequence according to the constraints of the chosen BIEOU annotation schema.
     */
    override var isValid: Boolean =
      sequenceOf(null)
        .plus(this.elements.indices.asSequence().map { i -> this.getElement(i).value })
        .plus(sequenceOf(null))
        .zipWithNext()
        .all { canFollow(prevLabel = it.first, curLabel = it.second) }
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
