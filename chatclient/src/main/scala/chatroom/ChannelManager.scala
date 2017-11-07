package chatroom

import java.io.IOException

import chatroom.AuthService.AuthenticationServiceGrpc
import chatroom.AuthService.AuthenticationServiceGrpc.AuthenticationServiceBlockingStub
import chatroom.ChatService._
import chatroom.grpc.JwtCallCredential
import io.grpc.{ManagedChannel, ManagedChannelBuilder, Status, StatusRuntimeException}
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import chatroom.grpc.JwtCallCredential

class ChannelManager {

  private val logger = LoggerFactory.getLogger(classOf[ChannelManager].getName)

  // Channels
  private var optAuthService: Option[AuthenticationServiceBlockingStub] = Option.empty[AuthenticationServiceBlockingStub]
  private var optAuthChannel: Option[ManagedChannel] = Option.empty[ManagedChannel]

  private var optChatChannel: Option[ManagedChannel] = Option.empty[ManagedChannel]
  private var optChatRoomService: Option[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub] = Option.empty[ChatRoomServiceGrpc.ChatRoomServiceBlockingStub]
  private var optToServer: Option[StreamObserver[ChatMessage]] = Option.empty[StreamObserver[ChatMessage]]

  /**
    * Initialize a managed channel to connect to the auth service.
    * Set the authChannel and authService
    */
  def initAuthService(): Unit = {
    logger.info("initializing auth service")
    // TODO Build a new ManagedChannel

    // TODO Get a new Blocking Stub
  }

  def initChatChannel(token:String, clientOutput: String => Unit): Unit = {
    logger.info("initializing chat services with token: " + token)
    val chatChannel = ManagedChannelBuilder.forTarget("localhost:9092").usePlaintext(true).build
    optChatChannel = Some(chatChannel)

    initChatServices()
    initChatStream(clientOutput)
  }

  /**
    * Initialize Chat Services
    *
    */
  def initChatServices(): Unit = {
    optChatChannel.foreach { chatChannel =>
      val chatRoomService = ChatRoomServiceGrpc.blockingStub(chatChannel)
      optChatRoomService = Some(chatRoomService)
    }
  }

  /**
    * Initalize Chat Stream
    */
  def initChatStream(clientOutput: String => Unit): Unit = {

    // TODO Implement new StreamObserver[ChatMessageFromServer]
    // TODO and assign the server responseObserver toServer variable
  }

  def shutdown(): Unit = {
    logger.info("Closing Chat Channels")
    optChatChannel.map(chatChannel => chatChannel.shutdown())
    optAuthChannel.map(authChannel => authChannel.shutdown())
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
    //  Call authService.authenticate(...) and retreive the token

    // TODO Call authService.authenticate(...) and retreive the token
    // TODO Retrieve all the roles with authService.authorization(...) and print out all the roles
    // TODO Return the token
    // TODO Catch StatusRuntimeException, because there could be Unauthenticated errors.
    // TODO If there are errors, return Option.empty[String]

    ???
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
