package chatroom

import java.util.concurrent.Executors

import chatroom.repository.UserRepository
import com.auth0.jwt.algorithms.Algorithm
import io.grpc.{Context, ServerBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object AuthServer {
  private val logger = LoggerFactory.getLogger("AuthServer")

  def main(args: Array[String]): Unit = {
    val repository = new UserRepository
    // TODO Use ServerBuilder to create a new Server instance. Start it, and await termination.
  }

}
