package akka.mesos.serde

import akka.libprocess.serde.{ MessageSerDe, TransportMessage }
import akka.mesos.protos.{ DeclineResourceOfferMessage, ProtoWrapper }
import akka.mesos.protos.internal.{ RegisterFrameworkMessage, RescindResourceOfferMessage, ResourceOffersMessage, FrameworkRegisteredMessage }
import com.google.protobuf.MessageLite
import com.typesafe.config.Config
import mesos.internal.Messages

import scala.util.{ Failure, Success, Try }

class ProtobufSerDe(config: Config) extends MessageSerDe {

  def deserialize(message: TransportMessage): Try[AnyRef] = {
    constructorMapping.get(message.messageName) map { ctor =>
      try {
        val msg = ctor(message.data)
        Success(msg)
      } catch {
        case e: Exception =>
          Failure(e)
      }
    } getOrElse {
      Failure(new NoSuchElementException)
    }
  }

  def serialize(obj: AnyRef): Try[TransportMessage] = obj match {
    case msg: ProtoWrapper[_] =>
      typeMapping.get(msg.getClass) map { name =>
        Success(TransportMessage(name, msg.toProto.toByteArray))
      } getOrElse {
        Failure(new NoSuchElementException)
      }

    case _ =>
      Failure(new ClassCastException)
  }

  val constructorMapping: Map[String, Array[Byte] => ProtoWrapper[_ <: MessageLite]] = Map(
    "mesos.internal.FrameworkRegisteredMessage" -> FrameworkRegisteredMessage.fromBytes,
    "mesos.internal.ResourceOffersMessage" -> ResourceOffersMessage.fromBytes
  )

  val typeMapping: Map[Class[_], String] = Map(
    classOf[RegisterFrameworkMessage] -> "mesos.internal.RegisterFrameworkMessage",
    classOf[RescindResourceOfferMessage] -> "mesos.internal.RescindResourceOfferMessage",
    classOf[DeclineResourceOfferMessage] -> "mesos.internal.LaunchTasksMessage"
  )
}
