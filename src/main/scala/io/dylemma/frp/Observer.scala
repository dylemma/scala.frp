package io.dylemma.frp

/** A memory management assistant. The expected usage is that an `Observer`
  * instance will be the only thing that holds strong references to some
  * collection of objects; once the `Observer` instance is no longer
  * strongly-reachable (can be garbage collected), then neither will the
  * collection of objects that it references.
  *
  * An implicit `Observer` is required when interacting with `Source`s, to
  * facilitate "automatic" memory management. To create an `Observer`, either
  * explicitly do so:
  
  * {{{ implicit object myObserver extends Observer }}}
  
  * or mix one into the containing class:
  
  * {{{
  * class Worker extends Observer {
  *     //do things that require an implicit Observer
  * }
  * }}}
  */
trait Observer {
	private val references = collection.mutable.ListBuffer[Any]()
	
	def add(ref: Any): Unit = references += ref
	
	implicit def observer: Observer = this
}