package chatroom.grpc

import chatroom.AuthService.{AuthenticationServiceGrpc, AuthorizationRequest}
import chatroom.ChatService.{ChatRoomServiceGrpc, Empty, Room}
import chatroom.repository.ChatRoomRepository
import io.grpc.{Status, StatusRuntimeException}
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ChatRoomServiceImpl(val repository: ChatRoomRepository,
                          authService: AuthenticationServiceGrpc.AuthenticationServiceStub)
  extends ChatRoomServiceGrpc.ChatRoomService {

  private val logger = LoggerFactory.getLogger(classOf[ChatRoomServiceImpl].getName)

  protected def processIfAdmin(room: Room, process: Room => Room) = {
    // TODO Retrieve JWT from Constant.JWT_CTX_KEY
    // TODO Retrieve the roles
    // TODO If not in the admin role, return Status.PERMISSION_DENIED
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
