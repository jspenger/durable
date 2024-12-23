package durable.impl

import upickle.default.*

import durable.*

/** Serializable representation of the execution state. */
case class State(
  // format: off
    dBlock:       Table[UID, DBlock],
    dPromiseData: Table[UID, DPromiseData],
    dLog:         Table[UID, DLogMsg],
    var dUID:     UID,
    // format: on
) derives ReadWriter {

  def clear(): Unit =
    this.dBlock.rows.clear()
    this.dPromiseData.rows.clear()
    this.dLog.rows.clear()
    this.dUID = 0

  def replaceWith(other: State): Unit =
    this.clear()
    // format: off
    this.dBlock.rows       ++= other.dBlock.rows
    this.dPromiseData.rows ++= other.dPromiseData.rows
    this.dLog.rows         ++= other.dLog.rows
    this.dUID                = other.dUID
    // format: on
}

object State {
  def empty: State = State(
    Table.empty[UID, DBlock],
    Table.empty[UID, DPromiseData],
    Table.empty[UID, DLogMsg],
    0,
  )
}
