package scala.frp

import java.util.concurrent.atomic.AtomicBoolean

trait EventJoin[A, B, C] extends EventSource[C] {
	protected def leftParent: EventStream[A]
	protected def rightParent: EventStream[B]

	protected def handle(event: Either[Event[A], Event[B]]): Boolean

	def parentsStopped: Boolean = {
		leftParent.stopped && rightParent.stopped
	}

	if (parentsStopped) {
		stop
	} else {
		leftParent addHandler {
			case e => handle(Left(e))
		}
		rightParent addHandler {
			case e => handle(Right(e))
		}
	}
}