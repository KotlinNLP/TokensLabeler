/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package test

import com.kotlinnlp.neuraltokenizer.NeuralTokenizer
import com.kotlinnlp.neuraltokenizer.NeuralTokenizerModel
import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.*
import java.io.File
import java.io.FileInputStream

/**
 * A simple example to get the labeling of a text entered from the standard input.
 *
 * Command line arguments:
 *   1. The file path of the tokenizer serialized model.
 *   2. The file path of the labeler serialized model.
 */
fun main(args: Array<String>) {

  require(args.size == 2) {
    "Required 2 arguments: <tokenizer_model_filename> <labeler_model_filename>."
  }

  val tokenizer: NeuralTokenizer = args[0].let {
    println("Loading tokenizer model from '$it'...")
    NeuralTokenizer(NeuralTokenizerModel.load(FileInputStream(File(it))))
  }

  println("Loading model \"${args[1]}\"...")

  val labeler = TokensLabeler(TokensLabelerModel.load(FileInputStream(File(args[1]))))

  while (true) {

    val inputText = readValue()

    if (inputText.isEmpty()) {
      break

    } else {

      tokenizer.tokenize(inputText).forEach { s ->

        val sentence = BaseSentence(tokens = s.tokens.map { BaseToken(form = it.form, position = it.position) })

        sentence.tokens.zip(labeler.predict(sentence)).forEach { (token, label) ->
          println("${token.form}\t$label")
        }
      }
    }
  }

  println("\nExiting...")
}

/**
 * Read a value from the standard input.
 *
 * @return the string read
 */
private fun readValue(): String {

  print("\nAnalyze a text (empty to exit): ")

  return readLine()!!
}
