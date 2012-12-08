package scala.react.impl

import scala.react._

private [react] trait EventSourceImplMixin[A] { self: EventSource[A] =>
	
	def map[B](f: A => B)(implicit obs: Observer): EventStream[B] = {
		new MappedEventStream(this, f) 
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
}