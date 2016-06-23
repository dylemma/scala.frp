package io.dylemma.frp.impl

import io.dylemma.frp._
import scala.concurrent.duration.{ Deadline, Duration, FiniteDuration }

private[frp] trait EventSourceImpl[A] { self: EventSource[A] =>

	def map[B](f: A => B): EventStream[B] = {
		new MappedEventStream[A, B](this, f)
	}

	def collect[B](pf: PartialFunction[A, B]): EventStream[B] = {
		new CollectedEventStream(this, pf)
	}

	def flatMap[B](f: A => EventStream[B]): EventStream[B] = {
		new FlatMappedEventStream(this, f)
	}

	def withFilter(p: A => Boolean): EventStream[A] = {
		new WithFilterEventStream(this, p)
	}

	def filter(p: A => Boolean): EventStream[A] = {
		new WithFilterEventStream(this, p)
	}

	def foldLeft[B](z: B)(op: (B, A) => B): EventStream[B] = {
		new FoldLeftEventStream(this, z, op)
	}

	def take(count: Int): EventStream[A] = {
		new TakeCountEventStream(this, count)
	}

	def takeWhile(p: A => Boolean): EventStream[A] = {
		new TakeWhileEventStream(this, p)
	}

	def drop(count: Int): EventStream[A] = {
		new DropCountEventStream(this, count)
	}

	def dropWhile(p: A => Boolean): EventStream[A] = {
		new DropWhileEventStream(this, p)
	}

	def ++[A1 >: A](that: EventStream[A1]): EventStream[A1] = {
		new ConcatenatedEventStream(this, that)
	}

	def until(end: EventStream[_]): EventStream[A] = {
		new TakeUntilEventStream(this, end)
	}

	def ||[A1 >: A](that: EventStream[A1]): EventStream[A1] = {
		new UnionEventStream(this, that)
	}

	def either[B](that: EventStream[B]): EventStream[Either[A, B]] = {
		new EitherEventStream(this, that)
	}

	def within(duration: Duration): EventStream[A] = duration match {
		case d: FiniteDuration      => new DeadlinedEventStream(this, Deadline.now + d)
		case d if d > Duration.Zero => this //infinite and positive
		case _                      => EventStream.Nil
	}

	def before(deadline: Deadline): EventStream[A] = {
		new DeadlinedEventStream(this, deadline)
	}

	def zipWithIndex: EventStream[(A, Int)] = {
		new ZipWithIndexEventStream(this)
	}

	def zipWithStaleness: EventStream[(A, () => Boolean)] = {
		new ZipWithStalenessEventStream(this)
	}

	def zipWithTime[T: Time]: EventStream[(A, T)] = {
		map { _ -> implicitly[Time[T]].currentTime }
	}

	def zip[B](that: EventStream[B]): EventStream[(A, B)] = {
		new ZippedEventStream(this, that)
	}

	def unzip[A1, A2](implicit asPair: A => (A1, A2)): (EventStream[A1], EventStream[A2]) = {
		val left = this.map { p => asPair(p)._1 }
		val right = this.map { p => asPair(p)._2 }
		left -> right
	}

	def grouped(size: Int): EventStream[List[A]] = {
		new GroupedEventStream(this, size)
	}
}