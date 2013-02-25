package io.dylemma.frp

import impl._
import java.util.concurrent.atomic.AtomicBoolean
import collection.parallel.mutable.ParHashSet
import java.lang.ref.WeakReference

object EventSource {
	/** Convenience method for creating an `EventSource` instance, given some type, `A`. */
	def apply[A](): EventSource[A] = new EventSource[A] {}
}

/** EventSource is an implementation of [[EventStream]] that adds `fire` and `stop` methods.
  * Usage in client code will generally look something like
  * {{{
  * class CoolThings {
  * 	private val _events = EventSource[Thing]
  *
  * 	// don't expose `fire` and `stop` publicly: EventStream is read-only
  * 	def events: EventStream[Thing] = _events
  *
  * 	def doThings = { events fire new Thing(...) }
  * }
  * }}}
  */
trait EventSource[A] extends EventStream[A] with EventSourceImpl[A] {
	private val _stopped = new AtomicBoolean(false)

	def stopped: Boolean = _stopped.get
	def stop: Unit = if (_stopped.compareAndSet(false, true)) produce(Stop)
	def fire(event: A): Unit = {
		if (stopped) throw new IllegalStateException("Cannot fire events from a stopped EventSource")
		else produce(Fire(event))
	}

	/** A number indicating the minimum number of cleared references
	  * that must be encountered before purging all cleared references from the list.
	  * This method may be overridden - the default value is 5.
	  */
	protected def purgeThreshold: Int = 5

	private var refs = new ParHashSet[WeakReference[Event[A] => Boolean]]

	private[frp] def addHandler(handler: Event[A] => Boolean): Unit = {
		refs += new WeakReference(handler)
	}
	private[frp] def removeHandler(handler: Event[A] => Boolean): Unit = {
		for (ref <- refs.find(_.get == handler)) refs -= ref
	}

	/** Produce a new item. All `handler` functions will be called with `item` as
	  * the argument. There is no guarantee of the order in which the `handler`
	  * functions will be called.
	  *
	  * @param item The item to be sent to all `handler`s (sinks).
	  */
	protected def produce(item: Event[A]): Unit = {
		var deadCount = 0
		try {
			for (ref <- refs) ref.get match {
				//count dead references
				case null => deadCount += 1

				//run the handler: if it returns false, count it as a dead reference
				case handle if !handle(item) =>
					ref.clear
					deadCount += 1
				case _ =>
			}
		} finally {
			//purge is considered expensive: only do it if there are a lot of dead refs
			if (deadCount >= purgeThreshold) purge
		}
	}

	/* Removes dead handler references */
	private def purge = {
		var deadRefs = refs.filter(_.get == null)
		for (r <- deadRefs) refs -= r
	}
}