package chatroom

import chatroom.repository.UserRepository
import com.typesafe.scalalogging.LazyLogging

object AuthServer extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val repository = new UserRepository
    // TODO Use ServerBuilder to create a new Server instance. Start it, and await termination.
  }

}
