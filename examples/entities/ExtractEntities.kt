/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package entities

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.properties.AnnotatedSegment
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.utils.Timer
import com.kotlinnlp.utils.pmapIndexed
import com.kotlinnlp.utils.toTitleCase
import com.xenomachina.argparser.mainBody
import parseAsBase64
import readInput
import java.io.File
import java.io.FileInputStream

/**
 * Extract the set of labeled entities from a text inserted from the standard input or parsed from a document.
 * If the inserted text is an existing path, then the referenced document is read and the textual content is parsed
 * using Apache Tika.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)

  val tokenizer: NeuralTokenizer = parsedArgs.tokenizerModelPath.let {
    println("Loading tokenizer model from '$it'...")
    NeuralTokenizer(NeuralTokenizerModel.load(FileInputStream(it)))
  }
  val labelerModel: TokensLabelerModel = parsedArgs.labelerModelPath.let {
    println("Loading labeler model from '$it'...")
    TokensLabelerModel.load(FileInputStream(it))
  }
  val labelers: List<TokensLabeler> = List(parsedArgs.parallelization) { TokensLabeler(labelerModel) }

  println("Parallelization: ${parsedArgs.parallelization}")

  while (true) {

    readInput()?.let { input ->

      val inputText: String = if (File(input).exists()) parseAsBase64(input) else input
      val timer = Timer()

      val entities: Set<Entity> = extractEntities(
        text = inputText,
        parallelization = parsedArgs.parallelization,
        tokenizer = tokenizer,
        labelers = labelers)

      println("Elapsed time: ${timer.formatElapsedTime()}")

      println("${entities.size} entities found:")
      entities.sortedByDescending { it.score }.forEach { println(it) }

    } ?: break
  }

  println("\nExiting...")
}

/**
 * An entity.
 *
 * @property type the entity type
 * @property value the entity value
 * @property score the confidence score
 */
private data class Entity(val type: String, val value: String, val score: Double) {

  override fun hashCode(): Int = this.type.hashCode() + 31 * this.value.hashCode()

  override fun equals(other: Any?): Boolean {

    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Entity

    if (type != other.type) return false
    if (value != other.value) return false

    return true
  }

  override fun toString(): String = "[$type] $value (%.1f %%)".format(100.0 * this.score)
}

/**
 * Extract entities from a text.
 *
 * @param text the input text
 * @param parallelization the max number of parallel threads
 * @param tokenizer a tokenizer
 * @param labelers an amount of labelers equal to the [parallelization]
 *
 * @return the entities found
 */
private fun extractEntities(text: String,
                            parallelization: Int,
                            tokenizer: NeuralTokenizer,
                            labelers: List<TokensLabeler>): Set<Entity> =
  tokenizer.tokenize(text)
    .pmapIndexed(parallelization) { i, sentence ->

      @Suppress("UNCHECKED_CAST")
      val segments: List<AnnotatedSegment> = labelers[i].predictAsSegments(sentence as RealSentence<RealToken>)

      segments.map {
        Entity(
          type = it.annotation,
          value = it.getRefTokens(sentence.tokens).joinToString(" ") { t -> t.form.toTitleCase() },
          score = it.score)
      }
    }
    .flatten()
    .toSet()
