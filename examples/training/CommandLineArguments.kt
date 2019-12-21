/* Copyright 2018-present LHRParser Authors. All Rights Reserved.
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
   * The language code
   */
  val langCode: String by parser.storing(
    "-l",
    "--language",
    help="the language ISO 639-1 code"
  )

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
   * The path of the file in which to save the serialized model.
   */
  val modelPath: String by parser.storing(
    "-m",
    "--model-path",
    help="the path of the file in which to save the serialized model"
  )

  /**
   * The path of the file from which to load the serialized dircet CharLM model.
   */
  val dirCharModelPath: String by parser.storing(
    "-c",
    "--charlm-dir",
    help="the path of the file from which to load the serialized dircet CharLM model"
  )

  /**
   * The path of the file in which to load the serialized CharLM model trained by reverse.
   */
  val revCharModelPath: String by parser.storing(
    "-r",
    "--charlm-rev",
    help="the path of the file in which to load the serialized CharLM model trained by reverse"
  )

  /**
   * The file path of the serialized gazetteers dictionary.
   */
  val gazetteersPath: String? by parser.storing(
    "-g",
    "--gazetteers",
    help="the file path of the serialized gazetteers"
  ).default { null }

  /**
   * The directory which contains pre-trained word embeddings.
   */
  val embeddingsDirname: String by parser.storing(
    "-w",
    "--word-emb-dir",
    help="the directory which contains pre-trained word embeddings"
  )

  /**
   * The size of the word embedding vectors.
   */
  val tokensEncodingSize: Int by parser.storing(
    "--tokens-encoding-size",
    help="the size of the tokens encoding vectors (default 100)"
  ){ toInt() }.default(100)

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
