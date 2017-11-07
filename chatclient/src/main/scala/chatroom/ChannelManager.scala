package chatroom

import java.io.IOException

import chatroom.AuthService.AuthenticationServiceGrpc.AuthenticationServiceBlockingStub
import chatroom.AuthService.{AuthenticationRequest, AuthenticationServiceGrpc, AuthorizationRequest}
import chatroom.ChatService._
import chatroom.grpc.{Constant, JwtCallCredential}
import com.typesafe.scalalogging.LazyLogging
import io.grpc._
import io.grpc.stub.{MetadataUtils, StreamObserver}

import scala.util.Try

case class ChannelManager(authChannel: ManagedChannel, authService: AuthenticationServiceBlockingStub) extends LazyLogging {

  // Channels
  private var optChatChannel: Option[ManagedChannel] = Option.empty[ManagedChannel]
  private var optChatRoomService: Option[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub] = Option.empty[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub]
  private var optToServer: Option[StreamObserver[ChatMessage]] = Option.empty[StreamObserver[ChatMessage]]

  def initChatChannel(token:String, clientOutput: String => Unit): Unit = {
    logger.info("initializing chat services with token: " + token)

    val metadata = new Metadata()
    metadata.put(Constant.JWT_METADATA_KEY, token)

    val chatChannel = ManagedChannelBuilder
      .forTarget("localhost:9092")
      .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
      .usePlaintext(true)
      .build

    optChatChannel = Some(chatChannel)

    val jwtCallCredentials = new JwtCallCredential(token)
    initChatServices(jwtCallCredentials)
    initChatStream(jwtCallCredentials, clientOutput)
  }

  /**
    * Initialize Chat Services
    *
    */
  def initChatServices(jwtCallCredentials: JwtCallCredential): Unit = {
    optChatChannel.foreach { chatChannel =>
      val chatRoomService = ChatRoomServiceGrpc.blockingStub(chatChannel).withCallCredentials(jwtCallCredentials)
      optChatRoomService = Some(chatRoomService)
    }
  }

  /**
    * Initalize Chat Stream
    */
  def initChatStream(jwtCallCredentials: JwtCallCredential, clientOutput: String => Unit): Unit = {

    // TODO Implement new StreamObserver[ChatMessageFromServer]
    // TODO and assign the server responseObserver toServer variable
  }

  def shutdown(): Unit = {
    logger.info("Closing Chat Channels")
    optChatChannel.map(chatChannel => chatChannel.shutdown())
    authChannel.shutdown()
  }

  /**
    * Authenticate the username/password with AuthenticationService
    *
    * @param username
    * @param password
    * @return If authenticated, return the authentication token, else, return null
    */
  def authenticate(username: String, password: String, clientOutput: String => Unit): Option[String] = {
    logger.info("authenticating user: " + username)
    (for {
      authenticationResponse <- Try(authService.authenticate(new AuthenticationRequest(username, password)))
      token = authenticationResponse.token
      authorizationResponse <- Try(authService.authorization(new AuthorizationRequest(token)))
    } yield {
      logger.info("user has these roles: " + authorizationResponse.roles)
      token
    }).fold({
      case e: StatusRuntimeException =>
        if (e.getStatus.getCode == Status.Code.UNAUTHENTICATED) {
          logger.error("user not authenticated: " + username, e)
        } else {
          logger.error("caught a gRPC exception", e)
        }
        None
    }, Some(_))
  }

  /**
    * List all the chat rooms from the server
    */
  def listRooms(clientOutput: String => Unit): Unit = {
    logger.info("listing rooms")
    optChatRoomService.foreach {
      chatRoomService =>
        val rooms = chatRoomService.getRooms(Empty.defaultInstance)
        rooms.foreach {
          room =>
            try
              clientOutput("Room: " + room.name)
            catch {
              case e: IOException =>
                e.printStackTrace()
            }
        }
    }
  }

  /**
    * Leave the room
    */
  def leaveRoom(room: String): Unit = {
    logger.info("leaving room: " + room)
    optToServer.foreach {
      chatStreamObserver =>
        val message = ChatMessage(`type` = MessageType.LEAVE, roomName = room)
        chatStreamObserver.onNext(message)
        logger.info("left room: " + room);
    }
  }

  /**
    * Join a Room
    *
    * @param room
    */
  def joinRoom(room: String): Unit = {
    logger.info("joinining room: " + room)
    optToServer.foreach {
      chatStreamObserver =>
        val message = ChatMessage(`type` = MessageType.JOIN, roomName = room)
        chatStreamObserver.onNext(message)
        logger.info("joined room: " + room)
    }
  }

  /**
    * Create Room
    *
    * @param room
    */
  def createRoom(room: String): Unit = {
    logger.info(s"create room: $room  optChatRoomService:$optChatRoomService")

    optChatRoomService.foreach {
      chatRoomService =>
        try {
          logger.info(s"chatRoomService: $chatRoomService")
          chatRoomService.createRoom(Room(name = room))
          logger.info("created room: " + room)
        }
        catch {
          case exc: Throwable => logger.error("Error creating room", exc)
        }
    }
  }

  /**
    * Send a message
    *
    * @param room
    * @param message
    */
  def sendMessage(room: String, message: String): Unit = {
    logger.info("sending chat message")
    // TODO call toServer.onNext(...)
  }
}

object ChannelManager extends LazyLogging  {

  /**
    * Initialize a managed channel to connect to the auth service.
    * Set the authChannel and authService
    */
  def apply(): ChannelManager = {
    logger.info("initializing auth service")
    val authChannel: ManagedChannel = ManagedChannelBuilder.forTarget("localhost:9091").usePlaintext(true).build
    val authService: AuthenticationServiceBlockingStub = AuthenticationServiceGrpc.blockingStub(authChannel)
    ChannelManager(authChannel, authService)
  }
}
