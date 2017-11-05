package chatroom.domain

import java.util.{Set => jSet}

import scala.collection.JavaConverters._

case class User(username:String, password:String, roles:Set[String]) {
  def rolesAsJavaSet: jSet[String] = roles.asJava

  override def toString: String = username + "," + password + "," + roles.mkString(",")
}

object User {

  //Java can't use the no arg copy, expose it directly
  def copyUser(user:User): User = {
    user.copy()
  }

  def userFromJava(username:String, password:String, roles: jSet[String]):User = {
    User(username, password, roles.asScala.toSet)
  }
}