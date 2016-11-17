package io.homemote.model

import io.homemote.serial.Protocol.IMessage

case class NodeMessage(node: Node, msg: IMessage)
