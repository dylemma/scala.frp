package scala.frp.impl

import scala.frp._

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.concurrent.duration._

//todo: create a "DerivedEventStream" that handles the logic for hooking up to parent streams
// with the *goal* of removing the Observer requirement for all of the combinators.

private [frp] class FlatMappedEventStream[A, B]
(val parent: EventStream[A], f: A => EventStream[B])
extends EventPipe[A, B] {
	
	private val _b = new AtomicReference[EventStream[B]] //initially null
	
	private val handler = (event: Event[B]) => event match {
		case _ if stopped => false
		case Fire(e) =>
			fire(e)
			true
		case Stop =>
			stop
			false
	}
	
	def consume(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			val b = f(e)
			
			//cancel the old stream
			_b getAndSet(b) match {
				case null => //no op
				case old => old removeHandler handler
			}
			
			b addHandler handler
			true
	}
}

private [frp] 
class CancellableEventStream[A]
(base: EventStream[A])(implicit obs: Observer) 
extends EventSource[A] {

	private val canceledAtomic = new AtomicBoolean(false)
	private val stoppedAtomic = new AtomicBoolean(false)
	
	def isCancelled: Boolean = canceledAtomic.get
	
	def cancel: Unit = canceledAtomic.set(true)
	
	private def tryStop = if(stoppedAtomic.compareAndSet(false, true)) stop
	
	base sink {
		case Stop =>
			tryStop
			false
		case Fire(e) =>
			if(isCancelled){
				tryStop
				false
			}
			else {
				fire(e)
				true
			}
	}
}

private [frp]
class WithFilterEventStream[A]
(protected val parent: EventStream[A], f: A => Boolean)
extends EventPipe[A, A] {
	protected def consume(item: Event[A]) = item match {
		case Fire(e) =>
			if( f(e) ) fire(e)
			true
		case Stop =>
			stop
			false
	}
	
	override def withFilter(p: A => Boolean): EventStream[A] = {
		val mergedFilter = (e: A) => { f(e) && p(e) }
		new WithFilterEventStream(this, mergedFilter)
	}
}

private [frp] class MappedEventStream[A,B]
(protected val parent: EventStream[A], f: A => B)
extends EventPipe[A, B] {
	protected def consume(item: Event[A]) = item match {
		case Fire(e) =>
			fire( f(e) )
			true
		case Stop =>
			stop
			false
	}
}

private [frp]
class TakeWhileEventStream[A]
(protected val parent: EventStream[A], p: A => Boolean)
extends EventPipe[A, A] {

	protected def consume(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			if( p(e) ){
				fire(e)
				true
			} else {
				stop
				false
			}
	}

}

private [frp]
class TakeCountEventStream[A]
(protected val parent: EventStream[A], count: Int)
extends EventPipe[A, A] {

	private var numSeen = 0

	protected def consume(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			numSeen += 1
			fire(e)
			
			if (numSeen >= count){
				stop
				false
			} else {
				true
			}
	}
	
}

private [frp]
class DropWhileEventStream[A]
(protected val parent: EventStream[A], p: A => Boolean)
extends EventPipe[A, A] {
	private var dropping = true
	
	protected def consume(event: Event[A]) = event match {
		case Stop => 
			stop
			false
		case Fire(e) => 
			if(dropping && !p(e)){
				dropping = false
				fire(e)
			} else if (!dropping){
				fire(e)
			}
			true
	}
}

private [frp]
class DropCountEventStream[A]
(protected val parent: EventStream[A], count: Int)
extends EventPipe[A, A] {
	
	private var numSeen = 0
	
	protected def consume(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			numSeen += 1
			if(numSeen > count) fire(e)
			true
	}
	
}

private [frp] class ConcatenatedEventStream[A]
(protected val parentA: EventStream[A], protected val parentB: EventStream[A])
extends EventJoin[A, A, A] {
	
	private var advanced = parentA.stopped
	
	protected def consumeA(event: Event[A]) = event match {
		case _ if advanced => false
		case Fire(e) =>
			fire(e)
			true
		case Stop =>
			advanced = true
			false
	}
	
	protected def consumeB(event: Event[A]) = event match {
		case _ if !advanced => true
		case Fire(e) =>
			fire(e)
			true
		case Stop =>
			stop
			false
	}
}

/**
  * @param parentA the 'source' stream
  * @param parentB the 'end' stream
  */
private [frp] class TakeUntilEventStream[A]
(val parentA: EventStream[A], val parentB: EventStream[_])
extends EventJoin[A, Any, A] {
	
	def consumeA(event: Event[A]) = event match {
		case _ if stopped => false
		case Stop =>
			stop
			false
		case Fire(e) =>
			fire(e)
			true
	}
	
	def consumeB(event: Event[Any]) = {
		stop
		false
	}
}

private [frp]
class UnionEventStream[A]
(val parentA: EventStream[A], val parentB: EventStream[A])
extends EventJoin[A, A, A] {

	val stopCounter = new AtomicInteger(0)

	private val handle = (e: Event[A]) => e match {
		case Stop => 
			val stopCount = stopCounter.incrementAndGet
			if (stopCount == 2) stop
			false
		case Fire(x) =>
			fire(x)
			true
	}

	def consumeA(event: Event[A]) = handle(event)
	def consumeB(event: Event[A]) = handle(event)

}

private [frp]
class DeadlinedEventStream[A]
(val parent: EventStream[A], deadline: Deadline)
extends EventPipe[A, A] {

	private def stopinate = {
		println("Stopping " + this)
		stop
	}

	TimeBasedFutures.after(deadline, stopinate)

	def consume(event: Event[A]) = event match {
		case Stop => 
			stopinate
			false
		case Fire(_) if deadline.isOverdue => 
			stopinate
			false
		case Fire(e) =>
			fire(e)
			true
	}

}
