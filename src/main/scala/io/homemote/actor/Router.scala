package io.homemote.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.http.scaladsl.server.Directives._
import io.homemote.actor.Network.NodeMessage
import io.homemote.api.{NodeApi, ServiceApi}
import io.homemote.extension._
import io.homemote.repository._
import io.homemote.serial.GatewayDriver
import io.homemote.serial.GatewayDriver.Connect
import scaldi.Injector
import scaldi.akka.AkkaInjectable

class Router(implicit inj: Injector) extends Actor with ActorLogging with AkkaInjectable {

  import context.watch

  // Boot Actors
  val httpApi: ActorRef = injectActorRef[HttpApi]
  val gateway: ActorRef = watch(injectActorRef[GatewayDriver])
  val network: ActorRef = injectActorRef[Network]

  // Load extensions
  val Extensions: Map[String, Extension] =
    injectAllOfType[Extension].map(extension => {
      log.debug(s"Loaded extension for firmware ${extension.firmware}")
      extension.firmware -> extension
    }).toMap

  // Build REST API
  httpApi ! HttpApi.InstallRoute(ServiceApi.route)
  httpApi ! HttpApi.InstallRoute(NodeApi.route(inject[NodeRepository]))
  httpApi ! HttpApi.InstallRoute(
    pathPrefix("api" / "v1") {
      (pathEnd & get & complete("Homemote API v1")) ~ Extensions.values
        .map(extension => pathPrefix(extension.firmware)(extension.handleHTTP))
        .reduce(_ ~ _)
    })

  override def preStart(): Unit = gateway ! Connect

  override def receive: Receive = {
    case Terminated(ref) if ref eq gateway => //sys.exit(1)
    case msg: GatewayDriver.MessageReceived => network.forward(msg)
    case msg: GatewayDriver.EmitMessage => gateway.forward(msg)
    case msg: NodeMessage => Extensions.get(msg.node.firmware.name) match {
      case Some(ext) => ext.handleRF.apply(msg.node, msg.msg)
      case None => log.error("Extension for firmware {} not found!", msg.node.firmware.name)
    }
  }

}
