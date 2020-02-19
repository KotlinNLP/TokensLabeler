/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package training

import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.linguisticdescription.sentence.RealSentence
import com.kotlinnlp.linguisticdescription.sentence.flattenTokens
import com.kotlinnlp.linguisticdescription.sentence.token.RealToken
import com.kotlinnlp.morphologicalanalyzer.dictionary.MorphologyDictionary
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMap
import com.kotlinnlp.simplednn.core.functionalities.activations.Tanh
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.adam.ADAMMethod
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.layers.models.merge.mergeconfig.AffineMerge
import com.kotlinnlp.tokensencoder.TokensEncoderModel
import com.kotlinnlp.tokensencoder.charactersbirnn.CharsBiRNNEncoderModel
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel
import com.kotlinnlp.tokensencoder.embeddings.keyextractor.NormWordKeyExtractor
import com.kotlinnlp.tokensencoder.embeddings.keyextractor.WordKeyExtractor
import com.kotlinnlp.tokensencoder.ensemble.EnsembleTokensEncoderModel
import com.kotlinnlp.tokensencoder.wrapper.TokensEncoderWrapperModel
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.gazetteers.GazetteersEncoderModel
import com.kotlinnlp.tokenslabeler.helpers.DatasetReader
import com.kotlinnlp.tokenslabeler.helpers.Trainer
import com.kotlinnlp.tokenslabeler.helpers.Evaluator
import com.kotlinnlp.tokenslabeler.helpers.LabelsStatistics
import com.kotlinnlp.tokenslabeler.language.*
import com.xenomachina.argparser.mainBody
import java.io.File
import java.io.FileInputStream

/**
 * Train the [TokensLabelerModel].
 *
 * Launch with the '-h' option for help about the command line arguments.
 */
fun main(args: Array<String>) = mainBody {

  val parsedArgs = CommandLineArguments(args)

  val trainingSentences: List<RealSentence<AnnotatedToken>> = DatasetReader(
    type = "training",
    filePath = parsedArgs.trainingSetPath,
    includes = parsedArgs.includes?.split(",")?.toSet(),
    maxSentences = parsedArgs.maxSentences).loadSentences()

  val testSentences: List<RealSentence<AnnotatedToken>> = DatasetReader(
    type = "test",
    filePath = parsedArgs.validationSetPath,
    includes = parsedArgs.includes?.split(",")?.toSet(),
    maxSentences = null).loadSentences()

  val dictionary: CorpusDictionary = trainingSentences.let {
    println("Creating corpus dictionary...")
    CorpusDictionary(it)
  }

  val model = TokensLabelerModel(
    language = getLanguageByIso(parsedArgs.langCode),
    tokensEncoderModel = buildTokensEncoderModel(parsedArgs = parsedArgs, trainingSentences = trainingSentences),
    biRNNConnectionType = LayerType.Connection.LSTM,
    biRNNActivation = Tanh(),
    biRNNHiddenSize = 200,
    numOfBiRNNLayers = TokensLabelerModel.BiRNNLayersNumber.Single,
    inputDropout = 0.25,
    labelerDropout = 0.0,
    outputLabels = dictionary.labels)

  println("\n-- START TRAINING ON %d SENTENCES".format(trainingSentences.size))

  Trainer(
    model = model,
    modelFilename = parsedArgs.modelPath,
    dataset = trainingSentences,
    epochs = parsedArgs.epochs,
    updateMethod = ADAMMethod(stepSize = 0.001, beta1 = 0.9, beta2 = 0.999),
    evaluator = Evaluator(
      model = model,
      testSentences = testSentences,
      ignoreMissingLabels = parsedArgs.ignoreMissingLabels),
    verbose = true
  ).train()

  println("\n-- START FINAL VALIDATION ON %d SENTENCES".format(testSentences.size))

  // Load the best model.
  val validationModel = TokensLabelerModel.load(FileInputStream(File(parsedArgs.modelPath)))
  val bestStats: LabelsStatistics = Evaluator(model = validationModel, testSentences = testSentences).evaluate()

  println("\nBest statistics:")
  println(bestStats)
}

/**
 * Build a tokens-encoder model.
 *
 * @param parsedArgs the parsed command line arguments
 * @param trainingSentences the list of training sentences
 *
 * @return a new tokens-encoder model
 */
private fun buildTokensEncoderModel(
  parsedArgs: CommandLineArguments,
  trainingSentences: List<RealSentence<AnnotatedToken>>
): TokensEncoderModel<RealToken, RealSentence<RealToken>> = EnsembleTokensEncoderModel(
  components = listOfNotNull(
    buildEmbeddingsEncoderComponent(parsedArgs),
    buildCharsEncoderComponent(trainingSentences),
    buildGazetteersEncoderModel(parsedArgs)
  ),
  outputMergeConfiguration = AffineMerge(outputSize = parsedArgs.tokensEncodingSize, activationFunction = null))

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return the encoder component that encodes with words embeddings
 */
private fun buildEmbeddingsEncoderComponent(parsedArgs: CommandLineArguments) =
  EnsembleTokensEncoderModel.ComponentModel(
    model = TokensEncoderWrapperModel(
      model = EmbeddingsEncoderModel.Base(
        embeddingsMap = EmbeddingsMap.load(parsedArgs.embeddingsPath),
        embeddingKeyExtractor = WordKeyExtractor(),
        fallbackEmbeddingKeyExtractors = listOf(NormWordKeyExtractor()),
        dropout = 0.0),
      converter = FormConverter()),
    trainable = false)

/**
 * @param trainingSentences the list of training sentences
 *
 * @return the encoder component that encodes with chars embeddings
 */
private fun buildCharsEncoderComponent(trainingSentences: List<RealSentence<AnnotatedToken>>) =
  EnsembleTokensEncoderModel.ComponentModel(
    model = TokensEncoderWrapperModel(
      model = CharsBiRNNEncoderModel(words = trainingSentences.flattenTokens().map { it.form }),
      converter = FormConverter()),
    trainable = true)
/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return the encoder component that encodes with a gazetteers dictionary
 */
private fun buildGazetteersEncoderModel(parsedArgs: CommandLineArguments) = parsedArgs.gazetteersPath?.let {

  println("Loading serialized gazetteers from '$it'...")
  val gazetteers = MorphologyDictionary.load(FileInputStream(File(it)))

  EnsembleTokensEncoderModel.ComponentModel(
    model = GazetteersEncoderModel(tokenEncodingSize = 50, activation = Tanh(), gazetteers = gazetteers),
    trainable = true)
}
