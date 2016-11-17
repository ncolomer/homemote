package io.homemote.actor

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, Props, Terminated}
import io.homemote.model.NodeMessage
import io.homemote.repository.{ElasticsearchClient, NodeRepository}
import io.homemote.serial.GatewayDriver
import io.homemote.serial.GatewayDriver.Connect
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

class Root extends Actor with ActorLogging {
  import context._

  val address = new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300)
  val client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(address)
  val NodeRepository = new NodeRepository with ElasticsearchClient { val es = client }
  NodeRepository.init()

  val httpApi = actorOf(Props(classOf[HttpApi], NodeRepository))

  val gateway = watch(actorOf(Props[GatewayDriver]))
  val network = actorOf(Props(classOf[Network], NodeRepository))

  val shutterNode = actorOf(Props[ShutterNode])
  val waterNode = actorOf(Props[WaterNode])

  override def preStart() = {
    gateway ! Connect
  }

  override def receive: Receive = {
    case Terminated(ref) if ref eq gateway => //sys.exit(1)
    case msg: GatewayDriver.MessageReceived => network.forward(msg)
    case msg: GatewayDriver.EmitMessage => gateway.forward(msg)
    case msg: NodeMessage => system.eventStream.publish(msg)
  }

}
