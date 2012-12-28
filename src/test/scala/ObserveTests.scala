import org.scalatest._
import scala.frp._
import collection.mutable.ListBuffer

class ObserveTests extends FunSuite {

	implicit object observer extends Observer
	
	test("Sink.next") {
		val s = new EventSource[Int]
		val ints = new ListBuffer[Int]
		Sink.next(s){ ints += _ }
		
		s fire 1
		s fire 2
		
		assert(ints.toList == List(1))
	}
	
	test("Sink.end not encountered") {
		val s = new EventSource[Int]
		var gotEnd = false
		Sink.end(s){ gotEnd = true }
		
		s fire 1
		s fire 2
		
		assert(!gotEnd)
	}
	
	test("Sink.end") {
		val s = new EventSource[Int]
		var gotEnd = false
		Sink.end(s){ gotEnd = true }
		
		s fire 1
		s fire 2
		s.stop
		
		assert(gotEnd)
	}
	
	test("Sink.events") {
		val s = new EventSource[Int]
		val ints = new ListBuffer[Int]
		Sink.events(s){ ints += _ }
		
		s fire 1
		s fire 2
		s fire 3
		
		assert(ints.toList == List(1,2,3))
		
	}
}