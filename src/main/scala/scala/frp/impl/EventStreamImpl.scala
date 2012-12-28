package scala.frp.impl

import scala.frp._

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.concurrent.duration._

//todo: create a "DerivedEventStream" that handles the logic for hooking up to parent streams
// with the *goal* of removing the Observer requirement for all of the combinators.


private [frp] 
class FlatMappedEventStream[A, B]
(base: EventStream[A], f: A => EventStream[B])(implicit obs: Observer) 
extends EventSource[B] {

	private val bStreamAtomic = new AtomicReference[CancellableEventStream[B]]

	//when base fires an event, subscribe to the resulting event stream, until base fires another event

	base sink {
		case Stop =>
			stop
			false
		case Fire(e) =>
			val bStream = new CancellableEventStream( f(e) )
			
			//cancel the old stream
			bStreamAtomic getAndSet(bStream) match {
				case null => //no op
				case oldStream => 
					oldStream.cancel
			}
			
			bStream sink {
				case Stop =>
					false
				case Fire(x) =>
					fire(x)
					true
			}
			
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
(base: EventStream[A], f: A => Boolean)(implicit obs: Observer) 
extends EventSource[A] {

	base sink {
		case Fire(e) =>
			if( f(e) ) fire(e)
			true
		case Stop =>
			stop
			false
	}
	
	override def withFilter(p: A => Boolean)(implicit obs: Observer): EventStream[A] = {
		val mergedFilter = (e: A) => { f(e) && p(e) }
		new WithFilterEventStream(this, mergedFilter)
	}

}

private [frp]
class MappedEventStream[A, B]
(base: EventStream[A], f: A => B)(implicit obs: Observer) 
extends EventSource[B] {
	
	base sink {
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
(base: EventStream[A], p: A => Boolean)(implicit obs: Observer) 
extends EventSource[A] {

	base sink {
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
(base: EventStream[A], count: Int)(implicit obs: Observer) 
extends EventSource[A] {

	private var numSeen = 0

	base sink {
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
(base: EventStream[A], p: A => Boolean)(implicit obs: Observer)
extends EventSource[A] {
	private var dropping = true
	
	base sink {
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
(base: EventStream[A], count: Int)(implicit obs: Observer)
extends EventSource[A] {
	
	private var numSeen = 0
	
	base sink {
		case Stop =>
			stop
			false
		case Fire(e) =>
			numSeen += 1
			if(numSeen > count) fire(e)
			true
	}
	
}

private [frp]
class ConcatenatedEventStream[A]
(left: EventStream[A], right: EventStream[A])(implicit obs: Observer)
extends EventSource[A] {
	
	left sink {
		case Stop =>
			right sink {
				case Stop =>
					stop
					false
				case Fire(e) =>
					fire(e)
					true
			}
			false
		case Fire(e) =>
			fire(e)
			true
	}
	
}

private [frp]
class TakeUntilEventStream[A]
(base: EventStream[A], end: EventStream[_])(implicit obs: Observer)
extends CancellableEventStream[A](base) {

	end sink {
		case Stop => 
			false
		case Fire(_) =>
			cancel
			false
	}

}

private [frp]
class UnionEventStream[A]
(left: EventStream[A], right: EventStream[A])(implicit obs: Observer)
extends EventSource[A] {

	val stopCounter = new AtomicInteger(0)

	val eventHandler = (e: Event[A]) => e match {
		case Stop => 
			val stopCount = stopCounter.incrementAndGet
			if (stopCount == 2) stop
			false
		case Fire(x) =>
			fire(x)
			true
	}

	left sink eventHandler
	right sink eventHandler

}

private [frp]
class DeadlinedEventStream[A]
(base: EventStream[A], deadline: Deadline)(implicit obs: Observer)
extends EventSource[A] {

	private def stopinate = {
		println("Stopping " + this)
		stop
	}

	TimeBasedFutures.after(deadline, stopinate)

	base sink {
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
