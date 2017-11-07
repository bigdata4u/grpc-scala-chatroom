package chatroom

import java.io.IOException

import com.typesafe.scalalogging.LazyLogging
import jline.console.ConsoleReader

object ChatClient extends LazyLogging {

  private val console = new ConsoleReader

  private val channelManager = new ChannelManager

  private var state = CurrentState()

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    channelManager.initAuthService()
    prompt()
  }

  def outputToConsole(output:String): Unit = {
    ConsoleUtil.printLine(console, output)
  }

  @throws[Exception]
  protected def prompt(): Unit = {

    while (true) {
      try {
        println(s"STATE: $state")
        state.status match {
          case STARTED =>
            readLogin()
            println(s"readLogin new state: $state")
          case AUTHENTICATED =>
            readCommand()
          case IN_ROOM =>
            readCommand()
        }
      }
      catch {
        case e: Exception =>
          e.printStackTrace()
          shutdown()
      }
    }
  }

  protected def shutdown(): Unit = {
    logger.info("Exiting chat client")
    channelManager.shutdown()
    System.exit(1)
  }

  @throws[Exception]
  protected def readLogin(): Unit = {
    val prompt = "/login [username] | /quit\n-> "
    val line = console.readLine(prompt)
    val splitLine = line.split(" ")
    val command = splitLine(0)
    if (splitLine.length >= 2) {
      val username = splitLine(1)
      if (command.equalsIgnoreCase("/create")) {
        logger.info("login to create a room")
        //createUser(username, consoleReader, authService);
      }
      else if (command.equalsIgnoreCase("/login")) {
        logger.info("processing login user")
        val password = console.readLine("password> ", '*')
        val optToken = channelManager.authenticate(username, password, outputToConsole)
        optToken.foreach {
          token =>
            this.state = CurrentState(AUTHENTICATED, username, token, null)
            channelManager.initChatChannel(token, outputToConsole)
        }
      }
    }
    else if (command.equalsIgnoreCase("/quit")) shutdown()
    else {
      logger.info(s"command $command is not implemented")
    }
  }

  @throws[IOException]
  protected def readCommand(): Unit = {
    val help = "[chat message] | /join [room] | /leave [room] | /create [room] | /list | /quit"
    logger.info(help)
    val prompt = this.state.username + "-> "
    val line = console.readLine(prompt)

    if (line.startsWith("/")) {
      if ("/quit".equalsIgnoreCase(line)) {
        shutdown()
      } else if ("/list".equalsIgnoreCase(line)) {
        channelManager.listRooms(outputToConsole)
      } else if ("/leave".equalsIgnoreCase(line)) {

        state.status match {
          case IN_ROOM =>
            channelManager.leaveRoom(state.room)
            state = CurrentState(AUTHENTICATED, state.username, state.token, null)
          case _ => logger.error("error - not in a room")
        }
      } else if ("/?".equalsIgnoreCase(line)) {
        console.println(help)
      } else {
        //handle a command that takes a room name, like join or create
        val splitCommands = line.split(" ")
        if (splitCommands.length == 2) {
          val command = splitCommands(0)
          val room = splitCommands(1)
          if ("/join".equalsIgnoreCase(command)) {
            if (this.state.status == IN_ROOM) {
              logger.info("already in room [" + room + "], leaving...")
              channelManager.leaveRoom(room)
              this.state = CurrentState(AUTHENTICATED, state.username, state.token, null)
            }
            channelManager.joinRoom(room)
            this.state = CurrentState(IN_ROOM, state.username, state.token, room)
          } else if ("/create".equalsIgnoreCase(command)) {
            channelManager.createRoom(room)
          }
        }
      }
    } else if (!line.isEmpty()) {
      // if the line was not a chat command then send it as a message to the other rooms
      if (this.state.status != IN_ROOM) {
        logger.error("error - not in a room")
      } else {
        channelManager.sendMessage(state.room, line)
      }
    }
  }


}


sealed trait ClientStatus

case object STARTED extends ClientStatus

case object AUTHENTICATED extends ClientStatus

case object IN_ROOM extends ClientStatus

case class CurrentState(status: ClientStatus = STARTED, username: String = "", token: String = "", room: String = "")
