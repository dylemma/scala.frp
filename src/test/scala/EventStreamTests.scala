import org.scalatest._
import scala.react._
import collection.mutable.ListBuffer

class EventStreamTests extends FunSuite {

	implicit object observer extends Observer
	
	test("EventStream.flatMap") {
	
		val s = new EventSource[Int]
		val t = new EventSource[Int]
		val results = new ListBuffer[(Int, Int)]
		
		val x = for {
			i <- s
			j <- t
		} yield i->j 
		
		for(e <- x) results += e
		
		s fire 1
		
		s fire 2
		t fire 1 //yield 2->1
		t fire 2 //yield 2->2
		
		s fire 3
		t fire 4 //yield 3->4
		
		assert(results.toList == List(2->1, 2->2, 3->4))
	}
	
	test("EventStream.map") {
		val s = new EventSource[Int]
		val results = new ListBuffer[Int]
		
		for {
			i <- s.map(_ * 2)
		} results += i
		
		s fire 1
		s fire 2
		s fire 3
		
		assert( results.toList == List(2,4,6) )
	}
	
	test("EventStream.withFilter") {
		val s = new EventSource[Int]
		val t = new EventSource[Int]
		val results = new ListBuffer[(Int, Int)]
		val x = for {
			i <- s if i % 2 == 0
			j <- t
		} yield i->j
		x foreach { results += _ }
		
		s fire 2
		t fire 1 // yield 2 -> 1
		
		s fire 3
		t fire 2 // yields 2 -> 2 ?
		
		s fire 4
		t fire 3 // yields 4 -> 3
		t fire 4 // yields 4 -> 4
		
		assert( results == List(2->1, 2->2, 4->3, 4->4) )
	}

	test("EventStream.take") {
		val s = new EventSource[Int]
		val results = new ListBuffer[Int]
		var gotEnd = false
		val x = s.take(3)
		
		for (e <- x) results += e
		Observe.end(x){ gotEnd = true }
		
		s fire 1
		s fire 2
		s fire 3

		assert(results.toList == List(1,2,3))
		assert(gotEnd)
		
		s fire 4
		
		assert(results.size == 3)
	}
	
	test("EventStream.takeWhile") {
		val s = new EventSource[Int]
		val results = new ListBuffer[Int]
		var gotEnd = false
		val x = s takeWhile {_ < 4}
		
		for(e <- x) results += e
		Observe.end(x){ gotEnd = true }
		
		s fire 1
		s fire 2
		s fire 3
		s fire 4
		s fire 5
		
		assert( results.toList == List(1,2,3) )
		assert(gotEnd)
	}
	
	test("EventStream.dropWhile") {
		val s = new EventSource[Int]
		val results = new ListBuffer[Int]
		val x = s dropWhile {_ < 3}
		
		for(e <- x) results += e
		
		s fire 1
		s fire 2
		s fire 3
		s fire 2
		s fire 1
		
		assert( results.toList == List(3,2,1) )
	}
	
	test("EventStream.drop") {
		val s = new EventSource[Int]
		val results = new ListBuffer[Int]
		val x = s drop 3
		
		for(e <- x) results += e
		
		s fire 1
		s fire 2
		s fire 3
		s fire 4
		s fire 5
		
		assert( results.toList == List(4,5) )	
	}
	
	test("EventStream.++") {
		val s = new EventSource[Int]
		val t = new EventSource[Int]
		val results = new ListBuffer[Int]
		
		for(e <- s ++ t) results += e
		
		s fire 1
		t fire 2 //ignored because s isn't done
		s fire 3
		s.stop
		t fire 4
		t fire 5
		
		assert( results.toList == List(1,3,4,5) )
	}
	
	test("EventStream.++.end") {
		val s = new EventSource[Int]
		val t = new EventSource[Int]
		var gotEnd = false
		
		Observe.end(s++t){ gotEnd = true }
		
		s fire 1
		s.stop
		t fire 2
		t.stop
		
		assert( gotEnd )
	}
	
	test("EventStream.until") {
		val s = new EventSource[Int]
		val end = new EventSource[String]
		val results = new ListBuffer[Int]
		
		for(e <- s until end) results += e
		
		s fire 1
		s fire 2
		s fire 3
		end fire "done"
		s fire 4
		s fire 5
		
		assert( results.toList == List(1,2,3) )
	}
	
	test("EventStream.||") {
		val s = new EventSource[Int]
		val t = new EventSource[Int]
		val results = new ListBuffer[Int]
		
		for(e <- s || t) results += e
		
		s fire 1
		t fire 2
		s fire 3
		t fire 4
		
		assert( results.toList == List(1,2,3,4) )
	}
	
	//todo: test `EventStream.within` and `EventStream.before`
}