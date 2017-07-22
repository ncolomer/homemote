package io.homemote.actor

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.http.scaladsl.server.Directives._
import com.typesafe.config.Config
import io.homemote.actor.Network.NodeMessage
import io.homemote.api.NodeApi
import io.homemote.extension._
import io.homemote.model.Node
import io.homemote.repository._
import io.homemote.serial.GatewayDriver
import io.homemote.serial.GatewayDriver.Connect
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.concurrent.Future

class Root extends Actor with ActorLogging {

  import context._
  val config: Config = system.settings.config

  // Boot Elasticsearch client
  val esClient: TransportClient = {
    val address = new InetSocketTransportAddress(InetAddress.getByName(
      config.getString("elasticsearch.host")),
      config.getInt("elasticsearch.port"))
    val settings = Settings.builder.put("cluster.name", "homemote").build
    new PreBuiltTransportClient(settings).addTransportAddress(address)
  }
  trait ESClient { val es: TransportClient = esClient }

  // Boot Repositories
  val Nodes: NodeRepository = new NodeRepository with ESClient
  val Groups: GroupRepository = new GroupRepository with ESClient
  val Measures: MeasureRepository = new MeasureRepository with ESClient
  val States: StateRepository = new StateRepository with ESClient
  Seq(Nodes, Groups, Measures, States).foreach(_.init())

  // Boot Actors
  val httpApi: ActorRef = actorOf(Props(classOf[RestApi]))
  val gateway: ActorRef = watch(actorOf(Props[GatewayDriver]))
  val network: ActorRef = actorOf(Props(classOf[Network], Nodes))

  // Boot Extensions
  trait Dependencies extends Companion with ContactPoint { self: Extension =>
    override def getNode(id: Node.Id): Future[Node] = Nodes.get(id)
      .filter(node => node.firmware.name == this.firmware)
    val actor: ActorRef = Root.this.self
  }

  val Extensions: Map[String, Extension] = List[Extension](
    new PingNode with Dependencies,
    new WaterNode with Dependencies,
    new ShutterNode with Dependencies
  ).map(extension => {
    val firmware = extension.firmware
    log.debug(s"Loaded extension for firmware $firmware")
    firmware -> extension
  }).toMap

  // Update API
  httpApi ! RestApi.PushRoute(NodeApi.route(Nodes))
  httpApi ! RestApi.PushRoute(Extensions.map({ case (name, ext) => pathPrefix(name)(ext.handleHTTP) }).reduce(_ ~ _))

  override def preStart(): Unit = gateway ! Connect

  override def receive: Receive = {
    case Terminated(ref) if ref eq gateway => //sys.exit(1)
    case msg: GatewayDriver.MessageReceived => network.forward(msg)
    case msg: GatewayDriver.EmitMessage => gateway.forward(msg)
    case msg: NodeMessage => Extensions.lift(msg.node.firmware.name) match {
      case Some(ext) => ext.handleRF.apply(msg.node, msg.msg)
      case None => log.error("Extension for firmware {} not found!", msg.node.firmware.name)
    }
  }

}
