package scala.react

import java.lang.ref.WeakReference

sealed abstract class Event[+A]
case class Fire[A](event: A) extends Event[A]
case object Stop extends Event[Nothing]

trait Observer {
	private val references = collection.mutable.ListBuffer[Any]()
	
	def add(ref: Any): Unit = references += ref
}

trait Observable[+A] {
	def observe(thunk: Event[A] => Boolean)(implicit o: Observer): Unit 
}

object Observe {

	def next[A](observable: Observable[A])(f: A => Unit)(implicit o: Observer): Unit = {
		observable observe {
			case Stop => false
			case Fire(event) =>
				f(event)
				false
		}
	}
	
	def events[A](observable: Observable[A])(f: A => Unit)(implicit o: Observer): Unit = {
		observable observe {
			case Stop => false
			case Fire(event) =>
				f(event)
				true
		}
	}
	
	def end[A](observable: Observable[A])(f: => Unit)(implicit o: Observer): Unit = {
		observable observe {
			case Fire(_) => true
			case Stop =>
				f
				false
		}
	}

}