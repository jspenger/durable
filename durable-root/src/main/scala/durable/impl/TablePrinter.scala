package durable.impl

import durable.*

object TablePrinter:
  private def prettyRow(strIter: IterableOnce[String], maxWidth: Int, cellWidth: Int): String =
    val str = "| "
      + strIter.iterator
        .map(s => s.padTo(cellWidth, ' ').take(cellWidth))
        .mkString(" | ")
      + " |"
    if maxWidth == -1 then str
    else if str.length > maxWidth then str.take(maxWidth) + "..."
    else str

  /** Pretty print a `table`. Produces a table-like String representation. */
  inline def pprint[T, U <: Product](
      name: String,
      headers: List[String],
      table: Table[T, U],
      nRows: Int = -1,
      maxWidth: Int = -1,
      cellWidth: Int = 24,
  ): String =
    val pretty = prettyRow(_, maxWidth, cellWidth)
    val nCols = headers.size
    val rows = if nRows == -1 then table.rows else table.rows.take(nRows)

    val nameStr = pretty(Iterator.single(name))
    val headerStr = pretty(headers)
    val dividerStr = "+-" + (0 until nCols).map(_ => "-" * cellWidth).mkString("-+-") + "-+"
    val rowStrs = rows.map { case (key, row) => pretty(row.productIterator.map(_.toString())) }

    val istr =
      Iterable.single(dividerStr)
        ++ Iterable.single(nameStr)
        ++ Iterable.single(dividerStr)
        ++ Iterable.single(headerStr)
        ++ Iterable.single(dividerStr)
        ++ rowStrs
        ++ Iterable.single(dividerStr)
    istr.mkString("\n")
