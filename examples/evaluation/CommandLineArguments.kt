/* Copyright 2018-present LHRParser Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * -----------------------------------------------------------------------------*/

package evaluation

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
   * The file path of the labeler serialized model.
   */
  val modelPath: String by parser.storing(
    "-m",
    "--model-path",
    help="the file path of the labeler serialized model"
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
   * A set of comma-separated labels to include in the dataset (others will be ignored).
   */
  val includes: String? by parser.storing(
    "-i",
    "--includes",
    help="a set of comma-separated labels to include in the dataset (others will be ignored)"
  ).default { null }

  /**
   * The maximum number of sentences to load for the validation (default unlimited)
   */
  val maxSentences: Int? by parser.storing(
    "-s",
    "--max-sentences",
    help="the maximum number of sentences to load for the validation (default unlimited)"
  ) { toInt() }.default { null }

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
