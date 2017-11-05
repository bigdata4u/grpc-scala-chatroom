package chatroom.repository

import chatroom.ChatService.Room

import scala.collection.parallel.immutable.ParHashMap

class ChatRoomRepository {

  var rooms: ParHashMap[String, Room] = ParHashMap[String, Room]()

  //create a handful of initial rooms for testing
  Seq("grpc","beta","random").map{name => save(Room(name))}

  def findRoom(name:String): Option[Room] = rooms.get(name)

  def save(room:Room): Room = {
    rooms += (room.name -> room)
    room
  }

  def delete(room:Room):Room = {
    rooms -= room.name
    room
  }

  def getRooms:List[Room] = rooms.values.toList

}
