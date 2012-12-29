import org.scalatest._
import scala.frp._

class EventStreamTests extends FunSuite with TestHelpers {

	implicit object observer extends Observer

	test("EventStream.map basic functionality") {
		val s = EventSource[Int]

		val results = accumulateEvents(s.map { _ * 2 })

		s fire 1
		s fire 2
		s fire 3

		assert(results.toList == List(2, 4, 6))
	}

	test("EventStream.withFilter basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		//val results = new ListBuffer[(Int, Int)]
		val x = for {
			i <- s if i % 2 == 0
			j <- t
		} yield i -> j

		val results = accumulateEvents(x)

		s fire 2
		t fire 1 // yield 2 -> 1

		s fire 3
		t fire 2 // yields 2 -> 2 ?

		s fire 4
		t fire 3 // yields 4 -> 3
		t fire 4 // yields 4 -> 4

		assert(results == List(2 -> 1, 2 -> 2, 4 -> 3, 4 -> 4))
	}

	test("EventStream.take basic functionality") {
		val s = EventSource[Int]
		val x = s.take(3)

		val results = accumulateEvents(x)
		val gotEnd = awaitStop(x)

		s fire 1
		s fire 2
		s fire 3

		assert(results.toList == List(1, 2, 3))
		assert(gotEnd())

		s fire 4

		assert(results.size == 3)
	}

	test("EventStream.takeWhile basic functionality") {
		val s = EventSource[Int]
		val x = s takeWhile { _ < 4 }
		val results = accumulateEvents(x)
		val gotEnd = awaitStop(x)

		for (i <- 1 to 5) s fire i

		assert(results.toList == List(1, 2, 3))
		assert(gotEnd())
	}

	test("EventStream.dropWhile basic functionality") {
		val s = EventSource[Int]
		val x = s dropWhile { _ < 3 }
		val results = accumulateEvents(x)

		s fire 1
		s fire 2
		s fire 3
		s fire 2
		s fire 1

		assert(results.toList == List(3, 2, 1))
	}

	test("EventStream.drop basic functionality") {
		val s = EventSource[Int]
		val x = s drop 3
		val results = accumulateEvents(x)

		for (i <- 1 to 5) s fire i

		assert(results.toList == List(4, 5))
	}

	test("EventStream.++ basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		val results = accumulateEvents(s ++ t)

		s fire 1
		t fire 2 //ignored because s isn't done
		s fire 3
		s.stop
		t fire 4
		t fire 5

		assert(results.toList == List(1, 3, 4, 5))
	}

	test("EventStream.++ encounters end when both parents end") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		var gotEnd = false

		s ++ t onEnd { gotEnd = true }

		s fire 1
		s.stop
		t fire 2
		t.stop

		assert(gotEnd)
	}

	test("EventStream.until basic functionality") {
		val s = EventSource[Int]
		val end = EventSource[String]
		val results = accumulateEvents(s until end)

		s fire 1
		s fire 2
		s fire 3
		end fire "done"
		s fire 4
		s fire 5

		assert(results.toList == List(1, 2, 3))
	}

	test("EventStream.|| basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[Int]
		val results = accumulateEvents(s || t)

		s fire 1
		t fire 2
		s fire 3
		t fire 4

		assert(results.toList == List(1, 2, 3, 4))
	}

}