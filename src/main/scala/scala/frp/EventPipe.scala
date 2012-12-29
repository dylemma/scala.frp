package scala.frp

import java.util.concurrent.atomic.AtomicBoolean

trait EventPipe[A, B] extends EventSource[B] {
	protected def parent: EventStream[A]
	protected def handle(event: Event[A]): Boolean

	private val _sunk = new AtomicBoolean(false)

	if (_sunk.compareAndSet(false, true)) {
		if (!parent.stopped) {
			parent addHandler handle _
		} else {
			stop
		}
	}

}