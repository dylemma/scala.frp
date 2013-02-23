package com.dylemma.frp.test

import org.scalatest._
import com.dylemma.frp._

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
		val x = for {
			i <- s if i % 2 == 0
			j <- t
		} yield i -> j

		val results = accumulateEvents(x)

		s fire 2
		t fire 1 // yield 2 -> 1

		s fire 3
		t fire 2 // yields 2 -> 2

		s fire 4
		t fire 3 // yields 4 -> 3
		t fire 4 // yields 4 -> 4

		assert(results == List(2 -> 1, 2 -> 2, 4 -> 3, 4 -> 4))
	}

	test("EventStream.collect basic functionality") {
		val s = EventSource[Int]
		val x = s collect {
			case i if i % 2 == 0 => i / 2
		}
		val results = accumulateEvents(x)

		s fire 1
		s fire 2 // yield 1
		s fire 3
		s fire 4 // yield 2

		assert(results == List(1, 2))
	}

	test("EventStream.foldLeft basic functionality") {
		val s = EventSource[Int]
		val x = s.foldLeft(0) { _ + _ } //accumulate a sum
		val results = accumulateEvents(x)

		s fire 1 // yield 1
		s fire 2 // yield 3
		s fire 3 // yield 6
		s fire 4 // yield 10

		assert(results == List(1, 3, 6, 10))
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

	test("EventStream.either basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[String]
		val results = accumulateEvents(s either t)

		s fire 1
		t fire "a"
		t fire "b"
		s fire 2

		assert(results.toList == List(Left(1), Right("a"), Right("b"), Left(2)))
	}

	test("EventStream.zipWithIndex basic functionality") {
		val s = EventSource[Char]
		val results = accumulateEvents(s.zipWithIndex)

		s fire 'a'
		s fire 'b'
		s fire 'c'

		assert(results.toList == List('a' -> 0, 'b' -> 1, 'c' -> 2))
	}

	test("EventStream.zipWithStaleness basic functionality") {
		val s = EventSource[Int]
		val x = s.zipWithStaleness

		var a: (Int, () => Boolean) = null
		x onNext { a = _ }
		s fire 1

		assert(!a._2(), "The first event should not be stale yet") // event 'a' isn't stale yet

		var b: (Int, () => Boolean) = null
		x onNext { b = _ }
		s fire 2

		assert(a._2(), "The first event should now be stale")
		assert(!b._2(), "The second event should be fresh")

		var c: (Int, () => Boolean) = null
		x onNext { c = _ }
		s fire 3

		assert(a._2(), "The first event should be stale")
		assert(b._2(), "The second event should be stale")
		assert(!c._2(), "The thrid event should be fresh")
	}

	test("EventStream.zipWithTime basic functionality") {
		val s = EventSource[Int]
		val fakeTime = new FakeTime

		//use fakeTime instead of SystemTime, so we can guarantee the values
		val x = s.zipWithTime(fakeTime)
		val results = accumulateEvents(x)

		fakeTime setTime 123L
		s fire 1

		fakeTime setTime 456L
		s fire 2

		fakeTime setTime 789L
		s fire 3

		assert(results.toList == List(1 -> 123L, 2 -> 456L, 3 -> 789L))

	}

	test("EventStream.zip basic functionality") {
		val s = EventSource[Int]
		val t = EventSource[String]
		val results = accumulateEvents(s zip t)

		s fire 1
		s fire 5
		t fire "A"
		s fire 10
		t fire "B"
		t fire "C"

		assert(results.toList == List(1 -> "A", 5 -> "B", 10 -> "C"))
	}

	test("EventStream.unzip basic functionality") {
		val x = EventSource[(Int, String)]
		val (a, b) = x.unzip

		val aResults = accumulateEvents(a)
		val bResults = accumulateEvents(b)

		x fire 1 -> "a"
		x fire 2 -> "b"

		assert(aResults.toList == List(1, 2) && bResults.toList == List("a", "b"))
	}

	test("EventStream.grouped basic functionality") {
		val x = EventSource[Int]
		val results = accumulateEvents(x grouped 3)

		//group 1: (1,2,3)
		x fire 1
		x fire 2
		x fire 3

		//group 2: (4,5,6)
		x fire 4
		x fire 5
		x fire 6

		//group 3: (7, <end>)
		x fire 7
		x.stop

		assert(results.toList == List(1, 2, 3) :: List(4, 5, 6) :: List(7) :: Nil)
	}
}