/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package training

import com.kotlinnlp.languagemodel.CharLM
import com.kotlinnlp.linguisticdescription.language.getLanguageByIso
import com.kotlinnlp.simplednn.core.embeddings.EMBDLoader
import com.kotlinnlp.simplednn.core.embeddings.EmbeddingsMapByDictionary
import com.kotlinnlp.simplednn.core.functionalities.activations.Tanh
import com.kotlinnlp.simplednn.core.functionalities.updatemethods.adam.ADAMMethod
import com.kotlinnlp.simplednn.core.layers.LayerType
import com.kotlinnlp.simplednn.core.layers.models.merge.mergeconfig.AffineMerge
import com.kotlinnlp.simplednn.core.layers.models.merge.mergeconfig.ConcatMerge
import com.kotlinnlp.tokensencoder.TokensEncoderModel
import com.kotlinnlp.tokensencoder.charlm.CharLMEncoderModel
import com.kotlinnlp.tokensencoder.embeddings.EmbeddingsEncoderModel
import com.kotlinnlp.tokensencoder.embeddings.keyextractor.WordKeyExtractor
import com.kotlinnlp.tokensencoder.ensemble.EnsembleTokensEncoderModel
import com.kotlinnlp.tokensencoder.wrapper.MirrorConverter
import com.kotlinnlp.tokensencoder.wrapper.TokensEncoderWrapperModel
import com.kotlinnlp.tokenslabeler.TokensLabelerModel
import com.kotlinnlp.tokenslabeler.helpers.DatasetReader
import com.kotlinnlp.tokenslabeler.helpers.SchemeConverter
import com.kotlinnlp.tokenslabeler.helpers.Trainer
import com.kotlinnlp.tokenslabeler.helpers.Validator
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

  val tagConverter = SchemeConverter.BioToIoe2()

  val trainingSentences: List<AnnotatedSentence> = DatasetReader(
    type = "training",
    filePath = parsedArgs.trainingSetPath,
    schemeConverter = tagConverter,
    maxSentences = parsedArgs.maxSentences).loadSentences()

  val testSentences: List<AnnotatedSentence> = DatasetReader(
    type = "test",
    filePath = parsedArgs.validationSetPath,
    schemeConverter = tagConverter,
    maxSentences = null).loadSentences()

  val dictionary: CorpusDictionary = trainingSentences.let {
    println("Creating corpus dictionary...")
    CorpusDictionary(it)
  }

  val model = TokensLabelerModel(
    language = getLanguageByIso(parsedArgs.langCode),
    tokensEncoderModel = buildTokensEncoderModel(parsedArgs = parsedArgs),
    biRNNConnectionType = LayerType.Connection.LSTM,
    biRNNActivation = Tanh(),
    biRNNHiddenSize = 200,
    biRNNLayers = 1,
    inputDropout = 0.25,
    labelerDropout = 0.0,
    outputLabels = dictionary.labels)

  val trainer = Trainer(
    model = model,
    modelFilename = parsedArgs.modelPath,
    epochs = parsedArgs.epochs,
    updateMethod = ADAMMethod(stepSize = 0.001, beta1 = 0.9, beta2 = 0.999),
    validator = Validator(model = model, testSentences = testSentences),
    verbose = true)

  trainer.train(trainingSentences)
}

/**
 * Build a tokens-encoder model.
 *
 * @param parsedArgs the parsed command line arguments
 *
 * @return a new tokens-encoder model
 */
private fun buildTokensEncoderModel(
  parsedArgs: CommandLineArguments
): TokensEncoderModel<BaseToken, BaseSentence> {

  return TokensEncoderWrapperModel(
    model = EnsembleTokensEncoderModel(
      components = loadEmbeddingsMaps(parsedArgs.embeddingsDirname!!).map { preEmbeddingsMap ->
        EnsembleTokensEncoderModel.ComponentModel(buildEmbeddingsEncoder(preEmbeddingsMap, 0.0), trainable = false)
      } + EnsembleTokensEncoderModel.ComponentModel(buildCharLMEncoder(parsedArgs)),
      outputMergeConfiguration = AffineMerge(
        outputSize = parsedArgs.tokensEncodingSize,
        activationFunction = null)),
    converter = MirrorConverter())
}

/**
 * @param embeddingsDirname the directory containing the embeddings vectors files
 */
fun loadEmbeddingsMaps(embeddingsDirname: String): List<EmbeddingsMapByDictionary> {

  println("Loading embeddings from '$embeddingsDirname'")
  val embeddingsDir = File(embeddingsDirname)

  require(embeddingsDir.isDirectory) { "$embeddingsDirname is not a directory" }

  return embeddingsDir.listFilesOrRaise().map { embeddingsFile ->

    println("Loading pre-trained word embeddings from '${embeddingsFile.name}'...")
    EMBDLoader(verbose = true).load(embeddingsFile.absolutePath.toString())
  }
}

/**
 * @param embeddingsMap the embeddings map
 * @param dropout the dropout
 *
 * @return the tokens encoder that works on the [embeddingsMap]
 */
fun buildEmbeddingsEncoder(embeddingsMap: EmbeddingsMapByDictionary, dropout: Double) = TokensEncoderWrapperModel (
  model = EmbeddingsEncoderModel(
    embeddingsMap = embeddingsMap,
    embeddingKeyExtractor = WordKeyExtractor(),
    dropoutCoefficient = dropout),
  converter = BaseConverter())

/**
 * @param parsedArgs the parsed command line arguments
 *
 * @return the tokens encoder that uses the chars language model
 */
fun buildCharLMEncoder(parsedArgs: CommandLineArguments) = TokensEncoderWrapperModel(
  model = CharLMEncoderModel(
    charLM = CharLM.load(FileInputStream(File(parsedArgs.charModelPath))),
    revCharLM = CharLM.load(FileInputStream(File(parsedArgs.revCharModelPath))),
    outputMergeConfiguration = ConcatMerge()),
  converter = FormConverter())

/**
 * @throws RuntimeException if the file of this directory is empty
 *
 * @return the list of files contained in this directory if it is not empty, otherwise an exception is raised
 */
private fun File.listFilesOrRaise(): Array<File> = this.listFiles().let {
  if (it.isNotEmpty()) it else throw RuntimeException("Empty directory.")
}