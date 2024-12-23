package durable.impl

import scala.collection.mutable.Map

import upickle.default.*

case class Table[K, R](
    rows: Map[K, R],
) derives ReadWriter {
  def insert(key: K, row: R): Unit =
    this.rows.update(key, row)

  def get(key: K): Option[R] =
    this.rows.get(key)

  def softDelete(key: K): Unit =
    this.rows -= key
}

object Table {
  def empty[K, R]: Table[K, R] =
    Table(Map.empty[K, R])
}
