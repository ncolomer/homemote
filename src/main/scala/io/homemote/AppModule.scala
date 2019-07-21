package io.homemote

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import io.homemote.actor.{HttpApi, Network, Router}
import io.homemote.extension.{ExtensionCompanion, PingNode, ShutterNode, WaterNode}
import io.homemote.repository._
import io.homemote.repository.postgres._
import io.homemote.serial.GatewayDriver
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}
import scaldi.akka.AkkaInjectable

class AppModule extends scaldi.Module {

  bind[Config] to ConfigFactory.load

  bind[Database] to {
    val config = inject [Config]
    val db = Databases(
      name = "homemote",
      driver = "org.postgresql.Driver",
      url = config.getString("db.url"),
      config = Map(
        "username" -> config.getString("db.username"),
        "password" -> config.getString("db.password")
      ))
    Evolutions.applyEvolutions(db)
    db
  } destroyWith (_.shutdown)

  bind [NodeRepository] to injected [PGNodeRepository]
  bind [GroupRepository] to injected [PGGroupRepository]
  bind [MeasureRepository] to injected [PGMeasureRepository]
  bind [StateRepository] to injected [PGStateRepository]

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
