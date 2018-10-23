/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package test

import com.kotlinnlp.tokenslabeler.TokensLabeler
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.language.*
import java.io.File
import java.io.FileInputStream

/**
 * Test the [TokensLabelerModel].
 */
fun main(args: Array<String>) {

  println("Loading model \"${args[0]}\"...")

  val labeler = TokensLabeler(TokensLabelerModel.load(FileInputStream(File(args[0]))))

  while (true) {

    val inputText = readValue()

    if (inputText.isEmpty()) {

      break

    } else {

      val sentence = BaseSentence(tokens = inputText.split(" ").map { BaseToken(it) }) // TODO: use the NeuralTokenizer

      sentence.tokens.zip(labeler.predict(sentence)).forEach { (token, label) ->
        println("${token.form}\t$label")
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

  print("\nType the beginning of the sequence. Even a single character (empty to exit): ")

  return readLine()!!
}
