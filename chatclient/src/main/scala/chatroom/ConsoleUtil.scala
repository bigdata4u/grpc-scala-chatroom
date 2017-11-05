package chatroom

import java.io.IOException

import jline.console.{ConsoleReader, CursorBuffer}

/**
  * Utility methods for working with jLine.
  */
object ConsoleUtil {
  @throws[IOException]
  def printLine(console: ConsoleReader, message: String): Unit = {
    val stashed = stashLine(console)
    console.println(message)
    unstashLine(console, stashed)
    console.flush()
  }

  def stashLine(console: ConsoleReader): CursorBuffer = {
    val stashed = console.getCursorBuffer.copy
    try {
      console.getOutput.write("\u001b[1G\u001b[K")
      console.flush()
    } catch {
      case e: IOException =>
        e.printStackTrace()
    }
    stashed
  }


  def unstashLine(console: ConsoleReader, stashed: CursorBuffer): Unit = {
    try
      console.resetPromptLine(console.getPrompt, stashed.toString, stashed.cursor)
    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }
}
