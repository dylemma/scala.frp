package scala.frp

import java.util.concurrent.atomic.AtomicBoolean

trait EventJoin[A, B, C] extends Join[Event[A], Event[B], Event[C]] with EventSource[C]

trait Join[A, B, C] extends Source[C] {
	private val _sunk = new AtomicBoolean
	
	protected val parentA: Stream[A]
	protected val parentB: Stream[B]
	
	protected def consumeA(item: A): Boolean
	protected def consumeB(item: B): Boolean
	
	parentA addHandler consumeA _
	parentB addHandler consumeB _
}