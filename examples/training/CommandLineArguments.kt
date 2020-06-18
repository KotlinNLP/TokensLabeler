/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * -----------------------------------------------------------------------------*/

package training

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

/**
 * The interpreter of command line arguments for the training script.
 *
 * @param args the array of command line arguments
 */
class CommandLineArguments(args: Array<String>) {

  /**
   * The parser of the string arguments.
   */
  private val parser = ArgParser(args)

  /**
   * The number of training epochs (default = 30).
   */
  val epochs: Int by parser.storing(
    "-e",
    "--epochs",
    help="the number of training epochs (default = 30)"
  ) { toInt() }.default(30)

  /**
   * The maximum number of sentences to load for training (default unlimited)
   */
  val maxSentences: Int? by parser.storing(
    "-s",
    "--max-sentences",
    help="the maximum number of sentences to load for training (default unlimited)"
  ) { toInt() }.default { null }

  /**
   * The file path of the training set.
   */
  val trainingSetPath: String by parser.storing(
    "-t",
    "--training-set",
    help="the file path of the training set"
  )

  /**
   * The file path of the validation set.
   */
  val validationSetPath: String by parser.storing(
    "-v",
    "--validation-set",
    help="the file path of the validation set"
  )

  /**
   * The name of the model.
   */
  val modelName: String by parser.storing(
    "-n",
    "--model-name",
    help="the name of the model"
  )

  /**
   * The path of the file in which to save the serialized model.
   */
  val modelPath: String by parser.storing(
    "-m",
    "--model-path",
    help="the path of the file in which to save the serialized model"
  )

  /**
   * The file path of the serialized gazetteers dictionary.
   */
  val gazetteersPath: String? by parser.storing(
    "-g",
    "--gazetteers",
    help="the file path of the serialized gazetteers dictionary"
  ).default { null }

  /**
   * The path of the pre-trained word embeddings.
   */
  val embeddingsPath: String by parser.storing(
    "-w",
    "--embeddings",
    help="the path of the pre-trained word embeddings"
  )

  /**
   * The size of the word embedding vectors.
   */
  val tokensEncodingSize: Int by parser.storing(
    "--tokens-encoding-size",
    help="the size of the tokens encoding vectors (default 100)"
  ) { toInt() }.default(100)

  /**
   * A set of comma-separated labels to include in the datasets (others will be ignored).
   */
  val includes: String? by parser.storing(
    "-i",
    "--includes",
    help="a set of comma-separated labels to include in the datasets (others will be ignored)"
  ).default { null }

  /**
   * Whether to ignore labels of the validation dataset that are missing in the model.
   */
  val ignoreMissingLabels: Boolean by parser.flagging(
    "--ignore-missing-labels",
    help = "whether to ignore labels of the validation dataset that are missing in the model"
  )

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
