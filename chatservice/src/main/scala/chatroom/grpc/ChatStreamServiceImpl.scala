package chatroom.grpc

import chatroom.ChatService.{ChatMessage, ChatMessageFromServer, ChatStreamServiceGrpc}
import chatroom.repository.ChatRoomRepository
import com.typesafe.scalalogging.LazyLogging
import io.grpc.stub.StreamObserver
import io.grpc.{Status, StatusRuntimeException}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

class ChatStreamServiceImpl(val repository: ChatRoomRepository) extends ChatStreamServiceGrpc.ChatStreamService with LazyLogging {

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
      }

      override def onError(t: Throwable): Unit = {
      }

      override def onCompleted(): Unit = {
      }
    }
  }
}
