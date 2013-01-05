package scalax.frp.test

import org.scalatest._
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.SpanSugar._
import scala.frp._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import org.scalatest.exceptions.TestFailedException

class EventStreamFuturesTest extends FunSuite with TestHelpers with AsyncAssertions with Observer {

	test("EventStream.next completes successfully when the stream fires an event") {
		val w = new Waiter
		val s = EventSource[Int]

		s.next onSuccess { case 5 => w.dismiss }
		s fire 5
		w.await(dismissals(1))
	}

	test("EventStream.next completes with a failure when called on a stopped stream") {
		val w = new Waiter
		val s = EventSource[Int]

		s.stop // stream stopped before `next` is called
		s.next onFailure { case _ => w.dismiss }
		w.await(dismissals(1))
	}

	test("EventStream.next completes with a failure if the stream stops before firing an event") {
		val w = new Waiter
		val s = EventSource[Int]

		s.next onFailure { case _ => w.dismiss }
		s.stop // stream stopped after `next` is called
		w.await(dismissals(1))
	}

	test("EventStream.next never completes if the stream does nothing") {
		val w = new Waiter
		val s = EventSource[Int]

		//this block should never actually run
		s.next onComplete {
			case _ =>
				fail("Future was expected to never complete")
				w.dismiss
		}

		//await should time out, causing a failure. expect and intercept that failure
		intercept[TestFailedException] {
			w.await(dismissals(1))
		}
	}

	test("EventStream.last completes successfully with the last event fired by the stream") {
		val w = new Waiter
		val s = EventSource[Int]

		s.last onSuccess { case 3 => w.dismiss }
		s fire 1
		s fire 2
		s fire 3
		s.stop

		w.await(dismissals(1))
	}

	test("EventStream.last should never complete if the stream never stops") {
		val w = new Waiter
		val s = EventSource[Int]

		s.last onSuccess { case 3 => w.dismiss }
		s fire 1
		s fire 2
		s fire 3
		//not stopping...

		intercept[TestFailedException] {
			w.await(dismissals(1))
		}
	}

	test("EventStream.last should be a failure when called on a stopped stream") {
		val w = new Waiter
		val s = EventSource[Int]
		s.stop

		s.last onFailure { case _ => w.dismiss }
		w.await(dismissals(1))
	}

	test("EventStream.last should be a failure if the stream stops before firing an event") {
		val w = new Waiter
		val s = EventSource[Int]

		s.last onFailure { case _ => w.dismiss }
		s.stop
		w.await(dismissals(1))
	}

	test("EventStream.end completes successfully when called on a stopped stream") {
		val w = new Waiter
		val s = EventSource[Int]
		s.stop

		s.end onSuccess { case _ => w.dismiss }
		w.await(dismissals(1))
	}

	test("EventStream.end completes successfully when the stream stops") {
		val w = new Waiter
		val s = EventSource[Int]

		s.end onSuccess { case _ => w.dismiss }
		s.stop
		w.await(dismissals(1))
	}

	test("EventStream.end never completes if the stream never stops") {
		val w = new Waiter
		val s = EventSource[Int]

		s.end onComplete { _ => w.dismiss }
		//not stopping s...
		intercept[TestFailedException] {
			w.await(dismissals(1))
		}

	}
}