package durable

import durable.impl.*

object DExecutionServices:
  def synchronous(fname: String): DExecutionService = new DExecutionServiceImpl(fname)
end DExecutionServices
