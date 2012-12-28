package scala.frp

import scala.concurrent.duration._

trait EventStream[+A] extends Stream[Event[A]] {
	def foreach[U](f: A => U)(implicit obs: Observer): Unit = {
		Sink.events(this) { e => f(e) }
	}
	def stopped: Boolean
	
	def map[B](f: A => B)(implicit obs: Observer): EventStream[B] 
	def flatMap[B](f: A => EventStream[B])(implicit obs: Observer): EventStream[B] 
	def withFilter(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def filter(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def take(count: Int)(implicit obs: Observer): EventStream[A]
	def takeWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def drop(count: Int)(implicit obs: Observer): EventStream[A] 
	def dropWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def ++[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1]
	def until(end: EventStream[_])(implicit obs: Observer): EventStream[A]
	def ||[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1]
	def within(duration: Duration)(implicit obs: Observer): EventStream[A]
	def before(deadline: Deadline)(implicit obs: Observer): EventStream[A]
	
	//todo: def next: Future[A]
	//todo: def grouped(size: Int): EventStream[List[A]]
	//todo: def either[B](that: EventStream[B]): EventStream[Either[A,B]]
}

object EventStream {
	val Nil = {
		val s = new EventSource[Nothing]
		s.stop
		s
	}
}