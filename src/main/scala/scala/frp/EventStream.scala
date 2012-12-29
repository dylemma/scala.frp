package scala.frp

import scala.concurrent.duration._

trait EventStream[+A] extends Stream[Event[A]] {
	def foreach[U](f: A => U)(implicit obs: Observer): Unit = sink {
		case Fire(e) =>
			f(e)
			true
		case Stop =>
			false
	}
	def onEnd(f: => Unit)(implicit obs: Observer): Unit = sink {
		case Fire(_) => true
		case Stop =>
			f
			false
	}
	def onNext(f: A => Unit)(implicit obs: Observer): Unit = sink {
		case Fire(e) =>
			f(e)
			false
		case Stop => false
	}
	
	def stopped: Boolean
	def map[B](f: A => B): EventStream[B] 
	def flatMap[B](f: A => EventStream[B]): EventStream[B] 
	def withFilter(p: A => Boolean): EventStream[A]
	def filter(p: A => Boolean): EventStream[A]
	def take(count: Int): EventStream[A]
	def takeWhile(p: A => Boolean): EventStream[A]
	def drop(count: Int): EventStream[A] 
	def dropWhile(p: A => Boolean): EventStream[A]
	def ++[A1 >: A](that: EventStream[A1]): EventStream[A1]
	def until(end: EventStream[_]): EventStream[A]
	def ||[A1 >: A](that: EventStream[A1]): EventStream[A1]
	def within(duration: Duration): EventStream[A]
	def before(deadline: Deadline): EventStream[A]
	
	//todo: def next: Future[A]
	//todo: def grouped(size: Int): EventStream[List[A]]
	//todo: def either[B](that: EventStream[B]): EventStream[Either[A,B]]
}

object EventStream {
	val Nil = {
		val s = EventSource[Nothing]
		s.stop
		s
	}
}