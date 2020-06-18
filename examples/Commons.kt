/* Copyright 2020-present Simone Cangialosi. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.sax.BodyContentHandler
import java.io.File
import java.io.StringWriter
import java.util.*

/**
 * Read a value from the standard input.
 *
 * @return the string read or `null` if the input was blank
 */
internal fun readInput(): String? {

  print("\nInsert a text or a document path (empty to exit): ")

  return readLine()!!.ifBlank { null }
}

/**
 * Parse a document with Apache Tika.
 *
 * @param docPath the document path
 *
 * @return the textual content of the given document
 */
internal fun parseAsBase64(docPath: String): String {

  val document: String = Base64.getEncoder().encodeToString(File(docPath).readBytes())
  val parser = AutoDetectParser()
  val handler = BodyContentHandler(StringWriter())
  val metadata = Metadata()
  val byteContent: ByteArray = Base64.getDecoder().decode(document)

  @Suppress("DEPRECATION")
  parser.parse(ByteInputStream(byteContent, byteContent.size), handler, metadata)

  return handler.toString().replace(Regex("-\n"), "")
}
