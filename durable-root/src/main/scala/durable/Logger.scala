package durable

trait Logger:
  val name: String

  private inline def msgFactory(level: Logger.Level, msg: Any): String =
    Logger.getTimestamp
      + " "
      + level.toString().padTo(5, ' ')
      + " "
      + name
      + " - "
      + msg.toString()

  private inline def printLevel(level: Logger.Level, msg: Any): Unit =
    if Logger._rootLevel.lvl <= level.lvl then println(msgFactory(level, msg))

  def verbose(msg: Any): Unit =
    printLevel(Logger.Level.VERBOSE, msg)

  def debug(msg: Any): Unit =
    printLevel(Logger.Level.DEBUG, msg)

  def info(msg: Any): Unit =
    printLevel(Logger.Level.INFO, msg)

  def warn(msg: Any): Unit =
    printLevel(Logger.Level.WARN, msg)

  def error(msg: Any): Unit =
    printLevel(Logger.Level.ERROR, msg)

object Logger:

  // format: off
  enum Level(val lvl: Int):
    case VERBOSE extends Level(0)
    case DEBUG   extends Level(1)
    case INFO    extends Level(2)
    case WARN    extends Level(3)
    case ERROR   extends Level(4)
  // format: on

  private var _rootLevel = Level.INFO

  def apply(_name: String): Logger =
    new Logger { val name = _name }

  def setRootLevel(level: Level): Unit =
    _rootLevel = level

  private def formatNumber(number: Int, digits: Int): String =
    val padded = "0" * (digits - number.toString().length) + number.toString()
    padded.substring(0, digits)

  private def getTimestamp: String =
    // format: off
    val now = java.time.LocalDateTime.now()
    val hours =        formatNumber(now.getHour(),   2)
    val minutes =      formatNumber(now.getMinute(), 2)
    val seconds =      formatNumber(now.getSecond(), 2)
    val milliseconds = formatNumber(now.getNano() / 1_000_000, 3)
    s"$hours:$minutes:$seconds.$milliseconds"
    // format: on
