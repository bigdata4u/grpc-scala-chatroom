package chatroom.grpc

import chatroom.AuthService.{AuthenticationServiceGrpc, AuthorizationRequest}
import chatroom.ChatService.{ChatRoomServiceGrpc, Empty, Room}
import chatroom.repository.ChatRoomRepository
import com.typesafe.scalalogging.LazyLogging
import io.grpc.Status
import io.grpc.stub.StreamObserver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ChatRoomServiceImpl(val repository: ChatRoomRepository,
                          authService: AuthenticationServiceGrpc.AuthenticationServiceStub)
  extends ChatRoomServiceGrpc.ChatRoomService with LazyLogging {

  protected def processIfAdmin(room: Room, process: Room => Room) = {
    val jwt = Constant.JWT_CTX_KEY.get
    val authorizationFuture = authService.authorization(AuthorizationRequest(jwt.getToken))
    authorizationFuture.flatMap { authz =>
      val tryAdmin = if (authz.roles.contains("admin")) {
        process(room)
        Success(room)
      }
      else {
        logger.error(s"permission denied processing request")
        Failure(Status.PERMISSION_DENIED.asRuntimeException)
      }
      Future.fromTry(tryAdmin)
    }
  }


  override def createRoom(requestedRoom: Room): Future[chatroom.ChatService.Room] = {
    processIfAdmin(requestedRoom, nxtRoom => {
      logger.info(s"creating room $requestedRoom")
      repository.save(nxtRoom)
      nxtRoom
    })
  }

  override def deleteRoom(requestedRoom: Room): Future[chatroom.ChatService.Room] = {
    processIfAdmin(requestedRoom, nxtRoom => {
      logger.info(s"deleting room $requestedRoom")
      repository.delete(nxtRoom)
      nxtRoom
    })
  }

  override def getRooms(request: Empty, responseObserver: StreamObserver[Room]): Unit = {
    repository.getRooms.foreach(responseObserver.onNext)
    responseObserver.onCompleted()
  }
}
