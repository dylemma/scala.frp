package scala.frp

import impl._
import java.util.concurrent.atomic.AtomicBoolean

object EventSource {
	def apply[A](): EventSource[A] = new EventSource[A]{}
}

trait EventSource[A] extends EventStream[A] with Source[Event[A]] with EventSourceImplMixin[A] {
	private val _stopped = new AtomicBoolean(false)
	
	def stopped: Boolean = _stopped.get
	def stop: Unit = if( _stopped.compareAndSet(false, true) ) produce( Stop )
	def fire(event: A): Unit = {
		if(stopped) throw new IllegalStateException("Cannot fire events from a stopped EventSource")
		else produce( Fire(event) )	
	}
}