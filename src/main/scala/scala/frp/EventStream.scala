package scala.frp

import impl._
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.ref.WeakReference
import scala.concurrent.duration._

trait EventStream[+A] extends Observable[A]{
	def foreach[U](f: A => U)(implicit obs: Observer): Unit = {
		Observe.events(this) { e => f(e) }
	}
	def isStopped: Boolean
	
	def map[B](f: A => B)(implicit obs: Observer): EventStream[B] 
	def flatMap[B](f: A => EventStream[B])(implicit obs: Observer): EventStream[B] 
	def withFilter(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def filter(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def take(count: Int)(implicit obs: Observer): EventStream[A]
	def takeWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def drop(count: Int)(implicit obs: Observer): EventStream[A] 
	def dropWhile(p: A => Boolean)(implicit obs: Observer): EventStream[A]
	def ++[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1]
	def until(end: EventStream[_])(implicit obs: Observer): EventStream[A]
	def ||[A1 >: A](that: EventStream[A1])(implicit obs: Observer): EventStream[A1]
	def within(duration: Duration)(implicit obs: Observer): EventStream[A]
	def before(deadline: Deadline)(implicit obs: Observer): EventStream[A]
	
	//todo: def next: Future[A]
	//todo: def grouped(size: Int): EventStream[List[A]]
	//todo: def either[B](that: EventStream[B]): EventStream[Either[A,B]]
}

object EventStream {
	val Nil = {
		val s = new EventSource[Nothing]
		s.stop
		s
	}
}

object EventSource {
	val purgeThreshold = 5
}

class EventSource[A] extends EventStream[A] with EventSourceImplMixin[A] {

	private var listeners: List[WeakReference[Event[A] => Boolean]] = Nil
	private val stoppedAtomic = new AtomicBoolean(false)
	
	def isStopped: Boolean = stoppedAtomic.get

	def observe(thunk: Event[A] => Boolean)(implicit o: Observer): Unit = {
		o add thunk
		o add this
		synchronized {
			listeners ::= new WeakReference(thunk)
		}
	}

	def fire(event: A): Unit = {
		if(isStopped) throw new IllegalStateException("Cannot fire events from a stopped EventSource")
		else send( Fire(event) )	
	}
	
	def stop: Unit = {
		if(!stoppedAtomic.getAndSet(true))
			send( Stop )
	}
	
	private def send(event: Event[A]): Unit = {
		var deadCount = 0
		
		//run the thunks. count the dead or dying ones
		for (tRef <- listeners) tRef.get match {
			case null => deadCount += 1
			case thunk =>
				if (!thunk(event)){
					tRef.clear
					deadCount += 1
				}
		}
		
		//if there are a lot of dead thunks, remove them
		if (deadCount > EventSource.purgeThreshold){
			synchronized {
				listeners = listeners filterNot { _.get == null }
			}
		}
	}
}