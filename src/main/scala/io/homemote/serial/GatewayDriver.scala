package io.homemote.serial

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors, LinkedBlockingQueue, TimeUnit}

import akka.actor.{Actor, ActorLogging, PoisonPill}
import gnu.io.SerialPort._
import gnu.io.{CommPortIdentifier, SerialPort}
import io.homemote.serial.Protocol.{Ack, Config, IMessage, NoAck, OMessage, Packet}
import io.homemote.serial.Protocol._
import scodec.bits.ByteVector

import scala.collection.JavaConversions._
import scala.concurrent.Future._
import scala.concurrent._
import scala.concurrent.duration._

object GatewayDriver {
  
  case object Connect
  case class InitGateway(serial: Serial)
  case class InitDriver(serial: Serial)
  case class EmitMessage(msg: OMessage)
  case object Disconnect

  case class SerialPortConnected(name: String)
  case class GatewayFound(uniqueId: String, version: String)
  case class GatewayInitialized(config: Config)
  case object DriverReady

  object GatewayError { def apply(message: String): GatewayError = GatewayError(new Exception(message)) }
  case class GatewayError(t: Throwable)
  case class MessageEmitted(msg: OMessage)
  case class MessageReceived(msg: IMessage)
  case class MessageAck(uuid: UUID, ack: Ack)
  case class MessageNoAck(uuid: UUID)

  /** Wraps a [[gnu.io.SerialPort]] instance and related blocking stuff to allow mocking. */
  case class Serial(port: SerialPort) {
    private val is = port.getInputStream
    private val os = port.getOutputStream
    val sync: (String) => Unit = (pattern) => Iterator.continually(is.read).map(_.toByte)
      .sliding(pattern.length).map(_.toArray).map(new String(_, "US-ASCII"))
      .takeWhile(str => !pattern.equals(str)).foreach(_ => /* nop */ ())
    val read: () => Array[Byte] = () => Iterator.fill(is.read)(is.read.toByte).toArray
    val write: (Array[Byte]) => Unit = (bytes) => { os.write(bytes.length); os.write(bytes); os.flush() }
  }

}

/** This is the driver actor that communicates with gateway through a serial port.
  * All communication is done exclusively with parent.
  *
  * Here is how to interact with this actor:
  * - send either [[GatewayDriver.Connect]] or [[GatewayDriver.InitDriver]] to initialize state (the latter for testing purpose only)
  * - be notified when the driver is ready by listening for [[GatewayDriver.DriverReady]] message
  * - send [[GatewayDriver.EmitMessage]] message to send packet through RF
  * - be notified of incoming RF packet by listening for [[GatewayDriver.MessageReceived]] message
  * - eventually shutdown the actor by sending [[GatewayDriver.Disconnect]]
  */
class GatewayDriver extends Actor with ActorLogging {
  import GatewayDriver._

  implicit val ec = context.dispatcher

  val GatewaySettings = context.system.settings.config.getConfig("gateway")
  val Pattern = GatewaySettings.getString("driver.port-pattern").r
  val Owner = GatewaySettings.getString("driver.port-owner")
  val ConnectTimeout = GatewaySettings.getDuration("driver.connect-timeout", TimeUnit.MILLISECONDS)
  val BaudRate = GatewaySettings.getInt("driver.baud-rate")
  val AckTimeout = GatewaySettings.getDuration("driver.ack-timeout", TimeUnit.MILLISECONDS)

  val queue = new LinkedBlockingQueue[OMessage]()
  var port: Option[SerialPort] = None
  var pool: Option[ExecutorService] = None

  def notifyAndTerminate: PartialFunction[Throwable, Unit] = { case t => self ! GatewayError(t); self ! PoisonPill }
  def notifyAndDisconnect: PartialFunction[Throwable, Unit] = { case t => self ! GatewayError(t); self ! Disconnect }
  def notifyTimeoutAndDisconnect: PartialFunction[Throwable, Unit] = { case t => self ! GatewayError(s"Connection ${t.getMessage}"); self ! Disconnect }

  override def postStop() = {
    port.foreach(_.close)
    pool.foreach(_.shutdown)
  }

  override def receive: Receive = {
    // Commands
    case Connect => doConnect()
    case InitGateway(serial) => doInitGateway(serial)
    case InitDriver(serial) => doInitDriver(serial)
    case EmitMessage(msg) => queue.offer(msg)
    case Disconnect => log.info("Disconnecting..."); self ! PoisonPill
    // Lifecycle
    case SerialPortConnected(name) => log.debug("Connected on port {}", name)
    case GatewayFound(uid, version) => log.info("Gateway found with uid {} and version {}", uid, version)
    case GatewayInitialized(config) => log.debug("Gateway initialized with {}", config)
    case ref @ DriverReady => log.info("Gateway is now running"); context.parent.forward(ref)
    case GatewayError(t) => log.error(s"Driver error! ${t.getMessage}")
    // Runtime
    case MessageEmitted(msg) => log.debug("> {}", msg)
    case ref @ MessageReceived(msg) => log.debug("< {}", msg); context.parent.forward(ref)
    case MessageAck(uuid, ack) => log.debug("✔ {} was {}", uuid, ack)
    case MessageNoAck(uuid) => log.debug("✘ {} was NoAck", uuid)
  }

  def doConnect(): Future[Unit] = Future {
    // Look for eligible port
    val identifier = CommPortIdentifier.getPortIdentifiers.map(_.asInstanceOf[CommPortIdentifier])
      .filter(_.getPortType equals CommPortIdentifier.PORT_SERIAL) // pick serial ports only
      .filterNot(_.isCurrentlyOwned) // pick not used ports
      .map(id => id.getName -> id).collectFirst { case (Pattern(), id) => id } // match port name to user pattern
      .getOrElse(throw new IllegalArgumentException(s"No available serial port was found for pattern '$Pattern'"))
    // Open port
    val port = identifier.open(Owner, ConnectTimeout.toInt).asInstanceOf[SerialPort]
    port.setSerialPortParams(BaudRate, DATABITS_8, STOPBITS_1, PARITY_NONE)
    port.setFlowControlMode(FLOWCONTROL_NONE)
    this.port = Some(port)
    self ! SerialPortConnected(port.getName)
    self ! InitGateway(Serial(port))
  } recover notifyAndTerminate

  def doInitGateway(serial: Serial): Future[Unit] = firstCompletedOf(Seq(timeout(ConnectTimeout), Future {
    // Wait for gateway greetings
    serial.sync("gateway")
    val uid = ByteVector(serial.read()).toHex.grouped(2).mkString(":")
    val version = new String(serial.read(), "US-ASCII")
    self ! GatewayFound(uid, version)
    // Initialize gateway
    val cfg = Config(
      networkId = GatewaySettings.getInt("network-id"),
      gatewayId = GatewaySettings.getInt("node-id"),
      encryptKey = GatewaySettings.getString("encrypt-key"))
    serial.write(cfg.encode)
    serial.sync("ready")
    self ! GatewayInitialized(cfg)
    self ! InitDriver(serial)
  })) recover (notifyTimeoutAndDisconnect orElse notifyAndDisconnect)

  def doInitDriver(serial: Serial): Unit = {
    type Input = Array[Byte]
    type Output = OMessage
    pool = Some(Executors.newFixedThreadPool(2))
    implicit val ec = ExecutionContext.fromExecutor(pool.get)
    def inputFuture: Future[Array[Byte]] = Future(blocking(serial.read()))
    def outputFuture: Future[OMessage] = Future(blocking(queue.poll()))
    def processInput(arr: Input): Future[Unit] =
      Future(decode[IMessage](arr)) flatMap { msg =>
        if (!msg.requestAck) Future.successful(())
        else firstCompletedOf(Seq(msg.ackFuture, timeout(AckTimeout))) map {
          case Some(ack: Ack) =>
            serial.write(ack.encode)
            self ! MessageAck(msg.uuid, ack)
        } recover {
          case _ => // either None (voluntary NoAck) or Timeout (default timeout)
            serial.write(NoAck.encode)
            self ! MessageNoAck(msg.uuid)
        }
      }
    def processOutput(input: Future[Input], msg: Output): Future[Unit] =
      Future(msg.encode) flatMap { bytes =>
        serial.write(bytes)
        self ! MessageEmitted(msg)
        if (!msg.requestAck) Future.successful(())
        else firstCompletedOf(Seq(input, timeout(AckTimeout))) map {
          case arr: Input => decode[Packet](arr) match {
            case ack: Ack => msg.ack(ack); self ! MessageAck(msg.uuid, ack)
            case NoAck => msg.noAck(); self ! MessageNoAck(msg.uuid)
            case s => throw new IllegalStateException(s"Packet $s was not expected here!")
          }
        }
      }
    def listen(input: Future[Array[Byte]], output: Future[OMessage]): Future[Unit] =
      firstCompletedOf(Seq(input, output)) flatMap {
        case arr: Array[Byte] => processInput(arr).flatMap(_ => listen(inputFuture, output))
        case msg: OMessage => processOutput(input, msg).flatMap(_ => listen(inputFuture, outputFuture))
      } recover notifyAndDisconnect
    listen(inputFuture, outputFuture)
    self ! DriverReady
  }

  def timeout(atMost: Long): Future[Unit] = {
    val promise = Promise[Unit]()
    val exception = new Exception(s"timed out after ${atMost}ms")
    context.system.scheduler.scheduleOnce(atMost.millis)(promise.failure(exception))
    promise.future
  }

}