package chatroom.grpc

import java.util.concurrent.Executor

import io.grpc.Attributes.Key
import io.grpc._


class JwtCallCredential(val jwt:String) extends CallCredentials {


  override def applyRequestMetadata(methodDescriptor: MethodDescriptor[_, _], attributes: Attributes,
                                    executor: Executor, metadataApplier: CallCredentials.MetadataApplier): Unit = {
    val authority = attributes.get(CallCredentials.ATTR_AUTHORITY)
    executor.execute(new Runnable() {
      override def run(): Unit = {
        try {
          val headers = new Metadata
          val jwtKey = Metadata.Key.of("jwt", Metadata.ASCII_STRING_MARSHALLER)
          headers.put(jwtKey, jwt)
          metadataApplier.apply(headers)
        } catch {
          case e: Throwable =>
            metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
        }
      }
    })
  }

  override def thisUsesUnstableApi(): Unit = ???
}
