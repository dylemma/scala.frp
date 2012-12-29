package scala.frp

import java.util.concurrent.atomic.AtomicBoolean

trait EventPipe[A, B] extends Pipe[Event[A], Event[B]] with EventSource[B]

trait Pipe[A, B] extends Source[B] {
	private val _sunk = new AtomicBoolean
	
	protected val parent: Stream[A]
	protected def consume(item: A): Boolean
	
	parent addHandler consume _
}