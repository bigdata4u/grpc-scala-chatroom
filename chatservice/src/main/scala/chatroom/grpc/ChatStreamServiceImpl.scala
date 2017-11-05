package chatroom.grpc

import java.util.Date

import org.slf4j.LoggerFactory
import chatroom.ChatService.{ChatMessage, ChatMessageFromServer, ChatStreamServiceGrpc, MessageType}
import chatroom.repository.ChatRoomRepository
import com.google.protobuf.timestamp.Timestamp
import io.grpc.{Status, StatusRuntimeException}
import io.grpc.stub.StreamObserver

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

class ChatStreamServiceImpl(val repository: ChatRoomRepository) extends ChatStreamServiceGrpc.ChatStreamService {

  private val logger = LoggerFactory.getLogger(classOf[ChatStreamServiceImpl].getName)

  val roomObservers = TrieMap[String, mutable.Set[StreamObserver[ChatMessageFromServer]]]()

  protected def failedBecauseRoomNotFound[T](roomName: String, responseObserver: StreamObserver[T]): Boolean = {
    val room = repository.findRoom(roomName)
    if (room == null) {
      responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription("Room not found: " + roomName)))
      true
    }
    false
  }

  def getRoomObservers(room: String): Option[mutable.Set[StreamObserver[ChatMessageFromServer]]] = {
    roomObservers.putIfAbsent(room, mutable.Set.empty)
    roomObservers.get(room)
  }

  //TODO - implement this right
  def removeObserverFromAllRooms(responseObserver: StreamObserver[ChatMessageFromServer]): Unit = {
    val setsOfObservers = roomObservers.values.map { setStreamObservers =>
      setStreamObservers - (responseObserver)
    }
  }

  override def chat(responseObserver: StreamObserver[ChatMessageFromServer]): StreamObserver[ChatMessage] = {
    val username = Constant.USER_ID_CTX_KEY.get()
    logger.info(s"processing chat from $username")

    new StreamObserver[ChatMessage] {
      //handle a new chat message and either join / leave a room or broadcast message
      override def onNext(chatMessage: ChatMessage): Unit = {
        val optObservers = getRoomObservers(chatMessage.roomName)
        logger.info(s"optObservers $optObservers")

        optObservers.foreach { observers =>
          chatMessage.`type` match {
            //add a new response observer to the current set of observers
            case MessageType.JOIN => {
              logger.info("adding observer to room")
              observers.add(responseObserver)
            }
            //remove an observer from the current set of observers
            case MessageType.LEAVE =>
              observers - (responseObserver)
            //add a new response observer to the current set of observers
            case MessageType.TEXT =>
              if (observers.contains(responseObserver)) {
                val now = Timestamp((new Date()).getTime)
                val messageFromServer = ChatMessageFromServer(Some(now),chatMessage.`type`,chatMessage.roomName, username, chatMessage.message)
                observers.foreach(nxtObserver => nxtObserver.onNext(messageFromServer))
              }
              else {
                logger.info("returning error because user is not in room")
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription(s"You are not in the room ${chatMessage.roomName}").asRuntimeException)
              }
            case _ => logger.error("Unknown chat message type")
          }
        }
      }

      override def onError(t: Throwable): Unit = {
        logger.error("gRPC error", t)
        removeObserverFromAllRooms(responseObserver)
      }

      override def onCompleted(): Unit = {
        removeObserverFromAllRooms(responseObserver)
      }

    }
  }
}
