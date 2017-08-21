package io.homemote.model

import java.time.Instant

case class State(origin: UniqueID,
                 updated: Instant,
                 key: String,
                 value: String)
