package io.homemote.actor

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, Props, Terminated}
import io.homemote.model.NodeMessage
import io.homemote.repository._
import io.homemote.serial.GatewayDriver
import io.homemote.serial.GatewayDriver.Connect
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

class Root extends Actor with ActorLogging {
  import context._

  // Boot repositories
  val address = new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300)
  val client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(address)
  trait ESClient { val es = client }
  val Node: NodeRepository = new NodeRepository with ESClient
  val Group: GroupRepository = new GroupRepository with ESClient
  val Measure: MeasureRepository = new MeasureRepository with ESClient
  val State: StateRepository = new StateRepository with ESClient
  Seq(Node, Group, Measure, State).foreach(_.init())

  // Boot listeners
  val httpApi = actorOf(Props(classOf[HttpApi], Node, Group))

  val gateway = watch(actorOf(Props[GatewayDriver]))
  val network = actorOf(Props(classOf[Network], Node))

  val waterNode = actorOf(Props[WaterNode])
  val shutterNode = actorOf(Props[ShutterNode])

  override def preStart() = gateway ! Connect

  override def receive: Receive = {
    case Terminated(ref) if ref eq gateway => //sys.exit(1)
    case msg: GatewayDriver.MessageReceived => network.forward(msg)
    case msg: GatewayDriver.EmitMessage => gateway.forward(msg)
    case msg: NodeMessage => system.eventStream.publish(msg)
  }

}
