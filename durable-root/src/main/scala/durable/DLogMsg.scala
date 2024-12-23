package durable

import upickle.default.*

/** Representation of a log message. */
private[durable] case class DLogMsg(
    uid: UID,
    timestamp: Long,
    message: String
) derives ReadWriter
