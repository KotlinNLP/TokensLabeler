/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package evaluate

import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.helpers.DatasetReader
import com.kotlinnlp.tokenslabeler.helpers.Evaluator
import com.kotlinnlp.tokenslabeler.helpers.LabelsStatistics
import com.kotlinnlp.tokenslabeler.language.*
import com.kotlinnlp.utils.Timer
import java.io.File
import java.io.FileInputStream

/**
 * Evaluate a [TokensLabelerModel] on a test set.
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) {

  val parsedArgs = CommandLineArguments(args)

  val model = parsedArgs.modelPath.let {
    println("Loading model from '$it'...")
    TokensLabelerModel.load(FileInputStream(File(it)))
  }

  val testSentences: List<AnnotatedSentence> = DatasetReader(
    type = "text",
    filePath = parsedArgs.validationSetPath,
    includes = parsedArgs.includes?.split(",")?.toSet(),
    maxSentences = null
  ).loadSentences()

  val evaluator = Evaluator(model = model, testSentences = testSentences)
  val timer = Timer()

  println("\nEvaluating the model on ${testSentences.size} test sentences...")
  val statistics: LabelsStatistics = evaluator.evaluate()
  println("Elapsed time: ${timer.formatElapsedTime()}")

  println()
  println(statistics)

  println("\nACCURACY: %.2f %%".format(100.0 * statistics.accuracy))
}
