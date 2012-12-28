package scala.frp

import java.lang.ref.WeakReference

//todo: I think that this should be replaced by implicit methods or something... I don't like this syntax

object Sink {

	def next[A](observable: Stream[Event[A]])(f: A => Unit)(implicit o: Observer): Unit = {
		observable sink {
			case Stop => false
			case Fire(event) =>
				f(event)
				false
		}
	}
	
	def events[A](observable: Stream[Event[A]])(f: A => Unit)(implicit o: Observer): Unit = {
		observable sink {
			case Stop => false
			case Fire(event) =>
				f(event)
				true
		}
	}
	
	def end[A](observable: Stream[Event[A]])(f: => Unit)(implicit o: Observer): Unit = {
		observable sink {
			case Fire(_) => true
			case Stop =>
				f
				false
		}
	}

}