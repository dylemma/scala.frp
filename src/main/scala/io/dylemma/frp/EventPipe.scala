package io.dylemma.frp

import java.util.concurrent.atomic.AtomicBoolean

/** An EventPipe is a transformer that takes in events of type `A`
  * from a single parent [[EventStream]] and produces any number of
  * new events of type `B` in response.
  *
  * To create an EventPipe, implement the two required methods:
  * {{{
  * protected def parent: EventStream[A]
  * protected def handle(event: Event[A]): Boolean
  * }}}
  * Generally, `handle` will call `fire` and/or `stop`.
  * **Note** to see the method signatures in this documentation, make
  * sure to select "Visibility: All".
  */
trait EventPipe[A, B] extends EventSource[B] {
	/** The stream that feeds events into this Pipe. The `handle`
	  * method will automatically be attached to the `parent`, unless
	  * the `parent` is `stopped`, in which case this stream will
	  * automatically be stopped.
	  */
	protected def parent: EventStream[A]

	/** The handler method that will be automatically attached to
	  * the `parent` stream. Generally, the body of this method will
	  * include a call to `fire` or `stop` in response to the `event`.
	  * The handler should return `false` if and only if it should be
	  * detached from the `parent` stream. Once it is detached, it
	  * will never be re-attached unless done explicitly.
	  *
	  * @param event An event sent from the `parent` stream.
	  * @return `true` to remain attached to the `parent`; `false` to detach.
	  */
	protected def handle(event: Event[A]): Boolean

	/** This is needed to prevent eta-expanded method
	  * from being garbage-collected.
	  */
	protected lazy val handleFunc = handle _

	if (parent.stopped) {
		// if the parent is stopped, then so should this
		stop()
	} else {
		// automatically attach the handler to the parent
		parent addHandler handleFunc
	}

}