package scalax.frp.test

import org.scalatest._
import org.scalatest.concurrent.AsyncAssertions
import scala.frp._
import scala.concurrent.duration._

class EventStreamDeadlineTests extends FunSuite with TestHelpers with AsyncAssertions with Observer {

	test("EventStream.before only encounters events before the deadline") {
		val w = new Waiter
		val s = EventSource[Int]
		w {
			val x = s.before(100 millis fromNow)
			val list = accumulateEvents(x)

			s fire 1
			s fire 2
			Thread.sleep(110)
			s fire 3
			s fire 4
			assert(list.result == List(1, 2))
			w.dismiss
		}
		w.await()
	}

	test("EventStream.before `now` should accumulate no events") {
		val s = EventSource[Int]
		val x = s.before(Deadline.now)
		val list = accumulateEvents(x)

		s fire 1
		s fire 2
		s fire 3

		assert(list.result == Nil)
	}

	test("EventStream.within only encounters events within the timespan") {
		val w = new Waiter
		val s = EventSource[Int]
		w {
			val x = s.within(100.millis)
			val list = accumulateEvents(x)

			s fire 1
			s fire 2
			Thread.sleep(110)
			s fire 3
			s fire 4
			assert(list.result == List(1, 2))
			w.dismiss
		}
		w.await()
	}

	test("EventStream.within will encounter all events given an infinite duration") {
		val w = new Waiter
		val s = EventSource[Int]
		w {
			val x = s.within(Duration.Inf)
			val list = accumulateEvents(x)
			s fire 1
			s fire 2
			Thread.sleep(110)
			s fire 3
			s fire 4
			assert(list.result == List(1, 2, 3, 4))
			w.dismiss
		}
		w.await()
	}
}