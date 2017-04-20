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
    
    val bytes = new ConsumerInterpolatedVoltageProvider(1, 100, 5000, 0, 12).encodePayload(date, value)
    // print(new String(ByteBuffer.wrap(bytes).array()))
    
    val tuple = dataDecoder.apply(bytes)    
    // print(tuple)
    
    assert(date.atOffset(ZoneOffset.MIN).toEpochSecond() == tuple._1)
    assert(date.withNano(0) == LocalDateTime.ofEpochSecond(tuple._1, 0, ZoneOffset.MIN))
    assert(value == tuple._2)
  }

  test("computeNbOfElements(usersPerSec: Double)") {
      for(i <- 1 to 21){
         val nb = math.pow(2, i)
         val (lineNb, transformerNb, usagePointNb) = ProviderUtil.computeNbOfElements(nb)
         assert(lineNb > 0)
         assert(transformerNb > 0)
         assert(usagePointNb > 0)
         
         assert(usagePointNb >= transformerNb)
         assert(transformerNb >= lineNb)
      }    
  }
  
  test ("computeIncr(streamingDuration: Int)") {
      for(i <- 1 to 10){
        val streamingDuration = i * 1000
        assert(60 == ProviderUtil.computeIncr(streamingDuration) * streamingDuration)
      }
  }

}