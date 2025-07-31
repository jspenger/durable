package spores

object SporeExtractor {
  import spores.Packed.*

  object Packed0 {
    def unapply[T](obj: Spore[T]): Boolean = obj match {
      case PackedClass(_) | PackedObject(_) | PackedLambda(_) => true
      case _ => false
    }
  }

  object Packed1 {
    def unapply[T](obj: Spore[T]): Option[Tuple2[Spore[?], Spore[?]]] = obj match {
      case PackedWithEnv(packed, packedEnv) => Some((packed, packedEnv))
      case _ => None
    }
  }

  object Packed2 {
    def unapply[T](obj: Spore[T]): Option[Tuple2[Spore[?], Spore[?]]] = obj match {
      case PackedWithCtx(packed, packedEnv) => Some((packed, packedEnv))
      case _ => None
    }
  }
}
