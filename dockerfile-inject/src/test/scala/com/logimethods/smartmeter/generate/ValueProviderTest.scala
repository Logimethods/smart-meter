package com.logimethods.smartmeter.generate

import com.logimethods.smartmeter.generate._
import org.scalatest._
import java.time._

class ValueProviderTest extends FunSuite {
  import java.nio.ByteBuffer

  val dataDecoder = new java.util.function.Function[Array[Byte],Tuple2[Long,Float]] {
    override def apply(bytes: Array[Byte]):Tuple2[Long,Float] = {
      val buffer = ByteBuffer.wrap(bytes);
      val epoch = buffer.getLong()
      val voltage = buffer.getFloat()
      (epoch, voltage)  
    }
  }

  test("encodePayload(date, value)") {
    val date = LocalDateTime.now()
    val value = 12345.6789f
    
    val bytes = new ConsumerInterpolatedVoltageProvider().encodePayload(date, value)
    // print(new String(ByteBuffer.wrap(bytes).array()))
    
    val tuple = dataDecoder.apply(bytes)    
    // print(tuple)
    
    assert(date.atZone(ZoneId.systemDefault()).toEpochSecond() == tuple._1)
    assert(value == tuple._2)
  }
}