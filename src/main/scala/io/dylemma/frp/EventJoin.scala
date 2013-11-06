package io.dylemma.frp

import java.util.concurrent.atomic.AtomicBoolean

/** An EventJoin is a transformer that takes in events of type `A`
  * and `B` from two parent [[EventStream]]s and produces any number
  * of new events of type `C` in response.
  *
  * To create an EventJoin, implement the three required methods:
  * {{{
  * protected def leftPparent: EventStream[A]
  * protected def rightParent: EventStream[B]
  * protected def handle(event: Either[Event[A], Event[B]]): Boolean
  * }}}
  * Generally, `handle` will call `fire` and/or `stop`.
  * **Note:** to see the method signatures in this documentation, make
  * sure to select "Visibility: All".
  */
trait EventJoin[A, B, C] extends EventSource[C] {

	/** The "left" parent EventStream. Events from this stream
	  * will be passed into the `handle` method, wrapped in a
	  * `scala.util.Left`.
	  */
	protected def leftParent: EventStream[A]

	/** The "right" parent EventStream. Events from this stream
	  * will be passed into the `handle` method, wrapped in a
	  * `scala.util.Right`.
	  */
	protected def rightParent: EventStream[B]

	/** The method that reacts to events from the two parent streams.
	  * Generally, the body of this method will include calls to `fire`
	  * or `stop`, depending on the received `event`.
	  *
	  * **Note:** if both parents are `stopped`, this method will be
	  * ignored, and this EventJoin will automatically become stopped
	  * as well.
	  *
	  * @param event An event sent from one of the two parent streams.
	  * Events from the `leftParent` will be wrapped in a `Left`, while
	  * events from the `rightParent` will be wrapped in a `Right`.
	  *
	  * @return Whether or not to keep the `handle` method attached to
	  * the parent stream that sent the `event`. Returning `true` will
	  * maintain the attachment. Returning `false` will cause the
	  * corresponding parent to detach the `handle`.
	  */
	protected def handle(event: Either[Event[A], Event[B]]): Boolean

	/** This is needed so that the respective closure is not
	  * garbage-collected
	  */
  	protected lazy val leftHandlerFunc = { e: Event[A] => handle(Left(e)) }
  	/** This is needed so that the respective closures are not
	  * garbage-collected
	  */
  	protected lazy val rightHandlerFunc = { e: Event[B] => handle(Right(e)) }

	/** Checks whether both parent streams are stopped.
	  * @return `true` if and only if both the `leftParent` and
	  * `rightParent` streams are `stopped`.
	  */
	def parentsStopped: Boolean = {
		leftParent.stopped && rightParent.stopped
	}

	if (parentsStopped) {
		// if both parents are stopped, automatically stop this and skip the handler setup
		stop
	} else {
		//wrap leftParent's events in a Left
		leftParent addHandler leftHandlerFunc
		//wrap rightParent's events in a Right
		rightParent addHandler rightHandlerFunc
	}
}