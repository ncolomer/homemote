package io.homemote.model

import org.joda.time.DateTime

/** Example:
  * {{{
  * {
  *   "origin": "d7:65:7c:68:e3:3c:1d:24"
  *   "timestamp": "2016-11-04T15:22:36.123Z"
  *   "name": "temperature"
  *   "value": 22.4
  * }
  * }}}
  */
case class Measure(origin: UniqueID,
                   timestamp: DateTime,
                   name: String,
                   value: Double)