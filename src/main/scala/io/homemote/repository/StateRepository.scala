package io.homemote.repository

import io.homemote.model.JsonSerde
import org.elasticsearch.client.transport.TransportClient

class StateRepository(val es: TransportClient) extends ESRepository with JsonSerde {

  init("state", "state")

}
