package scala.frp

import scala.concurrent.duration._

trait EventStream[+A] {

	/**
	 * Add a handler function that acts as a `sink` for items produced by this
	 * `Source`. The `handler` is expected to return `true` as long as it remains
	 * active. Once the `handler` function returns `false` in response to some
	 * produced item, it will be deactivated and will no longer receive new items.
	 * There is no guarantee of the order that handlers will be called.
	 *
	 * @param handler The handler function to receive items produced by this `Source`.
	 * Once the `handler` returns `false` in response to some produced item, it will
	 * be deactivated and will no longer receive new items.
	 *
	 * @param obs An implicit `Observer` which is required in order to properly
	 * manage references between this `Source` and any `handler`s, avoiding
	 * reference loops.
	 */
	def sink(handler: Event[A] => Boolean)(implicit obs: Observer): Unit = {
		/* Keep a reference to both `this` and the `handler` in memory, via the 
		 * implicit `Observer`. This way, neither can be garbage collected until
		 * the `Observer` can.
		 */
		obs add this
		obs add handler

		/* Add a weak reference to the handler function. By making it a *weak*
		 * reference, we ensure that this `Source` won't force the handler to
		 * stay in memory.
		 */
		//refs += new WeakReference(handler)
		addHandler(handler)
	}

	private[frp] def addHandler(handler: Event[A] => Boolean): Unit
	private[frp] def removeHandler(handler: Event[A] => Boolean): Unit

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