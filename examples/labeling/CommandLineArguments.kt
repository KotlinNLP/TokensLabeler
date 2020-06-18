/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * -----------------------------------------------------------------------------*/

package labeling

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
  val labelerModelPath: String by parser.storing(
    "-l",
    "--labeler",
    help="the file path of the labeler serialized model"
  )

  /**
   * The file path of the tokenizer serialized model.
   */
  val tokenizerModelPath: String by parser.storing(
    "-t",
    "--tokenizer",
    help="the file path of the tokenizer serialized model"
  )

  /**
   * The max number of parallel threads.
   */
  val parallelization: Int by parser.storing(
    "-p",
    "--parallelization",
    help="the max number of parallel threads"
  ) { toInt() }.default { 1 }

  /**
   * Force parsing all arguments (only read ones are parsed by default).
   */
  init {
    parser.force()
  }
}
