import org.scalatest._
import scala.react._
import collection.mutable.ListBuffer

class ObserveTests extends FunSuite {

	implicit object observer extends Observer
	
	test("Observe.next") {
		val s = new EventSource[Int]
		val ints = new ListBuffer[Int]
		Observe.next(s){ ints += _ }
		
		s fire 1
		s fire 2
		
		assert(ints.toList == List(1))
	}
	
	test("Observe.end not encountered") {
		val s = new EventSource[Int]
		var gotEnd = false
		Observe.end(s){ gotEnd = true }
		
		s fire 1
		s fire 2
		
		assert(!gotEnd)
	}
	
	test("Observe.end") {
		val s = new EventSource[Int]
		var gotEnd = false
		Observe.end(s){ gotEnd = true }
		
		s fire 1
		s fire 2
		s.stop
		
		assert(gotEnd)
	}
	
	test("Observe.events") {
		val s = new EventSource[Int]
		val ints = new ListBuffer[Int]
		Observe.events(s){ ints += _ }
		
		s fire 1
		s fire 2
		s fire 3
		
		assert(ints.toList == List(1,2,3))
		
	}
}