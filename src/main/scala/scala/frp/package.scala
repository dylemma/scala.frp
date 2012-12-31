package scala

import scala.concurrent._
import scala.util._

package object frp {

	/** An instance of the `Time` typeclass, for `Long` times.
	  * The `currentTime` will always return the result of
	  * `System.currentTimeMillis`.
	  */
	implicit object SystemTime extends Time[Long] {
		def currentTime = System.currentTimeMillis
	}

	implicit class EventStreamFutures[A](stream: EventStream[A]) {

		/** Returns a `Future` that will complete with the value of the next
		  * event fired by this stream. If the stream is stopped, or if it
		  * stops before firing an event, the Future will fail with a
		  * `NoSuchElementException`.
		  *
		  * @return A `Future` containing the next event fired by this stream.
		  */
		def next(implicit obs: Observer): Future[A] = {
			if (stream.stopped) Future.failed(new NoSuchElementException("A stopped EventStream has no next event"))
			else {
				val p = Promise[A]
				stream sink {
					case Stop =>
						p.failure(new NoSuchElementException("Stream stopped before firing any event"))
						false
					case Fire(e) =>
						p.success(e)
						false
				}
				p.future
			}
		}

		/** Returns a `Future` that will complete with the value of the last
		  * event fired by this stream. If the stream is stopped or becomes stopped
		  * before firing an event, the Future will fail with a `NoSuchElementException`.
		  *
		  * @return A `Future` containing the last event fired by this stream.
		  */
		def last(implicit obs: Observer): Future[A] = {
			if (stream.stopped) Future.failed(new NoSuchElementException("Stream is already stopped"))
			else {
				val p = Promise[A]
				var latest: Try[A] = Failure(new NoSuchElementException("No event fired"))
				stream sink {
					case Stop =>
						p complete latest
						false
					case Fire(e) =>
						latest = Success(e)
						true
				}
				p.future
			}
		}

		/** Returns a `Future` that will complete when this stream `stop`s. The
		  * resulting value will be a rough estimate (`System.currentTimeMillis`)
		  * of when the stream ended. If the stream never ends, the resulting
		  * Future will never complete.
		  *
		  * @return A `Future` containing a time stamp describing when this stream stopped.
		  */
		def end(implicit obs: Observer): Future[Long] = {
			if (stream.stopped) Future successful System.currentTimeMillis
			else {
				val p = Promise[Long]
				stream onEnd { p success System.currentTimeMillis }
				p.future
			}
		}
	}

}