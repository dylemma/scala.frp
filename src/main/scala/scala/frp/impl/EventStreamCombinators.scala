package scala.frp.impl

import scala.frp._

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicReference }
import scala.concurrent.duration._

private[frp] class FlatMappedEventStream[A, B](val parent: EventStream[A], f: A => EventStream[B])
	extends EventPipe[A, B] {

	private val _b = new AtomicReference[EventStream[B]] //initially null

	private val handler = (event: Event[B]) => event match {
		case _ if stopped =>
			false
		case Fire(e) =>
			fire(e)
			true
		case Stop =>
			false
	}

	def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			val b = f(e)

			//cancel the old stream
			_b getAndSet (b) match {
				case null => //no op
				case old => old removeHandler handler
			}
			if (!b.stopped) b addHandler handler
			true
	}
}

private[frp] class WithFilterEventStream[A](protected val parent: EventStream[A], f: A => Boolean)
	extends EventPipe[A, A] {
	protected def handle(item: Event[A]) = item match {
		case Fire(e) =>
			if (f(e)) fire(e)
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

private[frp] class FoldLeftEventStream[A, B](protected val parent: EventStream[A], init: B, op: (B, A) => B)
	extends EventPipe[A, B] {

	private var accum = init

	protected def handle(item: Event[A]) = item match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			accum = op(accum, e)
			fire(accum)
			true
	}
}

private[frp] class MappedEventStream[A, B](protected val parent: EventStream[A], f: A => B)
	extends EventPipe[A, B] {

	protected def handle(item: Event[A]) = item match {
		case Fire(e) =>
			fire(f(e))
			true
		case Stop =>
			stop
			false
	}
}

private[frp] class CollectedEventStream[A, B](protected val parent: EventStream[A], f: PartialFunction[A, B])
	extends EventPipe[A, B] {

	protected def handle(item: Event[A]) = item match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			if (f isDefinedAt e) fire(f(e))
			true
	}
}

private[frp] class TakeWhileEventStream[A](protected val parent: EventStream[A], p: A => Boolean)
	extends EventPipe[A, A] {

	protected def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			if (p(e)) {
				fire(e)
				true
			} else {
				stop
				false
			}
	}

}

private[frp] class TakeCountEventStream[A](protected val parent: EventStream[A], count: Int)
	extends EventPipe[A, A] {

	private var numSeen = 0

	protected def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			numSeen += 1
			fire(e)

			if (numSeen >= count) {
				stop
				false
			} else {
				true
			}
	}

}

private[frp] class DropWhileEventStream[A](protected val parent: EventStream[A], p: A => Boolean)
	extends EventPipe[A, A] {
	private var dropping = true

	protected def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			if (dropping && !p(e)) {
				dropping = false
				fire(e)
			} else if (!dropping) {
				fire(e)
			}
			true
	}
}

private[frp] class DropCountEventStream[A](protected val parent: EventStream[A], count: Int)
	extends EventPipe[A, A] {

	private var numSeen = 0

	protected def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(e) =>
			numSeen += 1
			if (numSeen > count) fire(e)
			true
	}

}

private[frp] class ConcatenatedEventStream[A](protected val leftParent: EventStream[A], protected val rightParent: EventStream[A])
	extends EventJoin[A, A, A] {

	private var advanced = leftParent.stopped

	def handle(event: Either[Event[A], Event[A]]): Boolean = event match {
		case Left(_) if advanced => false //disconnect
		case Right(_) if !advanced => true //keep waiting

		case Left(Fire(e)) =>
			fire(e)
			true
		case Right(Fire(e)) =>
			fire(e)
			true

		case Left(stop) =>
			advanced = true
			false
		case Right(Stop) =>
			stop
			false
	}
}

/** @param parentA the 'source' stream
  * @param parentB the 'end' stream
  */
private[frp] class TakeUntilEventStream[A](val leftParent: EventStream[A], val rightParent: EventStream[_])
	extends EventJoin[A, Any, A] {

	def handle(event: Either[Event[A], Event[Any]]): Boolean = event match {
		case Right(Fire(_)) =>
			stop
			false
		case Right(Stop) => false
		case Left(_) if stopped => false
		case Left(Stop) =>
			stop
			false
		case Left(Fire(e)) =>
			fire(e)
			true
	}
}

private[frp] class UnionEventStream[A](val leftParent: EventStream[A], val rightParent: EventStream[A])
	extends EventJoin[A, A, A] {

	def handle(event: Either[Event[A], Event[A]]) = event match {
		case Left(Stop) | Right(Stop) =>
			if (parentsStopped) stop
			false
		case Left(Fire(x)) =>
			fire(x)
			true
		case Right(Fire(x)) =>
			fire(x)
			true
	}

}

private[frp] class EitherEventStream[A, B](val leftParent: EventStream[A], val rightParent: EventStream[B])
	extends EventJoin[A, B, Either[A, B]] {

	def handle(event: Either[Event[A], Event[B]]) = event match {
		case Left(Stop) | Right(Stop) =>
			if (parentsStopped) stop
			false
		case Left(Fire(x)) =>
			fire(Left(x))
			true
		case Right(Fire(x)) =>
			fire(Right(x))
			true
	}
}

private[frp] class DeadlinedEventStream[A](val parent: EventStream[A], deadline: Deadline)
	extends EventPipe[A, A] {

	TimeBasedFutures.after(deadline, stop)

	def handle(event: Event[A]) = event match {
		case Stop =>
			stop
			false
		case Fire(_) if deadline.isOverdue =>
			stop
			false
		case Fire(e) =>
			fire(e)
			true
	}

}
