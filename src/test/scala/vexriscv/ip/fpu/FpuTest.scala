package vexriscv.ip.fpu

import java.lang

import org.scalatest.FunSuite
import spinal.core.SpinalEnumElement
import spinal.core.sim._
import spinal.lib.experimental.math.Floating
import spinal.lib.sim._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class FpuTest extends FunSuite{


  test("directed"){
    val portCount = 1
    val p = FpuParameter(
      internalMantissaSize = 23,
      withDouble = false
    )

    SimConfig.withFstWave.compile(new FpuCore(1, p)).doSim(seed = 42){ dut =>
      dut.clockDomain.forkStimulus(10)




      val cpus = for(id <- 0 until portCount) yield new {
        val cmdQueue = mutable.Queue[FpuCmd => Unit]()
        val commitQueue = mutable.Queue[FpuCommit => Unit]()
        val rspQueue = mutable.Queue[FpuRsp => Unit]()

        StreamDriver(dut.io.port(id).cmd ,dut.clockDomain){payload =>
          if(cmdQueue.isEmpty) false else {
            cmdQueue.dequeue().apply(payload)
            true
          }
        }


        StreamMonitor(dut.io.port(id)rsp, dut.clockDomain){payload =>
          rspQueue.dequeue().apply(payload)
        }

        StreamReadyRandomizer(dut.io.port(id).rsp, dut.clockDomain)


        StreamDriver(dut.io.port(id).commit ,dut.clockDomain){payload =>
          if(commitQueue.isEmpty) false else {
            commitQueue.dequeue().apply(payload)
            true
          }
        }


        def loadRaw(rd : Int, value : BigInt): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.LOAD
            cmd.value.randomize()
            cmd.rs1.randomize()
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.value #= value
            cmd.load #= true
          }
        }

        def load(rd : Int, value : Float): Unit ={
          loadRaw(rd, lang.Float.floatToIntBits(value).toLong & 0xFFFFFFFFl)
        }

        def storeRaw(rs : Int)(body : FpuRsp => Unit): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.STORE
            cmd.value.randomize()
            cmd.rs1.randomize()
            cmd.rs2 #= rs
            cmd.rs3.randomize()
            cmd.rd.randomize()
          }

          rspQueue += body
        }

        def storeFloat(rs : Int)(body : Float => Unit): Unit ={
          storeRaw(rs){rsp => body(lang.Float.intBitsToFloat(rsp.value.toLong.toInt))}
        }

        def mul(rd : Int, rs1 : Int, rs2 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.MUL
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def add(rd : Int, rs1 : Int, rs2 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.ADD
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def div(rd : Int, rs1 : Int, rs2 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.DIV
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def sqrt(rd : Int, rs1 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.SQRT
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def fma(rd : Int, rs1 : Int, rs2 : Int, rs3 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.FMA
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3 #= rs3
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }


        def cmp(rs1 : Int, rs2 : Int)(body : FpuRsp => Unit): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.CMP
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd.randomize()
          }
          rspQueue += body
        }

        def f2i(rs1 : Int)(body : FpuRsp => Unit): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.F2I
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd.randomize()
          }
          rspQueue += body
        }

        def i2f(rd : Int, value : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.I2F
            cmd.value #= value
            cmd.rs1.randomize()
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def fmv_x_w(rs1 : Int)(body : FpuRsp => Unit): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.FMV_X_W
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd.randomize()
          }
          rspQueue += body
        }

        def fmv_w_x(rd : Int, value : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.FMV_W_X
            cmd.value #= value.toLong & 0xFFFFFFFFl
            cmd.rs1.randomize()
            cmd.rs2.randomize()
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }

        def min(rd : Int, rs1 : Int, rs2 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.MIN_MAX
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }


        def sgnj(rd : Int, rs1 : Int, rs2 : Int): Unit ={
          cmdQueue += {cmd =>
            cmd.opcode #= cmd.opcode.spinalEnum.SGNJ
            cmd.value.randomize()
            cmd.rs1 #= rs1
            cmd.rs2 #= rs2
            cmd.rs3.randomize()
            cmd.rd #= rd
          }
          commitQueue += {cmd =>
            cmd.write #= true
            cmd.load #= false
          }
        }
      }





      val stim = for(cpu <- cpus) yield fork {
        import cpu._

        class RegAllocator(){
          var value = 0

          def allocate(): Int ={
            while(true){
              val rand = Random.nextInt(32)
              val mask = 1 << rand
              if((value & mask) == 0) {
                value |= mask
                return rand
              }
            }
            0
          }
        }
        def checkFloat(ref : Float, dut : Float): Boolean ={
          ref.abs * 1.0001 > dut.abs && ref.abs * 0.9999 < dut.abs && ref.signum == dut.signum
        }

        def randomFloat(): Float ={
          val exp = Random.nextInt(10)-5
          (Random.nextDouble() * (Math.pow(2.0, exp)) * (if(Random.nextBoolean()) -1.0 else 1.0)).toFloat
        }

        def testAdd(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)

          add(rd,rs1,rs2)
          storeFloat(rd){v =>
            val ref = a+b
            println(f"$a + $b = $v, $ref")
            assert(checkFloat(ref, v))
          }
        }

        def testMul(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)

          mul(rd,rs1,rs2)
          storeFloat(rd){v =>
            val ref = a*b
            println(f"$a * $b = $v, $ref")
            assert(checkFloat(ref, v))
          }
        }


        def testFma(a : Float, b : Float, c : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)
          load(rs3, c)

          fma(rd,rs1,rs2,rs3)
          storeFloat(rd){v =>
            val ref = a.toDouble * b.toDouble + c.toDouble
            println(f"$a%.20f * $b%.20f + $c%.20f = $v%.20f, $ref%.20f")
            val mul = a.toDouble * b.toDouble
            if((mul.abs-c.abs)/mul.abs > 0.1)  assert(checkFloat(ref.toFloat, v))
          }
        }


        def testDiv(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)

          div(rd,rs1,rs2)
          storeFloat(rd){v =>
            val ref = a/b
            val error = Math.abs(ref-v)/ref
            println(f"$a / $b = $v, $ref $error")
            assert(checkFloat(ref, v))
          }
        }

        def testSqrt(a : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)

          sqrt(rd,rs1)
          storeFloat(rd){v =>
            val ref = Math.sqrt(a).toFloat
            val error = Math.abs(ref-v)/ref
            println(f"sqrt($a) = $v, $ref $error")
            assert(checkFloat(ref, v))
          }
        }

        def testF2i(a : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          f2i(rs1){rsp =>
            val ref = a.toInt
            val v = rsp.value.toBigInt
            println(f"f2i($a) = $v, $ref")
            assert(v === ref)
          }
        }

        def testI2f(a : Int): Unit ={
          val rs = new RegAllocator()
          val rd = Random.nextInt(32)
          i2f(rd, a)
          storeFloat(rd){v =>
            val ref = a.toInt
            println(f"i2f($a) = $v, $ref")
            assert(v === ref)
          }
        }

        def testCmp(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)
          cmp(rs1, rs2){rsp =>
            val ref = if(a < b) 1 else 0
            val v = rsp.value.toBigInt
            println(f"$a < $b = $v, $ref")
            assert(v === ref)
          }
        }

        def testFmv_x_w(a : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          fmv_x_w(rs1){rsp =>
            val ref = lang.Float.floatToIntBits(a).toLong & 0xFFFFFFFFl
            val v = rsp.value.toBigInt
            println(f"fmv_x_w $a = $v, $ref")
            assert(v === ref)
          }
        }

        def testFmv_w_x(a : Int): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          fmv_w_x(rd, a)
          storeFloat(rd){v =>
            val ref = lang.Float.intBitsToFloat(a)
            println(f"fmv_w_x $a = $v, $ref")
            assert(v === ref)
          }
        }



        def testMin(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)

          min(rd,rs1,rs2)
          storeFloat(rd){v =>
            val ref = a min b
            println(f"min $a $b = $v, $ref")
            assert(ref ==  v)
          }
        }

        def testSgnj(a : Float, b : Float): Unit ={
          val rs = new RegAllocator()
          val rs1, rs2, rs3 = rs.allocate()
          val rd = Random.nextInt(32)
          load(rs1, a)
          load(rs2, b)

          sgnj(rd,rs1,rs2)
          storeFloat(rd){v =>
            val ref = a * a.signum * b.signum
            println(f"sgnf $a $b = $v, $ref")
            assert(ref ==  v)
          }
        }


        val b2f = lang.Float.intBitsToFloat(_)


        testFmv_x_w(1.246f)
        testFmv_w_x(lang.Float.floatToIntBits(7.234f))

        testMin(1.0f, 2.0f)
        testMin(1.5f, 2.0f)
        testMin(1.5f, 3.5f)
        testMin(1.5f, 1.5f)
        testMin(1.5f, -1.5f)
        testMin(-1.5f, 1.5f)
        testMin(-1.5f, -1.5f)
        testMin(1.5f, -3.5f)

        testSgnj(1.0f, 2.0f)
        testSgnj(1.5f, 2.0f)
        testSgnj(1.5f, 3.5f)
        testSgnj(1.5f, 1.5f)
        testSgnj(1.5f, -1.5f)
        testSgnj(-1.5f, 1.5f)
        testSgnj(-1.5f, -1.5f)
        testSgnj(1.5f, -3.5f)



        //TODO Test corner cases
        testI2f(17)
        testI2f(12)
        testI2f(512)
        testI2f(1)
//        dut.clockDomain.waitSampling(1000)
//        simFailure()

        //TODO Test corner cases
        testCmp(1.0f, 2.0f)
        testCmp(1.5f, 2.0f)
        testCmp(1.5f, 3.5f)
        testCmp(1.5f, 1.5f)
        testCmp(1.5f, -1.5f)
        testCmp(-1.5f, 1.5f)
        testCmp(-1.5f, -1.5f)
        testCmp(1.5f, -3.5f)

        //TODO Test corner cases
        testF2i(16.0f)
        testF2i(18.0f)
        testF2i(1200.0f)
        testF2i(1.0f)



        testAdd(0.1f, 1.6f)

        testSqrt(1.5625f)
        testSqrt(1.5625f*2)
        testSqrt(1.8f)
        testSqrt(4.4f)
        testSqrt(0.3f)
        testSqrt(1.5625f*2)
        testSqrt(b2f(0x3f7ffffe))
        testSqrt(b2f(0x3f7fffff))
        testSqrt(b2f(0x3f800000))
        testSqrt(b2f(0x3f800001))
        testSqrt(b2f(0x3f800002))
        testSqrt(b2f(0x3f800003))



        testMul(0.1f, 1.6f)
        testFma(1.1f, 2.2f, 3.0f)
        testDiv(1.0f, 1.1f)
        testDiv(1.0f, 1.5f)
        testDiv(1.0f, 1.9f)
        testDiv(1.1f, 1.9f)
        testDiv(1.0f, b2f(0x3f7ffffe))
        testDiv(1.0f, b2f(0x3f7fffff))
        testDiv(1.0f, b2f(0x3f800000))
        testDiv(1.0f, b2f(0x3f800001))
        testDiv(1.0f, b2f(0x3f800002))

        for(i <- 0 until 1000){
          testAdd(randomFloat(), randomFloat())
        }
        for(i <- 0 until 1000){
          testMul(randomFloat(), randomFloat())
        }
        for(i <- 0 until 1000){
          testFma(randomFloat(), randomFloat(), randomFloat())
        }
        for(i <- 0 until 1000){
          testDiv(randomFloat(), randomFloat())
        }
        for(i <- 0 until 1000){
          testSqrt(Math.abs(randomFloat()))
        }
        for(i <- 0 until 1000){
          testCmp(randomFloat(), randomFloat())
        }
        for(i <- 0 until 1000){
          val tests = ArrayBuffer[() => Unit]()
          tests += (() =>{testAdd(randomFloat(), randomFloat())})
          tests += (() =>{testMul(randomFloat(), randomFloat())})
          tests += (() =>{testFma(randomFloat(), randomFloat(), randomFloat())})
          tests += (() =>{testDiv(randomFloat(), randomFloat())})
          tests += (() =>{testSqrt(randomFloat().abs)})
          tests += (() =>{testCmp(randomFloat(), randomFloat())})
          tests += (() =>{testFmv_x_w(randomFloat())})
          tests += (() =>{testFmv_w_x(lang.Float.floatToIntBits(randomFloat()))})
          tests += (() =>{testMin(randomFloat(), randomFloat())})
          tests += (() =>{testSgnj(randomFloat(), randomFloat())})


          tests.randomPick().apply()
        }
        waitUntil(cpu.rspQueue.isEmpty)
      }


      stim.foreach(_.join())
      dut.clockDomain.waitSampling(100)
    }
  }
}
