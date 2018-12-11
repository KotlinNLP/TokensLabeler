/* Copyright 2018-present The KotlinNLP Authors. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ------------------------------------------------------------------*/

package com.kotlinnlp.tokenslabeler.helpers

import com.kotlinnlp.tokenslabeler.language.BIEOUTag

/**
 * The class that contains convenient methods to work with different annotation schemes.
 */
sealed class SchemeConverter {

  /**
   * @param tag the current tag
   * @param prevTag the previous tag
   * @param nextTag the next tag
   *
   * @return the current tag converted in the desired format
   */
  abstract fun convertTag(tag: String, prevTag: String?, nextTag: String?): BIEOUTag

  /**
   * The BIO to BIEOU converter.
   */
  object BioToBieou: SchemeConverter() {

    /**
     * @param tag the current tag
     * @param prevTag the previous tag
     * @param nextTag the next tag
     *
     * @return the current tag converted in the BILOU format
     */
    override fun convertTag(tag: String, prevTag: String?, nextTag: String?): BIEOUTag {
      return if (nextTag != null)
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("B-") && !nextTag.startsWith("I-") -> BIEOUTag.Unit
          tag.startsWith("B-") && nextTag.startsWith("I-") -> BIEOUTag.Beginning
          tag.startsWith("I-") && nextTag.startsWith("I-") -> BIEOUTag.Inside
          tag.startsWith("I-") && !nextTag.startsWith("I-") -> BIEOUTag.End
          else -> throw IllegalArgumentException("Unexpected tag")
        }
      else
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("I-") -> BIEOUTag.End
          tag.startsWith("B-") -> BIEOUTag.Unit
          else -> throw IllegalArgumentException("Unexpected tag")
        }
    }
  }

  /**
   * The BIO to IOE2 converter.
   */
  object BioToIoe2: SchemeConverter() {

    /**
     * @param tag the current tag
     * @param prevTag the previous tag
     * @param nextTag the next tag
     *
     * @return the current tag converted in the IOE2 format
     */
    override fun convertTag(tag: String, prevTag: String?, nextTag: String?): BIEOUTag {
      return if (nextTag != null)
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("B-") -> BIEOUTag.Inside
          tag.startsWith("I-") && !nextTag.startsWith("I-") -> BIEOUTag.End
          tag.startsWith("I-") && nextTag.startsWith("I-") -> BIEOUTag.Inside
          else -> throw IllegalArgumentException("Unexpected tag")
        }
      else
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("I-") -> BIEOUTag.End
          tag.startsWith("B-") -> BIEOUTag.Inside
          else -> throw IllegalArgumentException("Unexpected tag")
        }
    }
  }

  /**
   * The BIO to IOE1 converter.
   */
  object BioToIoe1: SchemeConverter() {

    /**
     * @param tag the current tag
     * @param prevTag the previous tag
     * @param nextTag the next tag
     *
     * @return the current tag converted in the IOE1 format
     */
    override fun convertTag(tag: String, prevTag: String?, nextTag: String?): BIEOUTag {
      return if (nextTag != null)
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("B-") -> BIEOUTag.Inside
          tag.startsWith("I-") && (nextTag.startsWith("I-") || nextTag.startsWith("O"))-> BIEOUTag.Inside
          tag.startsWith("I-") && nextTag.startsWith("B-") -> BIEOUTag.End
          else -> throw IllegalArgumentException("Unexpected tag")
        }
      else
        when {
          tag.startsWith("O") -> BIEOUTag.Outside
          tag.startsWith("I-") -> BIEOUTag.Inside
          tag.startsWith("B-") -> BIEOUTag.Inside
          else -> throw IllegalArgumentException("Unexpected tag")
        }
    }
  }

  /**
   * The BIO converter.
   */
  object Bio: SchemeConverter() {

    /**
     * @param tag the current tag
     * @param prevTag the previous tag
     * @param nextTag the next tag
     *
     * @return the current tag converted in the BIO format
     */
    override fun convertTag(tag: String, prevTag: String?, nextTag: String?): BIEOUTag {
      return when {
        tag.startsWith("O") -> BIEOUTag.Outside
        tag.startsWith("B-") -> BIEOUTag.Beginning
        tag.startsWith("I-") -> BIEOUTag.Inside
        else -> throw IllegalArgumentException("Unexpected tag")
      }
    }
  }
}