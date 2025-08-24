package durable.example.interactive

import java.net.*
import java.nio.*
import java.nio.channels.*

import spores.default.*
import spores.default.given

import durable.*
import durable.given

private class Server(host: String, port: Int):
  private val selector = Selector.open()
  private val serverSocket = ServerSocketChannel.open()

  serverSocket.bind(new InetSocketAddress(host, port))
  serverSocket.configureBlocking(false)
  serverSocket.register(selector, serverSocket.validOps())

  private val buffer = ByteBuffer.allocate(1024)

  private var selected: Option[Selector] = None
  private var keys: Option[java.util.Iterator[SelectionKey]] = None
  private def keysNext =
    keys match
      case Some(iterator) =>
        if iterator.hasNext() then
          val nxt = iterator.next()
          iterator.remove()
          Some(nxt)
        else
          keys = None
          selected = None
          None
      case None =>
        selected match
          case None =>
            selector.selectNow() match
              case 0 => None
              case _ =>
                keys = Some(selector.selectedKeys().iterator())
                selected = Some(selector)
                None
          case Some(selector) =>
            keys = Some(selector.selectedKeys().iterator())
            None

  def next: Option[(SocketChannel, ByteBuffer)] = keysNext match
    case None => None
    case Some(nxt) if nxt.isAcceptable() =>
      val chnl = nxt.channel().asInstanceOf[ServerSocketChannel]
      val client = chnl.accept()
      client.configureBlocking(false)
      client.register(selector, SelectionKey.OP_READ)
      None
    case Some(nxt) if nxt.isReadable() =>
      buffer.clear()
      val chnl = nxt.channel().asInstanceOf[SocketChannel]
      val bytesRead = chnl.read(buffer)
      bytesRead match
        case -1 =>
          chnl.close()
          None
        case _ =>
          buffer.flip()
          Some((chnl, buffer))
    case Some(nxt) =>
      throw new Exception("Unexpected selection key")

object EchoServer:
  inline val host = "localhost"
  inline val port = 8080
  lazy val server = Server(host, port)
  val buffer = ByteBuffer.allocate(1024)

  inline def parse(req: String, field: String): String =
    req.split("\r\n").find(_.startsWith(field)).get.split("=")(1)

  inline def response(msgs: String*): String =
    val msg = msgs.mkString("\r\n")
    "HTTP/1.1 200 OK\r\n"
      + "Content-Length: " + msg.length + "\r\n"
      + "Content-Type: text/plain\r\n"
      + "\r\n"
      + msg

  inline def sleep = Thread.sleep(1_000)

  def listen(history: List[String])(using DExecutionContext): DFuture[Unit] = DFuture {
    Spore.applyWithEnv(history) { history =>
      sleep
      ctx.log("Listening on " + host + ":" + port)
      server.next match
        case Some((chnl, buffer)) =>
          val req = String(buffer.array(), 0, buffer.limit())
          val msg = parse(req, "msg")
          val res = response("msg=" + msg, "history=" + history.mkString(", "))

          val newHistory = (msg :: history).take(10)

          ctx.log("Echo: " + msg + ", History: " + newHistory.mkString(", "))

          buffer.clear()
          buffer.put(res.getBytes())
          buffer.flip()
          chnl.write(buffer)

          listen(newHistory)

        case None =>
          listen(history)
    }
  }

  val workflow = DWorkflow {
    Spore.apply0 {
      listen(List.empty)
    }
  }

  def main(args: Array[String]): Unit =
    val service = DExecutionServices.synchronous("echoserver")
    // Observability.watch(service) // Uncomment to watch the service
    service.execute(workflow)

    // Message the server:
    // curl -X POST -d "msg=Hello" http://localhost:8080
