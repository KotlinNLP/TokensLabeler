/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package labeling

import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.*
import com.kotlinnlp.utils.pmapIndexed
import com.xenomachina.argparser.mainBody
import parseAsBase64
import readInput
import java.io.File
import java.io.FileInputStream

/**
 * Get the labeling of a text inserted from the standard input or parsed from a document.
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

    readInput()?.let {

      val inputText: String = if (File(it).exists()) parseAsBase64(it) else it

      tokenizer.tokenize(inputText).pmapIndexed(parsedArgs.parallelization) { i, sentence ->

        @Suppress("UNCHECKED_CAST")
        sentence as RealSentence<RealToken>

        println(sentence.annotate(labelers[i].predict(sentence)).toString() + "\n")
      }

    } ?: break
  }

  println("\nExiting...")
}
