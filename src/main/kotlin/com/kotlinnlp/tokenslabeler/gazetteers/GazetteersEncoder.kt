/* Copyright 2017-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.gazetteers

import com.kotlinnlp.linguisticdescription.morphology.MorphologicalAnalysis
import com.kotlinnlp.linguisticdescription.morphology.Morphology
import com.kotlinnlp.linguisticdescription.morphology.POS
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.morphologicalanalyzer.MorphologicalAnalyzer
import com.kotlinnlp.simplednn.core.neuralprocessor.NeuralProcessor
import com.kotlinnlp.simplednn.core.neuralprocessor.batchfeedforward.BatchFeedforwardProcessor
import com.kotlinnlp.simplednn.core.optimizer.ParamsErrorsList
import com.kotlinnlp.simplednn.simplemath.ndarray.Shape
import com.kotlinnlp.simplednn.simplemath.ndarray.dense.DenseNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.sparsebinary.SparseBinaryNDArray
import com.kotlinnlp.simplednn.simplemath.ndarray.sparsebinary.SparseBinaryNDArrayFactory
import com.kotlinnlp.tokensencoder.TokensEncoder
import java.lang.RuntimeException

/**
 * The [TokensEncoder] that encodes each token of a sentence using the Gazetteers.
 *
 * @property model the model of this tokens encoder
 * @property id an identification number useful to track a specific processor
 */
class GazetteersEncoder(
  override val model: GazetteersEncoderModel,
  override val id: Int = 0
) : TokensEncoder<RealToken, RealSentence<RealToken>>() {

  /**
   * The position of a token within an entity.
   *
   * @property B begin
   * @property I inside
   * @property E end
   * @property U unit (a single token entity)
   * @property O outside (no part of an entity)
   */
  private enum class TokenEntityPosition { B, I, E, U, O }

  /**
   * The entity type.
   *
   * @property index the related index in the binary array
   */
  private enum class EntityType(val index: Int) {
    OUT(0),
    LOC_B(1),
    LOC_I(2),
    LOC_E(3),
    LOC_U(4),
    ORG_B(5),
    ORG_I(6),
    ORG_E(7),
    ORG_U(8),
    PER_B(9),
    PER_I(10),
    PER_E(11),
    PER_U(12);

    /**
     * Factory object.
     */
    companion object Factory {

      /**
       * @param pos the POS of an entity
       * @param position the token position within the entity
       *
       * @return the entity type with the given pos and position
       */
      operator fun invoke(pos: POS, position: TokenEntityPosition): EntityType = when(position) {
        TokenEntityPosition.B -> when(pos) {
          POS.NounProperLoc -> LOC_B
          POS.NounProperOrg -> ORG_B
          POS.NounProperPer -> PER_B
          else -> throw RuntimeException("Invalid entity type: $pos")
        }
        TokenEntityPosition.I -> when(pos) {
          POS.NounProperLoc -> LOC_I
          POS.NounProperOrg -> ORG_I
          POS.NounProperPer -> PER_I
          else -> throw RuntimeException("Invalid entity type: $pos")
        }
        TokenEntityPosition.E -> when(pos) {
          POS.NounProperLoc -> LOC_E
          POS.NounProperOrg -> ORG_E
          POS.NounProperPer -> PER_E
          else -> throw RuntimeException("Invalid entity type: $pos")
        }
        TokenEntityPosition.U -> when(pos) {
          POS.NounProperLoc -> LOC_U
          POS.NounProperOrg -> ORG_U
          POS.NounProperPer -> PER_U
          else -> throw RuntimeException("Invalid entity type: $pos")
        }
        else -> throw RuntimeException("Factory cannot be used for the OUT entity type.")
      }
    }
  }

  /**
   * The information about an entity.
   *
   * @property tokensRange the range of indices of tokens that compose the entity
   * @property types the possible types of the entity
   */
  private data class EntityInfo(val tokensRange: IntRange, val types: List<POS>) {

    /**
     * The number of tokens that compose this entity.
     */
    val size: Int = this.tokensRange.last - this.tokensRange.first + 1
  }

  /**
   * The morphological analyzer that works with the gazetteers dictionary.
   */
  private val analyzer = MorphologicalAnalyzer(
    dictionary = this.model.gazetteers,
    processDateTimes = false,
    processNumbers = false)

  /**
   * The feed-forward network used to transform the input from sparse to dense.
   */
  private val encoder: BatchFeedforwardProcessor<SparseBinaryNDArray> =
    BatchFeedforwardProcessor(model = this.model.denseEncoder, propagateToInput = false)

  /**
   * Encode a list of tokens.
   *
   * @param input an input sentence
   *
   * @return a list of dense encoded representations of the given sentence tokens
   */
  override fun forward(input: RealSentence<RealToken>): List<DenseNDArray> {

    val analysis = this.analyzer.analyze(input)

    val entitiesFeatures: List<SparseBinaryNDArray> =
      this.convertToBinaryFeatures(entities = this.getEntities(analysis), sentenceSize = input.tokens.size)

    return this.encoder.forward(entitiesFeatures)
  }

  /**
   * The Backward.
   *
   * @param outputErrors the errors of the current encoding
   */
  override fun backward(outputErrors: List<DenseNDArray>) = this.encoder.backward(outputErrors)

  /**
   * @param copy a Boolean indicating whether the returned errors must be a copy or a reference
   *
   * @return the errors of the model parameters
   */
  override fun getParamsErrors(copy: Boolean): ParamsErrorsList =
    this.encoder.getParamsErrors(copy = copy)

  /**
   * @param copy whether to return by value or by reference
   *
   * @return the input errors of the last backward
   */
  override fun getInputErrors(copy: Boolean) = NeuralProcessor.NoInputErrors

  /**
   * Get info about the entities defined in the sentence.
   * If the tokens ranges of two or more entities are overlapped then the entity with max length is kept.
   * In case the length is the same, the entity with max priority is kept, following this order of importance: LOC, PER,
   * ORG.
   *
   * @param analysis the morphological analysis
   *
   * @return the list of entities defined in the given sentence
   */
  private fun getEntities(analysis: MorphologicalAnalysis): List<EntityInfo> {

    val entities: List<EntityInfo> = analysis.multiWords
      .mapNotNull { it.morphologies.toEntity(tokensRange = it.startToken..it.endToken) }
      .sortedByDescending { it.size }
      .plus(analysis.tokensMorphologies.mapIndexedNotNull { i, m -> m.toEntity(tokensRange = i..i) })

    val validEntities: MutableList<EntityInfo> = mutableListOf()

    entities.forEach { entity ->

      // Note: it is enough to search for the first entity because they are sorted by descending size.
      val overlapEntity: EntityInfo? = validEntities.firstOrNull { it.tokensRange.hasIntersection(entity.tokensRange) }

      if (overlapEntity == null || overlapEntity.size == entity.size)
        validEntities.add(entity)
    }

    return validEntities
  }

  /**
   * @param other another int range
   *
   * @return true if this range has an intersection with the other, otherwise false
   */
  private fun IntRange.hasIntersection(other: IntRange): Boolean {

    var ret = false

    run loop@{
      this.forEach {
        if (it in other) {
          ret = true
          return@loop
        }
      }
    }

    return ret
  }

  /**
   * @param tokensRange the range of tokens to which this morphologies are assigned
   *
   * @return the entity info if this morphologies represent an entity, otherwise null
   */
  private fun List<Morphology>.toEntity(tokensRange: IntRange): EntityInfo? {

    val posSet: Set<POS> = this.asSequence().flatMap { it.components.asSequence().map { c -> c.pos } }.toSet()
    val posTypes: List<POS> = posSet.filter { p -> p != POS.NounProper && p.isComposedBy(POS.NounProper) }

    return if (posTypes.isNotEmpty())
      EntityInfo(tokensRange = tokensRange, types = posTypes)
    else
      null
  }

  /**
   * @param entities the list of entities defined in a sentence
   * @param sentenceSize the number of tokens in the sentence
   *
   * @return the list of binary features representing the entity annotation of each token
   */
  private fun convertToBinaryFeatures(entities: List<EntityInfo>, sentenceSize: Int): List<SparseBinaryNDArray> {

    val activeIndicesPerToken: List<MutableList<Int>> = List(size = sentenceSize, init = { mutableListOf<Int>() })

    entities.forEach { entity ->
      entity.types.forEach { pos ->

        if (entity.size == 1) {

          val index: Int = EntityType.Factory(pos = pos, position = TokenEntityPosition.U).index
          activeIndicesPerToken[entity.tokensRange.first].add(index)

        } else {

          val beginIndex: Int = EntityType.Factory(pos = pos, position = TokenEntityPosition.B).index
          activeIndicesPerToken[entity.tokensRange.first].add(beginIndex)

          val endIndex: Int = EntityType.Factory(pos = pos, position = TokenEntityPosition.E).index
          activeIndicesPerToken[entity.tokensRange.last].add(endIndex)

          (entity.tokensRange.first + 1 until entity.tokensRange.last).forEach { tokenIndex ->
            val insideIndex: Int = EntityType.Factory(pos = pos, position = TokenEntityPosition.I).index
            activeIndicesPerToken[tokenIndex].add(insideIndex)
          }
        }
      }
    }

    activeIndicesPerToken.forEach { if (it.isEmpty()) it.add(EntityType.OUT.index) }

    return activeIndicesPerToken.map {
      SparseBinaryNDArrayFactory.arrayOf(activeIndices = it, shape = Shape(this.model.inputSize))
    }
  }
}
