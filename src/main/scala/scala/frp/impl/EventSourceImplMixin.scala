package scala.frp.impl

import scala.frp._
import scala.concurrent.duration.{Deadline, Duration, FiniteDuration}

private [frp] trait EventSourceImplMixin[A] { self: EventSource[A] =>
	
	def map[B](f: A => B)(implicit obs: Observer): EventStream[B] = {
		new MappedEventStream[A, B](this, f) 
	}
	
	def flatMap[B](f: A => EventStream[B])(implicit obs: Observer): EventStream[B] = {
		new FlatMappedEventStream(this, f)
	}
	
	def withFilter(p: A => Boolean)(implicit obs: Observer): EventStream[A] = {
		new WithFilterEventStream(this, p)
	}
		
	def filter(p: A => Boolean)(implicit obs: Observer): EventStream[A] = {
		new WithFilterEventStream(this, p)
	}

	def take(count: Int)(implicit obs: Observer): EventStream[A] = {
		new TakeCountEventStream(this, count)
	}
	
	def takeWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A] = {
		new TakeWhileEventStream(this, p)
	}
	
	def drop(count: Int)(implicit obs: Observer): EventStream[A] = {
		new DropCountEventStream(this, count)
	}
	
	def dropWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A] = {
		new DropWhileEventStream(this, p)
	}

	def ++[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1] = {
		new ConcatenatedEventStream(this, that)
	}
	
	def until(end: EventStream[_])(implicit obs: Observer): EventStream[A] = {
		new TakeUntilEventStream(this, end)
	}
	
	def ||[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1] = {
		new UnionEventStream(this, that)
	}
	
	def within(duration: Duration)(implicit obs: Observer): EventStream[A] = duration match {
		case d: FiniteDuration =>
			new DeadlinedEventStream(this, Deadline.now + d)
		case d if (d > Duration.Zero) => //infinite and positive
			this
		case _ =>
			EventStream.Nil
	}
	
	def before(deadline: Deadline)(implicit obs: Observer): EventStream[A] = {
		new DeadlinedEventStream(this, deadline)
	}
	
}