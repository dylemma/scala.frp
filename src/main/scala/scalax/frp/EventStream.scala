package scalax.frp

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

	/** Assign a block of code that will run when this stream `stop`s. If this stream
	  * is already stopped, the block of code will run immediately.
	  * @param f A block of code that will run when this stream sends a `Stop` event.
	  */
	def onEnd(f: => Unit)(implicit obs: Observer): Unit =
		if (stopped) {
			f
		} else sink {
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

	/** Creates a new EventStream by applying a partial function to all events fired
	  * by this stream on which the function is defined. The resulting stream will stop
	  * when this stream stops.
	  *
	  * @param pf The partial function to apply to events from this stream
	  * @return A new stream that fires events transformed by `pf`
	  */
	def collect[B](pf: PartialFunction[A, B]): EventStream[B]

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

	/** Creates a new EventStream that updates its state for each new event fired by
	  * this stream. The state starts at `z` and updates along the lines of
	  * `state = op(state, event)` for every `event` fired by this stream. Each time
	  * the state is updated, the new stream fires an event containing the state.
	  *
	  * @param z The initial state for the fold
	  * @param op The update function, of the form `(state, next) => newState`
	  * @return A new stream that fires events as its state updates according to `op`
	  */
	def foldLeft[B](z: B)(op: (B, A) => B): EventStream[B]

	/** Creates a new EventStream that takes the first `count` events from this stream
	  * and fires them, then stops. The resulting stream will also stop if this stream
	  * stops anytime before it fires `count` new events.
	  *
	  * @param count The number of events that the resulting stream will re-fire
	  * @return A new stream that re-fires the first `count` events from this stream.
	  */
	def take(count: Int): EventStream[A]

	/** Creates a new EventStream that re-fires events from this stream as long as the
	  * event data satisfies `p`, the filter predicate. The first event `e` that causes
	  * `p(e)` to be `false` will cause the resulting stream to stop. The new stream will
	  * also stop if this stream is already stopped, or becomes stopped at any time.
	  *
	  * @param p The filter predicate function. Events fired by this stream will be passed
	  * into `p`. As soon as the result is `false`, the new stream will stop.
	  * @return A new stream that re-fires events from this stream until the filter
	  * predicate fails for an event.
	  */
	def takeWhile(p: A => Boolean): EventStream[A]

	/** Creates a new EventStream that ignores the first `count` events fired by this stream,
	  * then re-fires all events afterward. The resulting stream will stop when this stream does.
	  *
	  * @param count The number of events to ignore.
	  * @return A new stream that fires all events from this stream after having ignored
	  * `count` of them.
	  */
	def drop(count: Int): EventStream[A]

	/** Creates a new EventStream that will re-fire all events fired by this stream, starting
	  * as soon as the predicate function `p` returns `true` in response to one of the events.
	  * All events prior to the first "passed" event will be ignored. The resulting stream
	  * will stop when this stream stops.
	  *
	  * @param p The filter predicate function to evaluate events. Once this function returns
	  * `true`, all events (including the current one) will be re-fired.
	  * @return A new stream that ignores events until one of them causes `p` to return `true`.
	  */
	def dropWhile(p: A => Boolean): EventStream[A]

	/** Creates a new EventStream that represents the concatenation of this stream and `that`.
	  * The resulting stream will re-fire all events from this stream at first. Once this stream
	  * stops, the new stream will begin re-firing events from `that` stream, up until that one
	  * stops as well. If both `this` and `that` streams are stopped at the time of creation,
	  * the resulting stream will also be stopped.
	  *
	  * @param that Another EventStream whose events will be re-fired after this stream has stopped.
	  * @return The concatenation of `this` and `that` event stream.
	  */
	def ++[A1 >: A](that: EventStream[A1]): EventStream[A1]

	/** Creates a new EventStream that will re-fire all events from this stream until the `end`
	  * stream fires an event. The `end` stream stopping does not count as a fired event in this case.
	  * The resulting stream will also stop when and if this stream stops.
	  *
	  * @param end An EventStream whose first event marks the end of the resulting stream
	  * @return A new stream that re-fires events from this stream until the first event
	  * from the `end` stream.
	  */
	def until(end: EventStream[_]): EventStream[A]

	/** Creates a new EventStream that represents the union of `this` and `that` stream.
	  * All events from both streams will be re-fired in the order that they are encountered.
	  * The resulting stream will stop once both parent streams are stopped.
	  *
	  * @param that Any EventStream, to be joined with this stream in a Union.
	  * @return The Union of this stream and `that` stream.
	  */
	def ||[A1 >: A](that: EventStream[A1]): EventStream[A1]

	/** Creates a new EventStream that fires all events from this stream as `Left`s, and all
	  * events from `that` stream as `Right`s. It is essentially the same as a union, but
	  * appropriate for when `this` and `that` are streams of different types. The resulting
	  * stream will stop once both parent streams are stopped.
	  *
	  * @param that Any EventStream to be joined with this stream in an "Either" Union.
	  * @return A new stream that fires events from `this` and `that` as `Either`s.
	  */
	def either[B](that: EventStream[B]): EventStream[Either[A, B]]

	/** Creates a new EventStream that re-fires events from this stream that happen within
	  * the given `duration` from the time of creation. The resulting stream will stop
	  * when this stream stops, or when the `duration` runs out. Time-based expiration will
	  * generally happen on another thread, as it is handled by a `ScheduledExecutorService`.
	  *
	  * @param duration The amount of time before the resulting stream stops automatically.
	  * @return A new stream that represents all events fired by this stream, within the
	  * given `duration` from the time of creation.
	  */
	def within(duration: Duration): EventStream[A]

	/** Creates a new EventStream that re-fires events from this stream that happen before
	  * the given `deadline`. The resulting stream will stop automatically when the deadline
	  * expires, or when this stream stops. Time-based expiration will generally happen on
	  * another thread, as it is handled by a `ScheduledExecutorService`.
	  *
	  * @param `deadline` A timestamp that tells when the resulting stream should stop.
	  * @param A new stream that represents all events from this stream that happen before
	  * the `deadline`.
	  */
	def before(deadline: Deadline): EventStream[A]

	/** Creates a new EventStream that re-fires events from this stream, paired with the
	  * zero-based index of the event.
	  */
	def zipWithIndex: EventStream[(A, Int)]

	/** Creates a new EventStream that re-fires events from this stream, paired with a
	  * function that checks if that event is currently "stale". A stale event is one that
	  * is not currently the most recent event fired by the stream.
	  */
	def zipWithStaleness: EventStream[(A, () => Boolean)]

	/** Creates a new EventStream that re-fires events from this stream, paired with
	  * a time stamp representing when the event was fired.
	  *
	  * @tparam T The type of Time
	  * @param arg0 An instance of `Time[T]` that can generate time stamps. The default
	  * implementation is [[SystemTime]] which generates `Long` time stamps by calling
	  * `System.currentTimeMillis`.
	  */
	def zipWithTime[T: Time]: EventStream[(A, T)]

	/** Creates a new EventStream that joins this stream with `that` stream, firing events
	  * as pairs as soon as an event is available from both streams. The new stream will
	  * buffer events from both parent streams, so take care to avoid creating a zipped stream
	  * if one stream is expected to fire significantly more events than the other; the
	  * buffer for the larger stream will continue to accumulate without being emptied.
	  *
	  * Example usage:
	  * {{{
	  * val a = EventSource[Int]
	  * val b = EventSource[String]
	  * val c: EventStream[(Int, String)] = a zip b
	  *
	  * c foreach println _
	  *
	  * a fire 5
	  * a fire 2
	  * a fire 4
	  * b fire "A" // prints "(5, A)"
	  * b fire "B" // prints "(2, B)"
	  * b fire "C" // prints "(4, C)"
	  * }}}
	  *
	  * @param that The event stream to be zipped with this stream.
	  * @return A new stream that fires a pair for each corresponding pair of events from
	  * `this` stream and `that` stream.
	  */
	def zip[B](that: EventStream[B]): EventStream[(A, B)]

	//TODO: def unzip[A1, A2](implicit asPair: A => (A1, A2)): (EventStream[A1], EventStream[A2])
	//TODO: def grouped(size: Int): EventStream[List[A]]
	//TODO: def grouped(duration: Duration): EventStream[List[A]]
	//TODO: consider where using Typeclasses would be appropriate
}

object EventStream {
	/** An EventStream that will never fire any events, and is currently `stopped`. */
	val Nil: EventStream[Nothing] = {
		val s = EventSource[Nothing]
		s.stop
		s
	}

	//TODO: def interval[A](span: Duration, f: () => A)): EventStream[A] /*fire a new f() every span*/
}