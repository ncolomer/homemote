package io.homemote

import java.net.InetAddress

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import io.homemote.actor.{HttpApi, Network, Router}
import io.homemote.extension.{ExtensionCompanion, PingNode, ShutterNode, WaterNode}
import io.homemote.repository.{GroupRepository, MeasureRepository, NodeRepository, StateRepository}
import io.homemote.serial.GatewayDriver
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import scaldi.akka.AkkaInjectable

class AppModule extends scaldi.Module {

  bind[Config] to ConfigFactory.load

  bind [TransportClient] to {
    val config = inject[Config]
    val address = new InetSocketTransportAddress(InetAddress.getByName(
      config.getString("elasticsearch.host")),
      config.getInt("elasticsearch.port"))
    val settings = Settings.builder.put("cluster.name", "homemote").build
    new PreBuiltTransportClient(settings).addTransportAddress(address)
  }

  binding to injected [NodeRepository]
  binding to injected [GroupRepository]
  binding to injected [MeasureRepository]
  binding to injected [StateRepository]

  binding to new ExtensionCompanion()
  binding to injected [PingNode]
  binding to injected [WaterNode]
  binding to injected [ShutterNode]

  bind [ActorSystem] to ActorSystem("homemote") destroyWith (_.terminate)

  binding toProvider new Router
  binding toProvider new HttpApi
  binding toProvider new GatewayDriver
  binding toProvider new Network

  binding identifiedBy 'router toNonLazy {
    implicit val system = inject [ActorSystem]
    AkkaInjectable.injectActorRef [Router]
  }

}
