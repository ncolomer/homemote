package io.homemote.model

/** Example:
  * {{{
  * {
  *   "name": "living room",
  *   "tags": ["shutter", "first floor", "living room"]
  *   "groups": []
  * }
  * }}}
  */
case class Group(name: String,
                 tags: Set[String] = Set.empty,
                 groups: Set[String] = Set.empty)