package scala.react.impl

import java.util.concurrent.{ThreadFactory, Executors}
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}

private [react] object TimeBasedFutures {

	private object DaemonThreadFactory extends ThreadFactory {
		def newThread(r: Runnable) = {
			var t = new Thread(r)
			t.setDaemon(true)
			t
		}
	}

	lazy val executor = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory)

	class PromiseCompletingRunnable[T](body: => T) extends Runnable {
		val promise = Promise[T]

		override def run() = {
			promise complete {
				try Success(body) catch { case NonFatal(e) => Failure(e) }
			}
		}
	}
	
	def after[T](duration: Duration, body: => T): Future[T] = {
		if (duration.isFinite){
			val runnable = new PromiseCompletingRunnable(body)
			executor.schedule(runnable, duration.length, duration.unit)
			runnable.promise.future
		} else {
			Future.failed(new TimeoutException)
		}
	}
	
	def after[T](deadline: Deadline, body: => T): Future[T] = {
		val waitTime = deadline - Deadline.now
		after(waitTime, body)
	}

}