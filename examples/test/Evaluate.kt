/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package test

import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.helpers.DatasetReader
import com.kotlinnlp.tokenslabeler.helpers.LabelStatistics
import com.kotlinnlp.tokenslabeler.helpers.Validator
import com.kotlinnlp.tokenslabeler.language.*
import com.kotlinnlp.utils.Timer
import java.io.File
import java.io.FileInputStream

/**
 * Evaluate a [TokensLabelerModel] on a test set.
 *
 * Command line arguments:
 *   1. The model
 *   2. The file path of the test set
 */
fun main(args: Array<String>) {

  require(args.size == 2) {
    "Required 2 arguments: <labeler_model_filename> <test_set_filename>"
  }

  println("Loading model from \"${args[0]}\"...")
  val model = TokensLabelerModel.load(FileInputStream(File(args[0])))

  val testSentences: List<AnnotatedSentence> = DatasetReader(
    type = "test",
    filePath = args[1],
    useOPlus = false,
    useBIEOU = true,
    maxSentences = null
  ).loadSentences()

  val validator = Validator(model = model, testSentences = testSentences)
  val timer = Timer()

  println("\nEvaluating the model on ${testSentences.size} test sentences...")
  val statistics: Map<String, LabelStatistics> = validator.evaluate()
  val accuracy: Double = statistics.values.sumByDouble { it.f1 } / statistics.size
  println("Elapsed time: ${timer.formatElapsedTime()}")

  println()
  statistics.values.forEach { println(it) }

  println("\nACCURACY: %.2f %%".format(100.0 * accuracy))
}
