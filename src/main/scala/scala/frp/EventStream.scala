package scala.frp

import scala.concurrent.duration._

/** An EventStream represents a (potentially finite) series of events that may occur at
  * any time in the future. EventStreams are stylistic replacements for Publishers
  * (from the Publisher/Subscriber, or Observer pattern). Events are produced as [[Event]]
  * instances - either a [[Fire]] or [[Stop]]. Each `Fire` event produced by an EventStream
  * represents a single "data". The `Stop` event represents the end of the stream.
  * Once an EventStream produces a `Stop` event, it is considered finished and will produce no more events.
  *
  * There are several ways to attach event handlers to an EventStream. The most low-level
  * way is to use the `sink` method, which operates on `Event` instances. The `foreach`
  * method is used to attach a handler that operates only on data from `Fire` events.
  * `onNext` and `onEnd` attach handlers to the next `Fire` and a `Stop`, respectively.
  * Attaching event handlers requires an implicit [[Observer]], which is used to manage
  * references between the handler and the stream.
  *
  * EventStreams can be transformed and combined in a variety of ways like filtering,
  * mapping, and concatenation.
  *
  * Below is a working example of EventStream usage.
  *
  * {{{
  * class Example extends App with Observer {
  * 	// Create an EventStream instance. EventSource is a subtype of
  * 	// EventStream, with the addition of `fire` and `stop` methods.
  * 	val stream = EventSource[Int]
  *
  * 	// Create a mapped version of the EventStream
  * 	val mappedStream = stream.map(_ * 2)
  *
  * 	// Attach an event handler, using the `foreach` syntax
  * 	for(i <- stream) println("Int event: " + i)
  * 	for(i <- mappedStream) println("Mapped event: " + i)
  *
  * 	// Attach an "end" event handler
  * 	stream onEnd { println("Got the end") }
  *
  * 	// fire some events
  * 	stream fire 1
  * 	// (prints "Int event: 1")
  * 	// (prints "Mapped event: 2")
  *
  * 	stream fire 5
  * 	// (prints "Int event: 5")
  * 	// (prints "Mapped event: 10")
  *
  * 	// stop the stream
  * 	stream.stop
  * 	// (prints "Got the end")
  * }
  * }}}
  */
trait EventStream[+A] {

	/** Add a handler function that acts as a `sink` for items produced by this
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
		addHandler(handler)
	}

	/** Marks whether or not this stream is stopped. A stopped stream will not
	  * produce any more events.
	  * @return `true` if this stream is stopped, `false` otherwise.
	  */
	def stopped: Boolean

	private[frp] def addHandler(handler: Event[A] => Boolean): Unit
	private[frp] def removeHandler(handler: Event[A] => Boolean): Unit

	/** Attach an event handler for data fired by this stream.
	  * @param f A function that takes in an event data and performs side effects.
	  */
	def foreach[U](f: A => U)(implicit obs: Observer): Unit = sink {
		case Fire(e) =>
			f(e)
			true
		case Stop =>
			false
	}

	/** Assign a block of code that will run when this stream `stop`s.
	  * @param f A block of code that will run when this stream sends a `Stop` event.
	  */
	def onEnd(f: => Unit)(implicit obs: Observer): Unit = sink {
		//TODO: should it run the `f` if this stream is already stopped?
		case Fire(_) => true
		case Stop =>
			f
			false
	}

	/** Assign a handler for the next event fired by this stream.
	  * @param f A function that takes in an event data and performs side effects.
	  * It can be assumed that `f` will be run at most once.
	  */
	def onNext(f: A => Unit)(implicit obs: Observer): Unit = sink {
		case Fire(e) =>
			f(e)
			false
		case Stop => false
	}

	/** Creates a mapped version of this EventStream. For every event `e`,
	  * fired by this stream, the mapped stream will fire an event equal
	  * to `f(e)`. The mapped stream will stop with this stream stops.
	  *
	  * @param f The transformation function to be applied to events from this stream.
	  * @return A new EventStream that fires events from this stream, transformed by `f`.
	  */
	def map[B](f: A => B): EventStream[B]

	/** Creates a new EventStream with the following behavior: for every event
	  * fired by this stream, a new stream will be created by applying `f` to that
	  * event; events from the new stream will be fired by the resulting stream until
	  * the next event from this stream, when the mapping re-starts. The resulting stream
	  * will stop when this stream stops.
	  *
	  * @param f A function that returns a new EventStream for every event fired by this stream.
	  * @return A new EventStream that fires events from the mapped streams, resetting the
	  * mapped stream every time this stream fires a new event.
	  */
	def flatMap[B](f: A => EventStream[B]): EventStream[B]

	/** Creates a new EventStream that fires all events from this stream that match
	  * the filter predicate. The resulting stream will stop when this stream stops.
	  *
	  * @param p The filter predicate. For each event `e`, fired by this stream, the
	  * filtered stream will fire `e` as long as `p(e)` returns `true`.
	  * @return A filtered version of this stream.
	  */
	def withFilter(p: A => Boolean): EventStream[A]

	/** Alias for `withFilter` */
	def filter(p: A => Boolean): EventStream[A]

	//TODO: scaladoc
	def take(count: Int): EventStream[A]

	//TODO: scaladoc
	def takeWhile(p: A => Boolean): EventStream[A]

	//TODO: scaladoc
	def drop(count: Int): EventStream[A]

	//TODO: scaladoc
	def dropWhile(p: A => Boolean): EventStream[A]

	//TODO: scaladoc
	def ++[A1 >: A](that: EventStream[A1]): EventStream[A1]

	//TODO: scaladoc
	def until(end: EventStream[_]): EventStream[A]

	//TODO: scaladoc
	def ||[A1 >: A](that: EventStream[A1]): EventStream[A1]

	//TODO: scaladoc
	def within(duration: Duration): EventStream[A]

	//TODO: scaladoc
	def before(deadline: Deadline): EventStream[A]

	//TODO: def next: Future[A]
	//TODO: def grouped(size: Int): EventStream[List[A]]
	//TODO: def either[B](that: EventStream[B]): EventStream[Either[A,B]]
}

object EventStream {
	/** An EventStream that will never fire any events, and is currently `stopped`. */
	val Nil: EventStream[Nothing] = {
		val s = EventSource[Nothing]
		s.stop
		s
	}
}